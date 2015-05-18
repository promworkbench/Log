package org.processmining.log.csvimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.CSVFileReference;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVEmptyCellHandlingMode;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVErrorHandlingMode;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVMapping;
import org.processmining.log.csvimport.config.CSVImportConfig;
import org.processmining.log.csvimport.exception.CSVConversionConfigException;
import org.processmining.log.csvimport.exception.CSVConversionException;
import org.processmining.log.csvimport.exception.CSVSortException;
import org.processmining.log.csvimport.handler.CSVConversionHandler;
import org.processmining.log.csvimport.handler.XESConversionHandlerImpl;
import org.processmining.log.csvimport.ui.ConversionConfigUI;
import org.processmining.log.csvimport.ui.ImportConfigUI;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.ning.compress.lzf.LZFInputStream;

/**
 * Conversion from CSV to a structure like XES.
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVConversion {

	public interface ProgressListener {
		Progress getProgress();

		void log(String message);
	}

	@SuppressWarnings("serial")
	static final Set<DateFormat> STANDARD_DATE_FORMATTERS = new LinkedHashSet<DateFormat>() {
		{
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
			add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
			add(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"));
			add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm"));
			add(new SimpleDateFormat("yyyy-MM-dd"));
			add(new SimpleDateFormat("MM/dd/yyyy HH:mm"));
			add(new SimpleDateFormat("MM/dd/yyyy"));
			add(new SimpleDateFormat("dd-MM-yyyy:HH:mm:ss"));
		}
	};

	static {
		for (DateFormat df : STANDARD_DATE_FORMATTERS) {
			df.setLenient(false);
		}
	}

	private static final class ImportOrdering extends Ordering<String[]> {

		private final int[] indices;

		private final int completionTimeIndex;
		private final CSVMapping completionTimeMapping;

		private final CSVErrorHandlingMode errorHandlingMode;

		public ImportOrdering(int[] indices, Map<Integer, CSVMapping> mappingMap, int completionTimeIndex,
				int startTimeIndex, CSVErrorHandlingMode errorHandlingMode) {
			this.indices = indices;
			this.completionTimeIndex = completionTimeIndex;
			this.completionTimeMapping = mappingMap.get(completionTimeIndex);
			this.errorHandlingMode = errorHandlingMode;
		}

		public int compare(String[] o1, String[] o2) {
			if (o1.length != o2.length) {
				throw new IllegalArgumentException("Can only compare String arrays of the exact same size!");
			}
			for (int i = 0; i < indices.length; i++) {
				int index = indices[i];
				int comp = o1[index].compareTo(o2[index]);
				if (comp != 0) {
					// Different on current index
					return comp;
				}
			}
			// Same over all indices
			if (completionTimeIndex != -1) {
				// Sort by completion time
				//TODO what to do with start & completion time
				return compareTime(completionTimeMapping, o1[completionTimeIndex], o2[completionTimeIndex]);
			} else {
				// Keep ordering
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
	 * Convert a {@link CSVFileReference} into an {@link XLog} using the
	 * supplied configuration.
	 * 
	 * @param progressListener
	 * @param csvFile
	 * @param importConfig
	 * @param conversionConfig
	 * @return
	 * @throws CSVConversionException
	 * @throws CSVConversionConfigException
	 */
	public XLog doConvertCSVToXES(final ProgressListener progressListener, CSVFile csvFile,
			CSVImportConfig importConfig, CSVConversionConfig conversionConfig) throws CSVConversionException,
			CSVConversionConfigException {
		return convertCSV(progressListener, importConfig, conversionConfig, csvFile, new XESConversionHandlerImpl(
				progressListener, importConfig, conversionConfig));
	}

	public static CSVImportConfig queryImportConfig(UIPluginContext context, CSVFile csv) throws UserCancelledException {
		ImportConfigUI importConfigUI = new ImportConfigUI(csv);
		InteractionResult result = context.showConfiguration("Configure Import of CSV", importConfigUI);
		if (result == InteractionResult.CONTINUE || result == InteractionResult.FINISHED) {
			return importConfigUI.getImportConfig();
		} else {
			throw new UserCancelledException();
		}
	}

	public static CSVConversionConfig queryConversionConfig(UIPluginContext context, CSVFile csv,
			CSVImportConfig importConfig) throws UserCancelledException, IOException {
		try (ConversionConfigUI conversionConfigUI = new ConversionConfigUI(csv, importConfig)) {
			InteractionResult result = context.showConfiguration("Configure Conversion from CSV to XES",
					conversionConfigUI);
			if (result == InteractionResult.CONTINUE || result == InteractionResult.FINISHED) {
				return conversionConfigUI.getConversionConfig();
			} else {
				throw new UserCancelledException();
			}
		}
	}

	/**
	 * Converts a {@link CSVFileReference} into something determined by the
	 * supplied {@link CSVConversionHandler}. Use
	 * {@link #doConvertCSVToXES(ProgressListener, CSVFileReference, CSVImportConfig, CSVConversionConfig)}
	 * in case you want to convert to an {@link XLog}.
	 * 
	 * @param progress
	 * @param config
	 * @param conversionConfig
	 * @param csvFile
	 * @param conversionHandler
	 * @return
	 * @throws CSVConversionException
	 * @throws CSVConversionConfigException
	 */
	public <R> R convertCSV(ProgressListener progress, CSVImportConfig config, CSVConversionConfig conversionConfig,
			CSVFile csvFile, CSVConversionHandler<R> conversionHandler) throws CSVConversionException,
			CSVConversionConfigException {

		Progress p = progress.getProgress();
		//TODO can we provide determinate progress? maybe based on bytes of CSV read
		p.setMinimum(0);
		p.setMaximum(1);
		p.setValue(0);
		p.setIndeterminate(true);

		long startCSVTime = System.currentTimeMillis();

		conversionHandler.startLog(csvFile);

		int[] caseColumnIndex = new int[conversionConfig.getCaseColumns().length];
		int[] eventNameColumnIndex = new int[conversionConfig.getEventNameColumns().length];
		int completionTimeColumnIndex = -1;
		int startTimeColumnIndex = -1;
		String[] header = null;

		final Map<String, Integer> headerMap = new HashMap<>();
		final Map<Integer, CSVMapping> mappingMap = new HashMap<>();

		try {
			header = csvFile.readHeader(config);
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
				mappingMap.put(i, columnMapping);
			}

			for (int i = 0; i < conversionConfig.getCaseColumns().length; i++) {
				caseColumnIndex[i] = headerMap.get(conversionConfig.getCaseColumns()[i]);
			}
			for (int i = 0; i < conversionConfig.getEventNameColumns().length; i++) {
				eventNameColumnIndex[i] = headerMap.get(conversionConfig.getEventNameColumns()[i]);
			}
			if (conversionConfig.getCompletionTimeColumn() != "") {
				completionTimeColumnIndex = headerMap.get(conversionConfig.getCompletionTimeColumn());
			}
			if (conversionConfig.getStartTimeColumn() != "") {
				startTimeColumnIndex = headerMap.get(conversionConfig.getStartTimeColumn());
			}
		} catch (IOException e) {
			throw new CSVConversionException("Could not read CSV file header", e);
		}

		InputStream sortedCsvInputStream = null;
		File sortedFile = null;

		try {
			try {
				long startSortTime = System.currentTimeMillis();
				int maxMemory = (int) ((Runtime.getRuntime().maxMemory() * 0.50) / 1024 / 1024);
				progress.log(String.format(
						"Sorting CSV file (%.2f MB) by case and time using maximal %s MB of memory ...",
						((double) csvFile.getFileSizeInBytes() / 1024 / 1024), maxMemory));
				Ordering<String[]> caseComparator = new ImportOrdering(caseColumnIndex, mappingMap,
						completionTimeColumnIndex, startTimeColumnIndex, conversionConfig.getErrorHandlingMode());
				sortedFile = CSVSorter.sortCSV(csvFile, caseComparator, config, maxMemory, header.length, progress);
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
			try (CSVReader reader = CSVUtils.createCSVReader(sortedCsvInputStream, config)) {

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
					try {

						// Read event name
						final String eventClass = readCompositeAttribute(eventNameColumnIndex, nextLine,
								conversionConfig.getCompositeAttributeSeparator());

						// Read timestamps
						final Date completionTime = completionTimeColumnIndex != -1 ? parseDate((DateFormat) mappingMap
								.get(completionTimeColumnIndex).getFormat(), nextLine[completionTimeColumnIndex])
								: null;
						final Date startTime = startTimeColumnIndex != -1 ? parseDate(
								(DateFormat) mappingMap.get(startTimeColumnIndex).getFormat(),
								nextLine[startTimeColumnIndex]) : null;

						conversionHandler.startEvent(eventClass, completionTime, startTime);

						for (int i = 0; i < nextLine.length; i++) {
							if (Ints.contains(eventNameColumnIndex, i) || Ints.contains(caseColumnIndex, i)
									|| i == completionTimeColumnIndex || i == startTimeColumnIndex) {
								// Is already mapped to a special column, do not include again
								continue;
							}

							final String name = header[i];
							final String value = nextLine[i];

							if (!(conversionConfig.getEmptyCellHandlingMode() == CSVEmptyCellHandlingMode.EXCLUDE && (conversionConfig
									.getTreatAsEmptyValues().contains(value) || value.isEmpty()))) {
								parseAttributes(progress, conversionConfig, conversionHandler, mappingMap.get(i),
										lineIndex, i, name, nextLine);
							}
						}

						// Already sorted by time
						conversionHandler.endEvent();
						eventIndex++;

					} catch (ParseException e) {
						conversionHandler.errorDetected(lineIndex, nextLine, e);
					}
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

		return conversionHandler.getResult();
	}

	private <R> void parseAttributes(ProgressListener progress, CSVConversionConfig conversionConfig,
			CSVConversionHandler<R> conversionHandler, CSVMapping csvMapping, int lineIndex, int columnIndex,
			String name, String[] line) throws ParseException, CSVConversionException {
		String value = line[columnIndex];
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
				conversionHandler.errorDetected(lineIndex, value, e);
				conversionHandler.startAttribute(name, value);
			}
		}
		conversionHandler.endAttribute();
	}

	/**
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
			size += line[index].length();
		}
		StringBuilder sb = new StringBuilder(size + columnIndex.length);
		for (int index : columnIndex) {
			sb.append(line[index]);
			sb.append(compositeSeparator);
		}
		return sb.substring(0, sb.length() - 1);
	}

	private static Pattern INVALID_MS_PATTERN = Pattern.compile("(\\.[0-9]{3})[0-9]*");

	private static Date parseDate(DateFormat customDateFormat, String value) throws ParseException {

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
					throw new ParseException("Could not parse " + value, pos.getErrorIndex());			
				}
			}
		}
		ParsePosition pos = new ParsePosition(0);
		for (DateFormat formatter : STANDARD_DATE_FORMATTERS) {
			pos.setIndex(0);
			Date date = formatter.parse(value, pos);
			if (date != null) {
				return date;
			}
		}
		
		// Milliseconds fix
		String fixedValue = INVALID_MS_PATTERN.matcher(value).replaceFirst("$1");
		for (DateFormat formatter : STANDARD_DATE_FORMATTERS) {
			pos.setIndex(0);
			Date date = formatter.parse(fixedValue, pos);
			if (date != null) {
				return date;
			}
		}		
		
		throw new ParseException("Could not parse " + value, pos.getErrorIndex());		
	}

}
