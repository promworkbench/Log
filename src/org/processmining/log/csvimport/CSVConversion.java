package org.processmining.log.csvimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.CSVFileReference;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVErrorHandlingMode;
import org.processmining.log.csvimport.config.CSVImportConfig;
import org.processmining.log.csvimport.exception.CSVConversionConfigException;
import org.processmining.log.csvimport.exception.CSVConversionException;
import org.processmining.log.csvimport.exception.CSVSortException;
import org.processmining.log.csvimport.handler.CSVConversionHandler;
import org.processmining.log.csvimport.handler.XESConversionHandlerImpl;
import org.processmining.log.csvimport.ui.ConversionConfigUI;
import org.processmining.log.csvimport.ui.ImportConfigUI;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.Ordering;
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

	public enum Datatype {
		LITERAL, DISCRETE, CONTINUOUS, TIME, BOOLEAN
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
		}
	};

	static {
		for (DateFormat df : STANDARD_DATE_FORMATTERS) {
			df.setLenient(false);
		}
	}

	private static final class ImportOrdering extends Ordering<String[]> {

		private final int[] indices;

		private final SimpleDateFormat userDefinedDateFormat;

		private final int completionTimeIndex;

		private final CSVErrorHandlingMode errorHandlingMode;

		public ImportOrdering(int[] indices, SimpleDateFormat userDefinedDateFormat, int completionTimeIndex,
				int startTimeIndex, CSVErrorHandlingMode errorHandlingMode) {
			this.indices = indices;
			this.userDefinedDateFormat = userDefinedDateFormat;
			this.completionTimeIndex = completionTimeIndex;
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
				return compareTime(o1[completionTimeIndex], o2[completionTimeIndex]);
			} else {
				// Keep ordering
				return 0;
			}
		}

		private int compareTime(String t1, String t2) {
			Date d1;
			try {
				d1 = parseDate(userDefinedDateFormat, t1);
			} catch (ParseException e) {
				if (errorHandlingMode == CSVErrorHandlingMode.ABORT_ON_ERROR) {
					throw new IllegalArgumentException("Cannot parse date: " + t1);
				} else {
					d1 = new Date(0);
				}
			}
			Date d2;
			try {
				d2 = parseDate(userDefinedDateFormat, t2);
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

		conversionConfig.check();

		long startCSVTime = System.currentTimeMillis();

		conversionHandler.startLog(csvFile);

		SimpleDateFormat userDefinedDateFormat = null;
		if (conversionConfig.timeFormat != null) {
			userDefinedDateFormat = new SimpleDateFormat(conversionConfig.timeFormat);
		}

		int[] caseColumnIndex = new int[conversionConfig.caseColumns.length];
		int eventNameColumnIndex = -1;
		int completionTimeColumnIndex = -1;
		int startTimeColumnIndex = -1;
		String[] header = null;

		try {
			header = csvFile.readHeader(config);
			for (int i = 0; i < conversionConfig.caseColumns.length; i++) {
				caseColumnIndex[i] = findColumnIndex(header, conversionConfig.caseColumns[i]);
			}
			eventNameColumnIndex = findColumnIndex(header, conversionConfig.eventNameColumn);
			if (conversionConfig.completionTimeColumn != "") {
				completionTimeColumnIndex = findColumnIndex(header, conversionConfig.completionTimeColumn);
			}
			if (conversionConfig.startTimeColumn != "") {
				startTimeColumnIndex = findColumnIndex(header, conversionConfig.startTimeColumn);
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
						"Sorting CSV file (%s MB) by case and time using maximal %s MB of memory ...",
						(csvFile.getFileSizeInBytes() / 1024 / 1024), maxMemory));
				Ordering<String[]> caseComparator = new ImportOrdering(caseColumnIndex, userDefinedDateFormat,
						completionTimeColumnIndex, startTimeColumnIndex, conversionConfig.errorHandlingMode);
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

				final StringBuilder eventsWithErrors = new StringBuilder();

				while ((nextLine = reader.readNext()) != null && (caseIndex % 1000 != 0 || !p.isCancelled())) {
					lineIndex++;

					final String newCaseID = readCaseID(caseColumnIndex, nextLine);

					// Handle new traces
					if (!newCaseID.equals(currentCaseId)) {

						if (currentCaseId != null) {
							// Finished with current case
							conversionHandler.endTrace(currentCaseId);
						}

						// Reading next case id
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

						final String eventClass = eventNameColumnIndex != -1 ? nextLine[eventNameColumnIndex] : null;
						final Date completionTime = completionTimeColumnIndex != -1 ? parseDate(userDefinedDateFormat, nextLine[completionTimeColumnIndex]) : null;
						final Date startTime = startTimeColumnIndex != -1 ? parseDate(userDefinedDateFormat, nextLine[startTimeColumnIndex]) : null;
						conversionHandler.startEvent(eventClass, completionTime, startTime);

						for (int i = 0; i < nextLine.length; i++) {
							if (i == eventNameColumnIndex || i == completionTimeColumnIndex
									|| i == startTimeColumnIndex) {
								// Is already mapped to a special column, do not include again
								continue;
							}

							final String name = header[i];
							final String value = nextLine[i];

							if (considerColumn(name, value)) {

								if (!(conversionConfig.omitNULL && isNullValue(value))) {
									parseAttributes(progress, conversionConfig, conversionHandler,
											userDefinedDateFormat, lineIndex, i, name, value);
								}

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

				if (eventsWithErrors.length() > 0) {
					progress.log("Could not convert the following events:\n");
					progress.log(eventsWithErrors.toString());
				}

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
			CSVConversionHandler<R> handler, SimpleDateFormat userDefinedDateFormat, int lineIndex, int i, String name,
			String value) throws ParseException, CSVConversionException {
		Datatype dataType = conversionConfig.datatypeMapping.get(i);
		if (dataType == null) {
			handler.startAttribute(name, value);
		} else {
			try {
				switch (dataType) {
					case BOOLEAN :
						boolean boolVal;
						if ("J".equalsIgnoreCase(value) || "Y".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value)) {
							boolVal = true;
						} else if ("N".equalsIgnoreCase(value) || "F".equalsIgnoreCase(value)) {
							boolVal = false;
						} else {
							boolVal = Boolean.valueOf(value);
						}
						handler.startAttribute(name, boolVal);
						break;
					case CONTINUOUS :
						handler.startAttribute(name, Double.valueOf(value));
						break;
					case DISCRETE :
						handler.startAttribute(name, Long.valueOf(value));
						break;
					case TIME :
						handler.startAttribute(name, parseDate(userDefinedDateFormat, value));
						break;
					case LITERAL :
					default :
						handler.startAttribute(name, value);
						break;
				}
			} catch (NumberFormatException e) {
				handler.errorDetected(lineIndex, value, e);
				handler.startAttribute(name, value);
			}
		}
		handler.endAttribute();
	}

	private String readCaseID(int[] caseColumnIndex, String[] nextLine) {
		if (caseColumnIndex.length == 0) {
			return "";
		}
		int size = 0;
		for (int index : caseColumnIndex) {
			size += nextLine[index].length();
		}
		StringBuilder sb = new StringBuilder(size + caseColumnIndex.length);
		for (int index : caseColumnIndex) {
			sb.append(nextLine[index]);
			sb.append(":");
		}
		return sb.substring(0, sb.length() - 1);
	}

	private static boolean considerColumn(String name, String value) {
		return !value.isEmpty() && !specialColumn(name);
	}

	private static boolean isNullValue(String value) {
		return "NULL".equalsIgnoreCase(value);
	}

	private static boolean specialColumn(String header) {
		return XConceptExtension.KEY_NAME.equals(header) || XTimeExtension.KEY_TIMESTAMP.equals(header)
				|| XConceptExtension.KEY_INSTANCE.equals(header);
	}

	private static Date parseDate(SimpleDateFormat customDateFormat, String s) throws ParseException {
		if (customDateFormat != null) {
			return customDateFormat.parse(s);
		}
		// Milliseconds fix
		String s2 = s.replaceAll("(\\.[0-9]{3})[0-9]*", "$1");
		ParsePosition pos = new ParsePosition(0);
		for (DateFormat formatter : STANDARD_DATE_FORMATTERS) {
			pos.setIndex(0);
			Date date = formatter.parse(s2, pos);
			if (date != null) {
				return date;
			}
		}
		throw new ParseException("Could not parse " + s, pos.getErrorIndex());
	}

	private static int findColumnIndex(String[] header, String caseColumn) {
		int i = 0;
		for (String column : header) {
			if (column.equals(caseColumn)) {
				return i;
			}
			i++;
		}
		return -1;
	}

}
