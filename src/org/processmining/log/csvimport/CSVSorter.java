package org.processmining.log.csvimport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.processmining.log.csvimport.CSVConversion.ProgressListener;
import org.processmining.log.csvimport.exception.CSVSortException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.fasterxml.sort.DataReader;
import com.fasterxml.sort.DataReaderFactory;
import com.fasterxml.sort.DataWriter;
import com.fasterxml.sort.DataWriterFactory;
import com.fasterxml.sort.IteratingSorter;
import com.fasterxml.sort.SortConfig;
import com.fasterxml.sort.SortingState.Phase;
import com.fasterxml.sort.TempFileProvider;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.parallel.PLZFOutputStream;


/**
 * @author F. Mannhardt
 *
 */
final class CSVSorter {

	private static final class UncompressedOpenCSVReader extends DataReader<String[]> {

		private final CSVReader reader;
		private final int numColumns;

		private UncompressedOpenCSVReader(InputStream inputStream, CSVImportConfig importConfig, int numColumns)
				throws UnsupportedEncodingException {
			this.numColumns = numColumns;
			this.reader = CSVUtils.createCSVReader(inputStream, importConfig);
		}

		public void close() throws IOException {
			reader.close();
		}

		public int estimateSizeInBytes(String[] val) {
			return estimateSize(val);
		}

		public String[] readNext() throws IOException {
			String[] val = reader.readNext();
			if (val != null && val.length != numColumns) {
				throw new IOException("Inconsistent number of fields in a row of the CSV file. Should be " + numColumns
						+ " according to the header, but read a line with " + val.length + " files! Invalid line: "
						+ Arrays.toString(val));
			}
			return val;
		}
	}

	private static final class CompressedOpenCSVDataWriterFactory extends DataWriterFactory<String[]> {
		private final CSVImportConfig importConfig;

		private CompressedOpenCSVDataWriterFactory(CSVImportConfig importConfig) {
			this.importConfig = importConfig;
		}

		public DataWriter<String[]> constructWriter(OutputStream os) throws IOException {
			final CSVWriter writer = CSVUtils.createCSVWriter(new PLZFOutputStream(os), importConfig);
			// Write Header
			return new DataWriter<String[]>() {

				public void close() throws IOException {
					writer.close();
				}

				public void writeEntry(String[] val) throws IOException {
					writer.writeNext(val, false);
				}
			};
		}
	}

	private static final class CompressedOpenCSVDataReaderFactory extends DataReaderFactory<String[]> {
		private final CSVImportConfig importConfig;

		private CompressedOpenCSVDataReaderFactory(CSVImportConfig importConfig) {
			this.importConfig = importConfig;
		}

		public DataReader<String[]> constructReader(InputStream is) throws IOException {
			final CSVReader reader = CSVUtils.createCSVReader(new LZFInputStream(is), importConfig);
			return new DataReader<String[]>() {

				public void close() throws IOException {
					reader.close();
				}

				public int estimateSizeInBytes(String[] item) {
					return estimateSize(item);
				}

				public String[] readNext() throws IOException {
					return reader.readNext();
				}
			};
		}
	}

	private CSVSorter() {
	}

	public static File sortCSV(final ProgressListener progress, final CSVFile csv, final CSVImportConfig importConfig, final Comparator<String[]> rowComparator,
			final int maxMemory, final int numColumns) throws CSVSortException {
		
		// Create Sorter
		final CompressedOpenCSVDataReaderFactory dataReaderFactory = new CompressedOpenCSVDataReaderFactory(importConfig);
		final CompressedOpenCSVDataWriterFactory dataWriterFactory = new CompressedOpenCSVDataWriterFactory(importConfig);
		final IteratingSorter<String[]> sorter = new IteratingSorter<>(new SortConfig().withMaxMemoryUsage(
				maxMemory * 1024l * 1024l).withTempFileProvider(new TempFileProvider() {

			public File provide() throws IOException {
				return Files.createTempFile(csv.getFilename()+"-merge-sort", ".lzf").toFile();
			}
		}), dataReaderFactory, dataWriterFactory, rowComparator);
		
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<File> future = executorService.submit(new Callable<File>() {

			public File call() throws Exception {
				
				// Read uncompressed CSV
				InputStream inputStream = skipFirstLine(CSVUtils.getCSVInputStream(csv));
				DataReader<String[]> inputDataReader = new UncompressedOpenCSVReader(inputStream, importConfig, numColumns);
				try {
					Iterator<String[]> result = sorter.sort(inputDataReader);
					
					// Write sorted result to compressed file
					if (result != null) {			
						File sortedCsvFile = Files.createTempFile(csv.getFilename()+"-sorted", ".lzf").toFile();
						DataWriter<String[]> dataWriter = dataWriterFactory
								.constructWriter(new FileOutputStream(sortedCsvFile));
						try {
							while (result.hasNext()) {
								dataWriter.writeEntry(result.next());
							}
						} finally {
							dataWriter.close();
						}
						return sortedCsvFile;
					}
					
				} finally {
					sorter.close();
				}
				
				throw new CSVSortException("Could not sort file.");				
			}
		});

		try {
			executorService.shutdown();
			int sortRound = -1;
			int preSortFiles = -1;
			while (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
				if (progress.getProgress().isCancelled()) {
					progress.log("Cancelling sorting, this might take a while ...");
					sorter.cancel();
					throw new CSVSortException("User cancelled sorting");
				}
				if (sorter.getPhase() == Phase.PRE_SORTING) {			
					if (sorter.getSortRound() != sortRound) {
						sortRound = sorter.getSortRound();
						progress.log(MessageFormat.format("Pre-sorting finished segment {0} in memory ...", sortRound+1));
					}
					if (sorter.getNumberOfPreSortFiles() != preSortFiles) {
						preSortFiles = sorter.getNumberOfPreSortFiles();
						progress.log(MessageFormat.format("Pre-sorting finished segment {0} ...", preSortFiles+1));	
					}					
				} else if (sorter.getPhase() == Phase.SORTING) {
					if (sorter.getSortRound() != sortRound) {
						sortRound = sorter.getSortRound();
						progress.log(MessageFormat.format("Sorting finished round {0}/{1} ...", sortRound+1, sorter.getNumberOfSortRounds()+1));
					}					
				}
			}			
			return future.get();
		} catch (InterruptedException e) {
			progress.log("Cancelling sorting, this might take a while ...");
			sorter.cancel();
			throw new CSVSortException("Cancelled sorting", e);
		} catch (ExecutionException e) {
			throw new CSVSortException("Could not sort file.", e);
		}
	}

	private static InputStream skipFirstLine(InputStream is) throws IOException {
		int val = -1;
		do {
			val = (byte) is.read();
		} while (val != -1 && (val != '\n'));
		return is;
	}

	private static int estimateSize(String[] item) {
		int size = 8 * ((item.length * 4 + 12) / 8);
		for (String s : item) {
			size += 8 * ((((s.length()) * 2) + 45) / 8);
		}
		return size;
	}

}
