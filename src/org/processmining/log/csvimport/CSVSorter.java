package org.processmining.log.csvimport;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.processmining.log.csv.CSVFile;
import org.processmining.log.csvimport.CSVConversion.ProgressListener;
import org.processmining.log.csvimport.config.CSVImportConfig;
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
 * Sorts an {@link CSVFile}
 * 
 * @author F. Mannhardt
 *
 */
final class CSVSorter {

	private static final class UncompressedOpenCSVReader extends DataReader<String[]> {

		private static final int MAX_COLUMNS_FOR_ERROR_REPORTING = 32;
		private static final int MAX_FIELD_LENGTH_FOR_ERROR_REPORTING = 64;
		
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
				String offendingLine = safeToString(val);
				throw new IOException("Inconsistent number of fields in a row of the CSV file. Should be " + numColumns
						+ " according to the header, but read a line with " + val.length + " files! Invalid line: "
						+ offendingLine);
			}
			return val;
		}

		private String safeToString(String[] valueArray) {
			if (valueArray == null) {
				return "NULL";
			} else if (valueArray.length == 0) {
				return "[]";
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append('[');
				for (int i = 0;; i++) {
					String value = valueArray[i];
					if (value.length() < MAX_FIELD_LENGTH_FOR_ERROR_REPORTING) {
						sb.append(value);
					} else {
						sb.append(value.substring(0, MAX_FIELD_LENGTH_FOR_ERROR_REPORTING-1));
					}
					if (i > MAX_COLUMNS_FOR_ERROR_REPORTING) {
						return sb.append(String.format("[... omitted %s further columns]", valueArray.length - i)).toString();
					}
					if (i == valueArray.length-1)
						return sb.append(']').toString();
					sb.append(", ");
				}
			}
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

	/**
	 * Sorts an {@link CSVFile} using only a configurable, limited amount of
	 * memory.
	 * 
	 * @param csvFile
	 * @param rowComparator
	 * @param importConfig
	 * @param maxMemory
	 * @param numOfColumnsInCSV
	 * @param progress
	 * @return a {@link File} containing the sorted CSV
	 * @throws CSVSortException
	 */
	public static File sortCSV(final CSVFile csvFile, final Comparator<String[]> rowComparator,
			final CSVImportConfig importConfig, final int maxMemory, final int numOfColumnsInCSV,
			final ProgressListener progress) throws CSVSortException {

		// Create Sorter
		final CompressedOpenCSVDataReaderFactory dataReaderFactory = new CompressedOpenCSVDataReaderFactory(
				importConfig);
		final CompressedOpenCSVDataWriterFactory dataWriterFactory = new CompressedOpenCSVDataWriterFactory(
				importConfig);
		final IteratingSorter<String[]> sorter = new IteratingSorter<>(new SortConfig().withMaxMemoryUsage(
				maxMemory * 1024l * 1024l).withTempFileProvider(new TempFileProvider() {

			public File provide() throws IOException {
				return Files.createTempFile(csvFile.getFilename() + "-merge-sort", ".lzf").toFile();
			}
		}), dataReaderFactory, dataWriterFactory, rowComparator);

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<File> future = executorService.submit(new Callable<File>() {

			public File call() throws Exception {

				// Read uncompressed CSV
				InputStream inputStream = skipFirstLine(CSVUtils.getCSVInputStream(csvFile));
				DataReader<String[]> inputDataReader = new UncompressedOpenCSVReader(inputStream, importConfig,
						numOfColumnsInCSV);
				try {
					Iterator<String[]> result = sorter.sort(inputDataReader);

					// Write sorted result to compressed file
					if (result != null && result.hasNext()) {
						File sortedCsvFile = Files.createTempFile(csvFile.getFilename() + "-sorted", ".lzf").toFile();
						DataWriter<String[]> dataWriter = dataWriterFactory.constructWriter(new FileOutputStream(
								sortedCsvFile));
						try {
							while (result.hasNext()) {
								dataWriter.writeEntry(result.next());
							}
						} finally {
							dataWriter.close();
						}
						return sortedCsvFile;
					} else {
						if (!result.hasNext()) {
							throw new CSVSortException("Could not sort file! Input parser returned empty file.");
						} else {
							throw new CSVSortException("Could not sort file! Unkown error while sorting.");
						}
					}

				} finally {
					sorter.close();
				}

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
						progress.log(MessageFormat.format("Pre-sorting finished segment {0} in memory ...",
								sortRound + 1));
					}
					if (sorter.getNumberOfPreSortFiles() != preSortFiles) {
						preSortFiles = sorter.getNumberOfPreSortFiles();
						progress.log(MessageFormat.format("Pre-sorting finished segment {0} ...", preSortFiles + 1));
					}
				} else if (sorter.getPhase() == Phase.SORTING) {
					if (sorter.getSortRound() != sortRound) {
						sortRound = sorter.getSortRound();
						progress.log(MessageFormat.format("Sorting finished round {0}/{1} ...", sortRound + 1,
								sorter.getNumberOfSortRounds() + 1));
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

	private static InputStream skipFirstLine(InputStream inputStream) throws IOException {
		InputStream is = new BufferedInputStream(inputStream);
		int val = -1;
		do {
			val = (byte) is.read();
		} while (val != -1 && ((val != '\n') && val != '\r'));
		is.mark(1);
		if (is.read() == '\n') {
			return is;
		} else {
			is.reset();
			return is;
		}
	}

	private static int estimateSize(String[] item) {
		int size = 8 * ((item.length * 4 + 12) / 8);
		for (String s : item) {
			size += 8 * ((((s.length()) * 2) + 45) / 8);
		}
		return size;
	}

}
