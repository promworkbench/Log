package org.processmining.log.csvimport.config;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XCostExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.ICSVReader;
import org.processmining.log.csv.config.CSVConfig;

import com.google.common.collect.ImmutableList;

/**
 * Configuration regarding the conversion of the CSV file.
 * 
 * @author F. Mannhardt
 * 
 */
public final class CSVConversionConfig {
	
	private static final int DATA_TYPE_FORMAT_AUTO_DETECT_NUM_LINES = 100;
	
	@SuppressWarnings("serial")
	public static final Set<DateFormat> STANDARD_DATE_FORMATTERS = new LinkedHashSet<DateFormat>() {
		{
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
			add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS"));
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
			add(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"));
			add(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss"));
			add(new SimpleDateFormat("MM-dd-yyyy HH:mm"));
			add(new SimpleDateFormat("MM-dd-yyyy"));
			add(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"));
			add(new SimpleDateFormat("dd-MM-yyyy HH:mm"));
			add(new SimpleDateFormat("dd-MM-yyyy"));
		}
	};

	static {
		for (DateFormat df : STANDARD_DATE_FORMATTERS) {
			df.setLenient(false);
		}
	}

	
	private static final Set<String> CASE_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = 1113995381788343439L;
	{
		add("case");
		add("trace");
		add("traceid");
		add("caseid");
	}};
	
	private static final Set<String> EVENT_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = -4218883319932959922L;
	{
		add("event");
		add("eventname");
		add("activity");
		add("eventid");
		add("activityid");
	}};
	
	private static final Set<String> START_TIME_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = 6419129336151793063L;
	{
		add("starttime");
		add("startdate");
		add("datumtijdbegin");
	}};

	
	private static final Set<String> COMPLETION_TIME_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = 6419129336151793063L;
	{
		add("completiontime");
		add("time");
		add("date");
		add("enddate");
		add("endtime");
		add("timestamp");
		add("datetime");
		add("date");
		add("eventtime");
		add("eindtijd");
		add("tijd");
		add("datum");
		add("datumtijdeind");
	}};

	public enum CSVErrorHandlingMode {
		ABORT_ON_ERROR("Stop on Error"), OMIT_TRACE_ON_ERROR("Omit Trace on Error"), OMIT_EVENT_ON_ERROR(
				"Omit Event on Error"), BEST_EFFORT("Omit Attribute on Error");

		private String desc;

		CSVErrorHandlingMode(String desc) {
			this.desc = desc;
		}

		@Override
		public String toString() {
			return desc;
		}
	}

	public enum CSVEmptyCellHandlingMode {
		DENSE("Dense (Include empty cells)"), SPARSE("Sparse (Exclude empty cells)");

		private String desc;

		CSVEmptyCellHandlingMode(String desc) {
			this.desc = desc;
		}

		@Override
		public String toString() {
			return desc;
		}

	}

	public enum Datatype {
		LITERAL, DISCRETE, CONTINUOUS, TIME, BOOLEAN
	}

	public static final class CSVMapping {

		public static final String DEFAULT_DATE_PATTERN = "";
		public static final String DEFAULT_DISCRETE_PATTERN = "";
		public static final String DEFAULT_CONTINUOUS_PATTERN = "";
		public static final String DEFAULT_LITERAL_PATTERN = "";

		public static final XExtension[] AVAILABLE_EXTENSIONS = new XExtension[] { XConceptExtension.instance(),
				XTimeExtension.instance(), XLifecycleExtension.instance(), XCostExtension.instance(),
				XOrganizationalExtension.instance() };

		private XExtension extension = null;
		private Datatype dataType = Datatype.LITERAL;
		private String dataPattern = "";
		private String traceAttributeName = "";
		private String eventAttributeName = "";

		public Datatype getDataType() {
			return dataType;
		}

		public void setDataType(Datatype dataType) {
			this.dataType = dataType;
		}

		public String getPattern() {
			return dataPattern;
		}

		public Format getFormat() {
			switch (getDataType()) {
				case BOOLEAN :
					return null;
				case CONTINUOUS :
					if (dataPattern.isEmpty()) {
						return null;
					} else {
						return new DecimalFormat(dataPattern);
					}
				case DISCRETE :
					if (dataPattern.isEmpty()) {
						return null;
					} else {
						DecimalFormat integerFormat = new DecimalFormat(dataPattern);
						integerFormat.setMaximumFractionDigits(0);
						integerFormat.setDecimalSeparatorAlwaysShown(false);
						integerFormat.setParseIntegerOnly(true);
						return integerFormat;
					}
				case LITERAL :
					if (dataPattern.isEmpty()) {
						return null;
					} else {
						return new MessageFormat(dataPattern);
					}
				case TIME :
					if (dataPattern.isEmpty()) {
						return null;
					} else {
						return new SimpleDateFormat(dataPattern);
					}
			}
			throw new RuntimeException("Unkown data type " + getDataType());
		}

		public void setPattern(String dataPattern) {
			this.dataPattern = dataPattern;
		}

		public void setExtension(XExtension extension) {
			this.extension = extension;
		}

		public XExtension getExtension() {
			return extension;
		}

		public String getTraceAttributeName() {
			return traceAttributeName;
		}

		public void setTraceAttributeName(String traceAttributeName) {
			this.traceAttributeName = traceAttributeName;
		}

		public String getEventAttributeName() {
			return eventAttributeName;
		}

		public void setEventAttributeName(String eventAttributeName) {
			this.eventAttributeName = eventAttributeName;
		}

	}

	// XFactory to use for conversion if XESConversionHandler is used
	private XFactory factory = XFactoryRegistry.instance().currentDefault();

	// Mapping to some of the XES standard extensions
	private List<String> caseColumns = Collections.emptyList();
	private List<String> eventNameColumns = Collections.emptyList();
	private String completionTimeColumn;
	private String startTimeColumn;

	// How to concatenate attributes built from multiple columns
	private String compositeAttributeSeparator = "|";

	// Data-type mapping
	private Map<String, CSVMapping> conversionMap = new HashMap<>();

	// Various "expert" configuration options	
	private CSVErrorHandlingMode errorHandlingMode = CSVErrorHandlingMode.OMIT_TRACE_ON_ERROR;
	private CSVEmptyCellHandlingMode emptyCellHandlingMode = CSVEmptyCellHandlingMode.SPARSE;
	private Set<String> treatAsEmptyValues = new HashSet<>();

	private boolean shouldGuessDataTypes = true;

	// Internal only
	private final CSVFile csvFile;
	private final CSVConfig csvConfig;

	public CSVConversionConfig(CSVFile csvFile, CSVConfig csvConfig) throws IOException {
		this.csvFile = csvFile;
		this.csvConfig = csvConfig;
		
		String[] headers = csvFile.readHeader(csvConfig);
		for (String columnHeader : headers) {
			if (columnHeader != null) {
				CSVMapping mapping = new CSVMapping();
				mapping.setEventAttributeName(columnHeader);
				conversionMap.put(columnHeader, mapping);
			}
		}
		
		// Standard settings for empty/NULL or N/A values
		treatAsEmptyValues.add("");
		treatAsEmptyValues.add("NULL");
		treatAsEmptyValues.add("null");
		treatAsEmptyValues.add("NOT_SET");
		treatAsEmptyValues.add("N/A");
		treatAsEmptyValues.add("n/a");
	}

	public void autoDetect() throws IOException {
		String[] headers = csvFile.readHeader(csvConfig);
		autoDetectCaseColumn(headers);
		autoDetectEventColumn(headers);
		autoDetectCompletionTimeColumn(headers);
		autoDetectStartTimeColumn(headers);
		autoDetectDataTypes(csvFile, getConversionMap(), csvConfig);
	}
	
	private void autoDetectCaseColumn(String[] headers) {
		List<String> caseColumns = new ArrayList<>();
		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];
						
			if (CASE_COLUMN_IDS.contains(header.toLowerCase(Locale.US).trim())) {
				caseColumns.add(header);
				return;
			}
		}
		setCaseColumns(caseColumns);
	}

	private void autoDetectEventColumn(String[] headers) {
		List<String> eventColumns = new ArrayList<>();
		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if (EVENT_COLUMN_IDS.contains(header.toLowerCase(Locale.US).trim())) {
				eventColumns.add(header);
				return;
			}
		}
		setEventNameColumns(eventColumns);
	}

	private void autoDetectCompletionTimeColumn(String[] headers) {
		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if (COMPLETION_TIME_COLUMN_IDS.contains(header.toLowerCase(Locale.US).trim())) {
				setCompletionTimeColumn(header);
				return;
			}
		}
	}
	
	private void autoDetectStartTimeColumn(String[] headers) {
		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if (START_TIME_COLUMN_IDS.contains(header.toLowerCase(Locale.US).trim())) {
				setStartTimeColumn(header);
				return;
			}
		}
	}

	private void autoDetectDataTypes(CSVFile csv, Map<String, CSVMapping> conversionMap, CSVConfig csvConfig) throws IOException {
		try (ICSVReader reader = csv.createReader(csvConfig)) {
			String[] header = reader.readNext();
			Map<String, List<String>> valuesPerColumn = new HashMap<>();
			for (String h : header) {
				valuesPerColumn.put(h, new ArrayList<String>());
			}
			// now read some lines or so to guess the data type
			for (int i = 0; i < DATA_TYPE_FORMAT_AUTO_DETECT_NUM_LINES; i++) {
				String[] cells = reader.readNext();
				if (cells == null) {
					break;
				}
				for (int j = 0; j < cells.length; j++) {
					List<String> values = valuesPerColumn.get(header[j]);
					values.add(cells[j]);
					valuesPerColumn.put(header[j], values);
				}
			}
			// now we can guess the data type
			for (String column : header) {
				List<String> values = valuesPerColumn.get(column);
				Datatype inferred = inferDataType(values);
				getConversionMap().get(column).setDataType(inferred);
			}
		}
	}
	
	private static boolean isInteger(String s) {
	    return isInteger(s,19);
	}

	private static boolean isInteger(String s, int radix) {
	    if(s.isEmpty()) return false;
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) return false;
	            else continue;
	        }
	        if(Character.digit(s.charAt(i),radix) < 0) return false;
	    }
	    return true;
	}
	
	private Datatype inferDataType(List<String> values){
		boolean allEmpty = true;
		for(String value : values){
			if(value != null && !value.isEmpty()){
				allEmpty = false;
				break;
			}
		}
		if(allEmpty)
			return Datatype.LITERAL;
		
		// check whether type is boolean
		boolean isBoolean = true;
		for (String value : values){
			if(value!=null && !value.isEmpty() && !(value.toLowerCase().equals("true".toLowerCase()) || value.toLowerCase().equals("false".toLowerCase()))){
				isBoolean = false;
				break;
			}
		}
		if(isBoolean)
			return Datatype.BOOLEAN;
		
		// check whether type is discrete
		boolean isDiscrete = true;
		for (String value : values){
			if(value!=null && !value.isEmpty() && !isInteger(value)){
				isDiscrete = false;
				break;
			}
		}
		if(isDiscrete)
			return Datatype.DISCRETE;
		
		// check whether type is continuous
		final Pattern CONTINUOUS_PATTERN = Pattern.compile("((-)?[0-9]*\\.[0-9]+)|((-)?[0-9]+(\\.[0-9]+)?(e|E)\\+[0-9]+)");
		boolean isContinuous = true;
		for (String value : values){
			if(value!=null && !value.isEmpty() && !CONTINUOUS_PATTERN.matcher(value).matches()){
				isContinuous = false;
				break;
			}
		}
		if(isContinuous)
			return Datatype.CONTINUOUS;

		// check whether type is date
		boolean isConsistentDateFormat = true;
		DateFormat globalFormatter = null;
		final Pattern INVALID_MS_PATTERN = Pattern.compile("(\\.[0-9]{3})[0-9]*");
		for (String value : values){
			if(value==null || value.isEmpty())
				continue;
			ParsePosition pos = new ParsePosition(0);
			String fixedValue = INVALID_MS_PATTERN.matcher(value).replaceFirst("$1");
			for (DateFormat formatter : STANDARD_DATE_FORMATTERS) {
				pos.setIndex(0);
				Date date = formatter.parse(fixedValue, pos);
				if (date != null && globalFormatter==null){
					globalFormatter = formatter;
				}
				// check whether date is not parsable, or date format for parsing is inconsistent
				if (date != null && (globalFormatter!=null && !globalFormatter.equals(formatter))) {
					isConsistentDateFormat = false;
					break;
				}
			}
		}
		if(isConsistentDateFormat && globalFormatter != null)
			return Datatype.TIME;
		
		return Datatype.LITERAL;
	}

	public XFactory getFactory() {
		return factory;
	}

	public void setFactory(XFactory factory) {
		this.factory = factory;
	}

	public List<String> getCaseColumns() {
		return ImmutableList.copyOf(caseColumns);
	}

	public void setCaseColumns(List<String> caseColumns) {
		this.caseColumns = caseColumns;
		for (String caseColumn : caseColumns) {
			getConversionMap().get(caseColumn).traceAttributeName = "concept:name";
		}
	}

	public List<String> getEventNameColumns() {
		return ImmutableList.copyOf(eventNameColumns);
	}

	public void setEventNameColumns(List<String> eventNameColumns) {
		this.eventNameColumns = eventNameColumns;
		for (String eventColumn : eventNameColumns) {
			getConversionMap().get(eventColumn).eventAttributeName = "concept:name";
		}
	}

	public String getCompletionTimeColumn() {
		return completionTimeColumn;
	}

	public void setCompletionTimeColumn(String completionTimeColumn) {
		if (completionTimeColumn != null && !completionTimeColumn.isEmpty()) {
			getConversionMap().get(completionTimeColumn).setDataType(Datatype.TIME);
			getConversionMap().get(completionTimeColumn).setPattern(CSVMapping.DEFAULT_DATE_PATTERN);
			getConversionMap().get(completionTimeColumn).eventAttributeName = "time:timestamp";
		}
		this.completionTimeColumn = completionTimeColumn;
	}

	public String getStartTimeColumn() {
		return startTimeColumn;
	}

	public void setStartTimeColumn(String startTimeColumn) {
		if (startTimeColumn != null && !startTimeColumn.isEmpty()) {
			getConversionMap().get(startTimeColumn).setDataType(Datatype.TIME);
			getConversionMap().get(startTimeColumn).setPattern(CSVMapping.DEFAULT_DATE_PATTERN);
			getConversionMap().get(startTimeColumn).eventAttributeName = "time:timestamp";
		}
		this.startTimeColumn = startTimeColumn;
	}

	public String getCompositeAttributeSeparator() {
		return compositeAttributeSeparator;
	}

	public void setCompositeAttributeSeparator(String compositeAttributeSeparator) {
		this.compositeAttributeSeparator = compositeAttributeSeparator;
	}

	public boolean isShouldGuessDataTypes() {
		return shouldGuessDataTypes;
	}

	public void setShouldGuessDataTypes(boolean shouldGuessDataTypes) {
		this.shouldGuessDataTypes = shouldGuessDataTypes;
	}

	public CSVErrorHandlingMode getErrorHandlingMode() {
		return errorHandlingMode;
	}

	public void setErrorHandlingMode(CSVErrorHandlingMode errorHandlingMode) {
		this.errorHandlingMode = errorHandlingMode;
	}

	public Map<String, CSVMapping> getConversionMap() {
		return conversionMap;
	}

	public Set<String> getTreatAsEmptyValues() {
		return treatAsEmptyValues;
	}

	public CSVEmptyCellHandlingMode getEmptyCellHandlingMode() {
		return emptyCellHandlingMode;
	}

	public void setEmptyCellHandlingMode(CSVEmptyCellHandlingMode emptyCellHandlingMode) {
		this.emptyCellHandlingMode = emptyCellHandlingMode;
	}

}