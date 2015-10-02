package org.processmining.log.csvimport.config;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import org.deckfour.xes.model.XAttribute;
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

	private static final int DATA_TYPE_FORMAT_AUTO_DETECT_NUM_LINES = 10000;

	@SuppressWarnings("serial")
	public static final Set<SimpleDateFormat> STANDARD_DATE_FORMATTERS = new LinkedHashSet<SimpleDateFormat>() {
		{
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX"));
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSz"));
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ssXXX"));
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ssz"));
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
			add(new SimpleDateFormat("yyyy-MM-dd HH:mm"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"));

			add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSXXX"));
			add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSz"));
			add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS"));
			add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ssXXX"));
			add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ssz"));
			add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
			add(new SimpleDateFormat("yyyy/MM/dd HH:mm"));
			add(new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss.SSSXXX"));
			add(new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss.SSSz"));
			add(new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss.SSS"));
			add(new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ssXXX"));
			add(new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ssz"));
			add(new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss"));
			add(new SimpleDateFormat("yyyy/MM/dd'T'HH:mm"));
			add(new SimpleDateFormat("yyyy-MM-dd"));

			add(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSSXXX"));
			add(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSSz"));
			add(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS"));
			add(new SimpleDateFormat("MM/dd/yyyy HH:mm:ssXXX"));
			add(new SimpleDateFormat("MM/dd/yyyy HH:mm:ssz"));
			add(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"));
			add(new SimpleDateFormat("MM/dd/yyyy HH:mm"));
			add(new SimpleDateFormat("MM/dd/yyyy'T'HH:mm:ss.SSSXXX"));
			add(new SimpleDateFormat("MM/dd/yyyy'T'HH:mm:ss.SSSz"));
			add(new SimpleDateFormat("MM/dd/yyyy'T'HH:mm:ss.SSS"));
			add(new SimpleDateFormat("MM/dd/yyyy'T'HH:mm:ssXXX"));
			add(new SimpleDateFormat("MM/dd/yyyy'T'HH:mm:ssz"));
			add(new SimpleDateFormat("MM/dd/yyyy'T'HH:mm:ss"));
			add(new SimpleDateFormat("MM/dd/yyyy'T'HH:mm"));
			add(new SimpleDateFormat("MM/dd/yyyy"));

			add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSSXXX"));
			add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSSz"));
			add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS"));
			add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ssXXX"));
			add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ssz"));
			add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"));
			add(new SimpleDateFormat("yyyy.MM.dd HH:mm"));

			add(new SimpleDateFormat("dd-MM-yyyy:HH:mm:ss"));
			add(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"));
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
		}
	};

	private static final Set<String> EVENT_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = -4218883319932959922L;
		{
			add("event");
			add("eventname");
			add("activity");
			add("eventid");
			add("activityid");
			add("task");
			add("action");
			add("actie");
		}
	};

	private static final Set<String> START_TIME_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = 6419129336151793063L;
		{
			add("starttime");
			add("startdate");
			add("datumtijdbegin");
		}
	};

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
		}
	};

	public static final class ExtensionAttribute {

		public ExtensionAttribute(String key, XExtension extension) {
			this.key = key;
			this.extension = extension;
		}

		public XExtension extension;
		public String key;

		public String toString() {
			if (key != null) {
				return String.format("%s (%s)", key, extension.getName());
			} else {
				return "";
			}
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((extension == null) ? 0 : extension.hashCode());
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ExtensionAttribute other = (ExtensionAttribute) obj;
			if (extension == null) {
				if (other.extension != null)
					return false;
			} else if (!extension.equals(other.extension))
				return false;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}

	}

	public static final ExtensionAttribute NO_EXTENSION_ATTRIBUTE = new ExtensionAttribute(null, null);
	public static final ExtensionAttribute[] AVAILABLE_EVENT_EXTENSIONS_ATTRIBUTES;
	static {
		List<ExtensionAttribute> list = new ArrayList<>();
		list.add(NO_EXTENSION_ATTRIBUTE);
		addAttributesFromExtension(XConceptExtension.instance(), XConceptExtension.instance().getEventAttributes(),
				list);
		addAttributesFromExtension(XConceptExtension.instance(), XOrganizationalExtension.instance()
				.getEventAttributes(), list);
		addAttributesFromExtension(XConceptExtension.instance(), XTimeExtension.instance().getEventAttributes(), list);
		addAttributesFromExtension(XConceptExtension.instance(), XLifecycleExtension.instance().getEventAttributes(),
				list);
		addAttributesFromExtension(XConceptExtension.instance(), XCostExtension.instance().getEventAttributes(), list);
		AVAILABLE_EVENT_EXTENSIONS_ATTRIBUTES = list.toArray(new ExtensionAttribute[list.size()]);
	}

	private static void addAttributesFromExtension(XExtension extension, Collection<XAttribute> attributes,
			List<ExtensionAttribute> list) {
		for (XAttribute attr : attributes) {
			list.add(new ExtensionAttribute(attr.getKey(), extension));
		}
	}

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

		private Datatype dataType = Datatype.LITERAL;
		private String dataPattern = "";
		private String traceAttributeName = "";
		private String eventAttributeName = "";
		private ExtensionAttribute eventExtensionAttribute = NO_EXTENSION_ATTRIBUTE;

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

		public void setEventExtensionAttribute(ExtensionAttribute extensionAttribute) {
			this.eventExtensionAttribute = extensionAttribute;
		}

		public ExtensionAttribute getEventExtensionAttribute() {
			return eventExtensionAttribute;
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
		//TODO put those auto detection methods in a new class
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

	private void autoDetectDataTypes(CSVFile csv, Map<String, CSVMapping> conversionMap, CSVConfig csvConfig)
			throws IOException {
		try (ICSVReader reader = csv.createReader(csvConfig)) {
			String[] header = reader.readNext();
			Map<String, List<String>> valuesPerColumn = new HashMap<>();
			for (String h : header) {
				valuesPerColumn.put(h, new ArrayList<String>(DATA_TYPE_FORMAT_AUTO_DETECT_NUM_LINES));
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
				DatatypeWithPattern inferred = inferDataType(values);
				getConversionMap().get(column).setDataType(inferred.getType());
				getConversionMap().get(column).setPattern(inferred.getPattern());
			}
		}
	}

	private static boolean isInteger(String s) {
		//return isInteger(s, 19);
		try {
			Long.parseLong(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}		
	}

	private static boolean isInteger(String s, int radix) {
		if (s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			if (i == 0 && s.charAt(i) == '-') {
				if (s.length() == 1)
					return false;
				else
					continue;
			}
			if (Character.digit(s.charAt(i), radix) < 0)
				return false;
		}
		return true;
	}

	public interface DatatypeWithPattern {
		Datatype getType();

		String getPattern();
	}

	private DatatypeWithPattern inferDataType(List<String> values) {

		boolean allEmpty = true;
		for (String value : values) {
			if (value != null && !value.isEmpty()) {
				allEmpty = false;
				break;
			}
		}
		if (allEmpty)
			return new DatatypeWithPattern() {

				public Datatype getType() {
					return Datatype.LITERAL;
				}

				public String getPattern() {
					return "";
				}
			};

		boolean hasParsed = false;

		// check whether type is boolean
		boolean isBoolean = true;
		for (String value : values) {
			if (value == null || value.isEmpty() || treatAsEmptyValues.contains(value)) {
				continue;
			}
			hasParsed = true;
			//TODO what about mixed
			if (!("J".equalsIgnoreCase(value) || "Y".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value)
					|| "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "N".equalsIgnoreCase(value) || "F"
						.equalsIgnoreCase(value))) {
				isBoolean = false;
				break;
			}
		}
		if (hasParsed && isBoolean)
			return new DatatypeWithPattern() {

				public Datatype getType() {
					return Datatype.BOOLEAN;
				}

				public String getPattern() {
					return "";
				}
			};

		// check whether type is discrete
		hasParsed = false;
		boolean isDiscrete = true;
		for (String value : values) {
			if (value == null || value.isEmpty() || treatAsEmptyValues.contains(value)) {
				continue;
			}
			hasParsed = true;
			if (!isInteger(value)) {
				isDiscrete = false;
				break;
			} 
		}
		if (hasParsed && isDiscrete)
			return new DatatypeWithPattern() {

				public Datatype getType() {
					return Datatype.DISCRETE;
				}

				public String getPattern() {
					return "";
				}
			};

		// check whether type is continuous
		final Pattern CONTINUOUS_PATTERN = Pattern
				.compile("((-)?[0-9]*\\.[0-9]+)|((-)?[0-9]+(\\.[0-9]+)?(e|E)\\+[0-9]+)");
		hasParsed = false;
		boolean isContinuous = true;
		for (String value : values) {
			if (value == null || value.isEmpty() || treatAsEmptyValues.contains(value)) {
				continue;
			}
			hasParsed = true;
			if (!CONTINUOUS_PATTERN.matcher(value).matches()) {
				isContinuous = false;
				break;
			}
		}
		if (hasParsed && isContinuous)
			return new DatatypeWithPattern() {

				public Datatype getType() {
					return Datatype.CONTINUOUS;
				}

				public String getPattern() {
					return "";
				}
			};

		// check whether type is date
		boolean isConsistentDateFormat = true;
		final Pattern INVALID_MS_PATTERN = Pattern.compile("(\\.[0-9]{3})[0-9]*");
		for (SimpleDateFormat formatter : STANDARD_DATE_FORMATTERS) {
			if (canParseAllValues(values, isConsistentDateFormat, INVALID_MS_PATTERN, formatter)) {
				final String pattern = formatter.toPattern();
				return new DatatypeWithPattern() {

					public Datatype getType() {
						return Datatype.TIME;
					}

					public String getPattern() {
						return pattern;
					}
				};
			}
		}

		return new DatatypeWithPattern() {

			public Datatype getType() {
				return Datatype.LITERAL;
			}

			public String getPattern() {
				return "";
			}
		};
	}

	private boolean canParseAllValues(List<String> values, boolean isConsistentDateFormat,
			final Pattern INVALID_MS_PATTERN, DateFormat formatter) {
		boolean hasParsed = false;
		for (String value : values) {
			if (value == null || value.isEmpty() || treatAsEmptyValues.contains(value))
				continue;

			// Millisecond fix for Java SimpleDateFormat in case of a date like this 14:08:09.100000 
			// where the milliseconds would be treated as 100000 ms instead of 100ms
			String fixedValue = INVALID_MS_PATTERN.matcher(value).replaceFirst("$1");

			ParsePosition pos = new ParsePosition(0);
			pos.setIndex(0);
			Date date = formatter.parse(fixedValue, pos);

			hasParsed = true;

			// check whether date is not parsable, or date format for parsing is inconsistent
			if (date == null) {
				return false;
			}

		}
		return hasParsed;
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
		// Remove old mapping
		for (String caseColumn : this.caseColumns) {
			getConversionMap().get(caseColumn).traceAttributeName = "";
		}
		// Set new mapping
		for (String caseColumn : caseColumns) {
			getConversionMap().get(caseColumn).traceAttributeName = "concept:name";
			getConversionMap().get(caseColumn).setDataType(Datatype.LITERAL);
		}
		this.caseColumns = caseColumns;
	}

	public List<String> getEventNameColumns() {
		return ImmutableList.copyOf(eventNameColumns);
	}

	public void setEventNameColumns(List<String> eventNameColumns) {
		// Remove old mapping
		for (String eventColumn : this.eventNameColumns) {
			getConversionMap().get(eventColumn).setEventExtensionAttribute(NO_EXTENSION_ATTRIBUTE);
			getConversionMap().get(eventColumn).eventAttributeName = eventColumn;
		}
		// Set new mapping
		for (String eventColumn : eventNameColumns) {
			getConversionMap().get(eventColumn).setEventExtensionAttribute(
					new ExtensionAttribute("concept:name", XConceptExtension.instance()));
			getConversionMap().get(eventColumn).eventAttributeName = "concept:name";
			getConversionMap().get(eventColumn).setDataType(Datatype.LITERAL);
		}
		this.eventNameColumns = eventNameColumns;
	}

	public String getCompletionTimeColumn() {
		return completionTimeColumn;
	}

	public void setCompletionTimeColumn(String completionTimeColumn) {
		if (this.completionTimeColumn != null && !this.completionTimeColumn.isEmpty()) {
			// Reset mapping for old column
			getConversionMap().get(this.completionTimeColumn).setEventExtensionAttribute(NO_EXTENSION_ATTRIBUTE);
			getConversionMap().get(this.completionTimeColumn).eventAttributeName = this.completionTimeColumn;
		}
		if (completionTimeColumn != null && !completionTimeColumn.isEmpty()) {
			getConversionMap().get(completionTimeColumn).setDataType(Datatype.TIME);
			getConversionMap().get(completionTimeColumn).setEventExtensionAttribute(
					new ExtensionAttribute("time:timestamp", XTimeExtension.instance()));
			getConversionMap().get(completionTimeColumn).eventAttributeName = "time:timestamp";
		}
		this.completionTimeColumn = completionTimeColumn;
	}

	public String getStartTimeColumn() {
		return startTimeColumn;
	}

	public void setStartTimeColumn(String startTimeColumn) {
		if (this.startTimeColumn != null && !this.startTimeColumn.isEmpty()) {
			// Reset mapping for old column
			getConversionMap().get(this.startTimeColumn).setEventExtensionAttribute(NO_EXTENSION_ATTRIBUTE);
			getConversionMap().get(this.startTimeColumn).eventAttributeName = this.startTimeColumn;
		}
		if (startTimeColumn != null && !startTimeColumn.isEmpty()) {
			getConversionMap().get(startTimeColumn).setDataType(Datatype.TIME);
			getConversionMap().get(startTimeColumn).setEventExtensionAttribute(
					new ExtensionAttribute("time:timestamp", XTimeExtension.instance()));
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