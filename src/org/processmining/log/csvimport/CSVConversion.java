package org.processmining.log.csvimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.Progress;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.CSVFileReferenceOpenCSVImpl;
import org.processmining.log.csv.ICSVReader;
import org.processmining.log.csv.config.CSVConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVEmptyCellHandlingMode;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVErrorHandlingMode;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVMapping;
import org.processmining.log.csvimport.exception.CSVConversionConfigException;
import org.processmining.log.csvimport.exception.CSVConversionException;
import org.processmining.log.csvimport.exception.CSVSortException;
import org.processmining.log.csvimport.handler.CSVConversionHandler;
import org.processmining.log.csvimport.handler.XESConversionHandlerImpl;

import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.ning.compress.lzf.LZFInputStream;

/**
 * Conversion from CSV to a structure like XES. Use
 * {@link #doConvertCSVToXES(ProgressListener, CSVFile, CSVConfig, CSVConversionConfig)}
 * to convert to XES, use
 * {@link #convertCSV(ProgressListener, CSVConfig, CSVConversionConfig, CSVFile, CSVConversionHandler)}
 * to use your own {@link CSVConversionHandler} for some other format.
 * <p>
 * Example usage:
 * <p>
 * 
 * <pre>
 * CSVFileReferenceUnivocityImpl csvFile = new CSVFileReferenceUnivocityImpl(getFile().toPath());
 * CSVConfig config = new CSVConfig(csvFile);
 * try (ICSVReader reader = csvFile.createReader(config)) {
 * 	CSVConversion conversion = new CSVConversion();
 * 	CSVConversionConfig conversionConfig = new CSVConversionConfig(csvFile, config);
 * 	conversionConfig.autoDetect();
 * 
 * 	conversionConfig.setCaseColumns(ImmutableList.of(&quot;case&quot;));
 * 	conversionConfig.setEventNameColumns(ImmutableList.of(&quot;event&quot;));
 * 	conversionConfig.setCompletionTimeColumn(&quot;time&quot;);
 * 	conversionConfig.setEmptyCellHandlingMode(CSVEmptyCellHandlingMode.SPARSE);
 * 	conversionConfig.setErrorHandlingMode(CSVErrorHandlingMode.ABORT_ON_ERROR);
 * 	Map&lt;String, CSVMapping&gt; conversionMap = conversionConfig.getConversionMap();
 * 	CSVMapping mapping = conversionMap.get(&quot;time&quot;);
 * 	mapping.setDataType(Datatype.TIME);
 * 	mapping.setPattern(&quot;yyyy/MM/dd&quot;);
 * 
 * 	ConversionResult&lt;XLog&gt; result = conversion.doConvertCSVToXES(new NoOpProgressListenerImpl(), csvFile, config,
 * 			conversionConfig);
 * 
 * 	XLog log = result.getResult();
 * }
 * </pre>
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVConversion {

	public interface ConversionResult<R> {
		R getResult();

		boolean hasConversionErrors();

		String getConversionErrors();
	}

	public interface ProgressListener {
		Progress getProgress();

		void log(String message);
	}

	public final static class NoOpProgressListenerImpl implements ProgressListener {
		public void log(String message) {
		}

		public Progress getProgress() {
			return new NoOpProgressImpl();
		}
	}

	private final static class NoOpProgressImpl implements Progress {
		public void setValue(int value) {
		}

		public void setMinimum(int value) {
		}

		public void setMaximum(int value) {
		}

		public void setIndeterminate(boolean makeIndeterminate) {
		}

		public void setCaption(String message) {
		}

		public boolean isIndeterminate() {
			return false;
		}

		public boolean isCancelled() {
			return false;
		}

		public void inc() {
		}

		public int getValue() {
			return 0;
		}

		public int getMinimum() {
			return 0;
		}

		public int getMaximum() {
			return 0;
		}

		public String getCaption() {
			return null;
		}

		public void cancel() {

		}
	}

	private static final class ImportOrdering extends Ordering<String[]> {

		private final int[] caseIndices;

		private final int completionTimeIndex;
		private final CSVMapping completionTimeMapping;

		private final CSVErrorHandlingMode errorHandlingMode;

		public ImportOrdering(int[] indices, Map<Integer, CSVMapping> mappingMap, int completionTimeIndex,
				int startTimeIndex, CSVErrorHandlingMode errorHandlingMode) {
			this.caseIndices = indices;
			this.completionTimeIndex = completionTimeIndex;
			this.completionTimeMapping = mappingMap.get(completionTimeIndex);
			this.errorHandlingMode = errorHandlingMode;
		}

		public int compare(String[] o1, String[] o2) {
			if (o1.length != o2.length) {
				throw new IllegalArgumentException(
						"Can only compare lines in a CSV file with the same number of columns!");
			}
			// First compare on all the case columns
			for (int i = 0; i < caseIndices.length; i++) {
				int index = caseIndices[i];
				// We treat empty and NULL cells as the same as there is no concept of a NULL cell in CSV 
				String s1 = o1[index] == null ? "" : o1[index];
				String s2 = o2[index] == null ? "" : o2[index];
				int comp = s1.compareTo(s2);
				if (comp != 0) {
					// Case ID is different on current index
					return comp;
				}
			}
			// Belongs to the same case over all indices, compare on completion time
			if (completionTimeIndex != -1) {
				// Sort by completion time
				return compareTime(completionTimeMapping, o1[completionTimeIndex], o2[completionTimeIndex]);
			} else {
				// Keep ordering -> using a stable sort algorithm
				return 0;
			}
		}

		private int compareTime(CSVMapping mapping, String t1, String t2) {
			Date d1;
			try {
				d1 = parseDate((DateFormat) mapping.getFormat(), t1);
			} catch (ParseException e) {
				if (errorHandlingMode == CSVErrorHandlingMode.ABORT_ON_ERROR) {
					throw new IllegalArgumentException("Cannot parse date: " + t1);
				} else {
					d1 = new Date(0);
				}
			}
			Date d2;
			try {
				d2 = parseDate((DateFormat) mapping.getFormat(), t2);
			} catch (ParseException e) {
				if (errorHandlingMode == CSVErrorHandlingMode.ABORT_ON_ERROR) {
					throw new IllegalArgumentException("Cannot parse date: " + t2);
				} else {
					d2 = new Date(0);
				}
			}
			return d1.compareTo(d2);
		}
	}

	/**
	 * Convert a {@link CSVFileReferenceOpenCSVImpl} into an {@link XLog} using
	 * the supplied configuration.
	 * 
	 * @param progressListener
	 * @param csvFile
	 * @param importConfig
	 * @param conversionConfig
	 * @return
	 * @throws CSVConversionException
	 * @throws CSVConversionConfigException
	 */
	public ConversionResult<XLog> doConvertCSVToXES(final ProgressListener progressListener, CSVFile csvFile,
			CSVConfig importConfig, CSVConversionConfig conversionConfig) throws CSVConversionException,
			CSVConversionConfigException {
		return convertCSV(progressListener, importConfig, conversionConfig, csvFile, new XESConversionHandlerImpl(
				importConfig, conversionConfig));
	}

	/**
	 * Converts a {@link CSVFileReferenceOpenCSVImpl} into something determined
	 * by the supplied {@link CSVConversionHandler}. Use
	 * {@link #doConvertCSVToXES(ProgressListener, CSVFileReferenceOpenCSVImpl, CSVConfig, CSVConversionConfig)}
	 * in case you want to convert to an {@link XLog}.
	 * 
	 * @param progress
	 * @param importConfig
	 * @param conversionConfig
	 * @param csvFile
	 * @param conversionHandler
	 * @return
	 * @throws CSVConversionException
	 * @throws CSVConversionConfigException
	 */
	public <R> ConversionResult<R> convertCSV(ProgressListener progress, CSVConfig importConfig,
			CSVConversionConfig conversionConfig, CSVFile csvFile, final CSVConversionHandler<R> conversionHandler)
			throws CSVConversionException, CSVConversionConfigException {

		Progress p = progress.getProgress();

		//TODO can we provide determinate progress? maybe based on bytes of CSV read
		p.setMinimum(0);
		p.setMaximum(1);
		p.setValue(0);
		p.setIndeterminate(true);

		long startCSVTime = System.currentTimeMillis();

		conversionHandler.startLog(csvFile);

		int[] caseColumnIndex = new int[conversionConfig.getCaseColumns().size()];
		int[] eventNameColumnIndex = new int[conversionConfig.getEventNameColumns().size()];
		int completionTimeColumnIndex = -1;
		int startTimeColumnIndex = -1;
		String[] header = null;

		final Map<String, Integer> headerMap = new HashMap<>();
		final Map<Integer, CSVMapping> columnMap = new HashMap<>();

		try {
			header = csvFile.readHeader(importConfig);
			for (int i = 0; i < header.length; i++) {
				String columnHeader = header[i];
				Integer oldIndex = headerMap.put(columnHeader, i);
				if (oldIndex != null) {
					throw new CSVConversionException(
							String.format(
									"Ambigous header in the CSV file: Two columns (%s, %s) have the same header %s. Please fix this in the CSV file!",
									oldIndex, i, columnHeader));
				}
				CSVMapping columnMapping = conversionConfig.getConversionMap().get(columnHeader);
				columnMap.put(i, columnMapping);
			}

			for (int i = 0; i < conversionConfig.getCaseColumns().size(); i++) {
				caseColumnIndex[i] = headerMap.get(conversionConfig.getCaseColumns().get(i));
			}
			for (int i = 0; i < conversionConfig.getEventNameColumns().size(); i++) {
				eventNameColumnIndex[i] = headerMap.get(conversionConfig.getEventNameColumns().get(i));
			}
			if (conversionConfig.getCompletionTimeColumn() != "") {
				completionTimeColumnIndex = headerMap.get(conversionConfig.getCompletionTimeColumn());
			}
			if (conversionConfig.getStartTimeColumn() != "") {
				startTimeColumnIndex = headerMap.get(conversionConfig.getStartTimeColumn());
			}
		} catch (IOException e) {
			throw new CSVConversionException("Could not read first row of CSV file with header information", e);
		}

		InputStream sortedCsvInputStream = null;
		File sortedFile = null;

		try {
			try {
				long startSortTime = System.currentTimeMillis();
				int maxMemory = (int) ((Runtime.getRuntime().maxMemory() * 0.30) / 1024 / 1024);
				progress.log(String.format(
						"Sorting CSV file (%.2f MB) by case and time using maximal %s MB of memory ...",
						(getFileSizeInBytes(csvFile) / 1024 / 1024), maxMemory));
				Ordering<String[]> caseComparator = new ImportOrdering(caseColumnIndex, columnMap,
						completionTimeColumnIndex, startTimeColumnIndex, conversionConfig.getErrorHandlingMode());
				sortedFile = CSVSorter.sortCSV(csvFile, caseComparator, importConfig, maxMemory, header.length,
						progress);
				sortedCsvInputStream = new LZFInputStream(new FileInputStream(sortedFile));
				long endSortTime = System.currentTimeMillis();
				progress.log(String.format("Finished sorting in %d seconds", (endSortTime - startSortTime) / 1000));
			} catch (IllegalArgumentException e) {
				throw new CSVSortException("Could not sort CSV file", e);
			} catch (IOException e) {
				throw new CSVSortException("Could not sort CSV file", e);
			}

			// The following code assumes that the file is sorted by cases and written to disk compressed with LZF
			progress.log("Reading cases ...");
			try (ICSVReader reader = csvFile.getCSV().createReader(sortedCsvInputStream, importConfig)) {

				int caseIndex = 0;
				int eventIndex = 0;
				int lineIndex = -1;
				String[] nextLine;
				String currentCaseId = null;

				while ((nextLine = reader.readNext()) != null && (caseIndex % 1000 != 0 || !p.isCancelled())) {
					lineIndex++;

					final String newCaseID = readCompositeAttribute(caseColumnIndex, nextLine,
							conversionConfig.getCompositeAttributeSeparator());

					// Handle new traces
					if (!newCaseID.equals(currentCaseId)) {

						if (currentCaseId != null) {
							// Finished with current case
							conversionHandler.endTrace(currentCaseId);
						}

						// Update current case id to next case id
						currentCaseId = newCaseID;

						// Create new case
						conversionHandler.startTrace(currentCaseId);
						caseIndex++;

						if (caseIndex % 1000 == 0) {
							progress.log("Reading line " + lineIndex + ", already " + caseIndex + " cases and "
									+ eventIndex + " events processed ...");
						}

					}

					// Create new event

					// Read event name
					final String eventClass = readCompositeAttribute(eventNameColumnIndex, nextLine,
							conversionConfig.getCompositeAttributeSeparator());

					// Read time stamps
					Date completionTime = parseTime(conversionHandler, completionTimeColumnIndex, columnMap, lineIndex,
							nextLine);
					Date startTime = parseTime(conversionHandler, startTimeColumnIndex, columnMap, lineIndex, nextLine);

					conversionHandler.startEvent(eventClass, completionTime, startTime);

					for (int i = 0; i < nextLine.length; i++) {
						if (Ints.contains(eventNameColumnIndex, i) || Ints.contains(caseColumnIndex, i)
								|| i == completionTimeColumnIndex || i == startTimeColumnIndex) {
							// Is already mapped to a special column, do not include again
							continue;
						}

						final String name = header[i];
						final String value = nextLine[i];

						if (!(conversionConfig.getEmptyCellHandlingMode() == CSVEmptyCellHandlingMode.SPARSE && (value == null
								|| conversionConfig.getTreatAsEmptyValues().contains(value) || value.isEmpty()))) {
							parseAttributes(progress, conversionConfig, conversionHandler, columnMap.get(i), lineIndex,
									i, name, nextLine);
						}
					}

					// Already sorted by time
					conversionHandler.endEvent();
					eventIndex++;
				}

				// Close last trace
				conversionHandler.endTrace(currentCaseId);

			} catch (IOException e) {
				throw new CSVConversionException("Error converting the CSV file to XES", e);
			}
		} finally {
			if (sortedCsvInputStream != null) {
				try {
					sortedCsvInputStream.close();
				} catch (Exception e) {
					throw new CSVConversionException("Error closing the CSV file", e);
				}
			}
			if (sortedFile != null) {
				sortedFile.delete();
			}
		}

		long endConvertTime = System.currentTimeMillis();
		progress.log(String.format("Finished reading cases in %d seconds.", (endConvertTime - startCSVTime) / 1000));

		return new ConversionResult<R>() {

			public R getResult() {
				return conversionHandler.getResult();
			}

			public boolean hasConversionErrors() {
				return conversionHandler.hasConversionErrors();
			}

			public String getConversionErrors() {
				return conversionHandler.getConversionErrors();
			}
		};
	}

	private <R> Date parseTime(final CSVConversionHandler<R> conversionHandler, int timeColumnIndex,
			final Map<Integer, CSVMapping> columnMap, int lineIndex, String[] nextLine) throws CSVConversionException {
		if (timeColumnIndex == -1) {
			return null;
		} else {
			String timeValue = nextLine[timeColumnIndex];
			try {
				return parseDate((DateFormat) columnMap.get(timeColumnIndex).getFormat(), timeValue);
			} catch (ParseException e) {
				conversionHandler.errorDetected(lineIndex, timeColumnIndex, columnMap.get(timeColumnIndex)
						.getEventAttributeName(), timeValue, e);
				return null;
			}
		}
	}

	private static double getFileSizeInBytes(CSVFile csvFile) throws IOException {
		return Files.size(csvFile.getFile());
	}

	private <R> void parseAttributes(ProgressListener progress, CSVConversionConfig conversionConfig,
			CSVConversionHandler<R> conversionHandler, CSVMapping csvMapping, int lineIndex, int columnIndex,
			String name, String[] line) throws CSVConversionException {

		String value = line[columnIndex];
		if (name == null) // TODO: Nicer would be to create names like "unknown-1", "unknown-2", etc. instead of skipping the attribute
			return;
		if (csvMapping.getDataType() == null) {
			conversionHandler.startAttribute(name, value);
		} else {
			try {
				switch (csvMapping.getDataType()) {
					case BOOLEAN :
						boolean boolVal;
						if ("J".equalsIgnoreCase(value) || "Y".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value)) {
							boolVal = true;
						} else if ("N".equalsIgnoreCase(value) || "F".equalsIgnoreCase(value)) {
							boolVal = false;
						} else {
							boolVal = Boolean.valueOf(value);
						}
						conversionHandler.startAttribute(name, boolVal);
						break;
					case CONTINUOUS :
						if (csvMapping.getFormat() != null) {
							conversionHandler.startAttribute(name, (Double) csvMapping.getFormat().parseObject(value));
						} else {
							conversionHandler.startAttribute(name, Double.parseDouble(value));
						}
						break;
					case DISCRETE :
						if (csvMapping.getFormat() != null) {
							conversionHandler.startAttribute(name, (Integer) csvMapping.getFormat().parseObject(value));
						} else {
							conversionHandler.startAttribute(name, Long.parseLong(value));
						}
						break;
					case TIME :
						conversionHandler.startAttribute(name, parseDate((DateFormat) csvMapping.getFormat(), value));
						break;
					case LITERAL :
					default :
						if (csvMapping.getFormat() != null) {
							value = ((MessageFormat) csvMapping.getFormat()).format(ObjectArrays.concat(value, line),
									new StringBuffer(), null).toString();
						}
						conversionHandler.startAttribute(name, value);
						break;
				}
			} catch (NumberFormatException e) {
				conversionHandler.errorDetected(lineIndex, columnIndex, name, value, e);
				conversionHandler.startAttribute(name, value);
			} catch (ParseException e) {
				conversionHandler.errorDetected(lineIndex, columnIndex, name, value, e);
				conversionHandler.startAttribute(name, value);
			}
		}
		conversionHandler.endAttribute();
	}

	/**
	 * Concatenates multiple composite attributes to a String representation.
	 * 
	 * @param columnIndex
	 * @param line
	 * @param compositeSeparator
	 * @return the composite attributes concatenated or an empty String in case
	 *         no columns are selected
	 */
	private static String readCompositeAttribute(int[] columnIndex, String[] line, String compositeSeparator) {
		if (columnIndex.length == 0) {
			return "";
		}
		int size = 0;
		for (int index : columnIndex) {
			String cell = line[index];
			size += (cell == null ? 0 : cell.length());
		}
		StringBuilder sb = new StringBuilder(size + columnIndex.length);
		for (int index : columnIndex) {
			String cell = line[index];
			if (cell != null) {
				sb.append(cell);
			}
			sb.append(compositeSeparator);
		}
		return sb.substring(0, sb.length() - 1);
	}

	private static Pattern INVALID_MS_PATTERN = Pattern.compile("(\\.[0-9]{3})[0-9]*");

	private static Date parseDate(DateFormat customDateFormat, String value) throws ParseException {

		if (value == null) {
			throw new ParseException("Could not parse NULL timestamp!", 0);
		}

		if (customDateFormat != null) {
			ParsePosition pos = new ParsePosition(0);
			Date date = customDateFormat.parse(value, pos);
			if (date != null) {
				return date;
			} else {
				String fixedValue = INVALID_MS_PATTERN.matcher(value).replaceFirst("$1");
				pos.setIndex(0);
				date = customDateFormat.parse(fixedValue, pos);
				if (date != null) {
					return date;
				} else {
					String pattern = "unkown";
					if (customDateFormat instanceof SimpleDateFormat) {
						pattern = ((SimpleDateFormat) customDateFormat).toPattern();
					}
					throw new ParseException("Could not parse " + value + " using pattern '" + pattern + "'",
							pos.getErrorIndex());
				}
			}
		}

		throw new ParseException("Could not parse " + value, -1);
	}

}
