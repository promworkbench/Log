package org.processmining.log.csvimport.config;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XCostExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;

/**
 * Configuration regarding the conversion of the CSV file.
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVConversionConfig {

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
		INCLUDE("Include empty cells"), EXCLUDE("Exclue empty cells");

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

	public static class CSVMapping {

		public static final String DEFAULT_DATE_PATTERN = "";
		public static final String DEFAULT_DISCRETE_PATTERN = "";
		public static final String DEFAULT_CONTINUOUS_PATTERN = "";
		public static final String DEFAULT_LITERAL_PATTERN = "";

		public static final XExtension[] AVAILABLE_EXTENSIONS = new XExtension[] { XConceptExtension.instance(),
				XTimeExtension.instance(), XLifecycleExtension.instance(), XCostExtension.instance(),
				XOrganizationalExtension.instance() };

		private Set<XExtension> extensions = new HashSet<>();
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

		public Set<XExtension> getExtensions() {
			return extensions;
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
	private String[] caseColumns;
	private String[] eventNameColumns;
	private String completionTimeColumn;
	private String startTimeColumn;

	// How to concatenate attributes built from multiple columns
	private String compositeAttributeSeparator = "|";

	// Data-type mapping
	private Map<String, CSVMapping> conversionMap = new HashMap<>();

	// Various "expert" configuration options	
	private CSVErrorHandlingMode errorHandlingMode = CSVErrorHandlingMode.OMIT_TRACE_ON_ERROR;
	private CSVEmptyCellHandlingMode emptyCellHandlingMode = CSVEmptyCellHandlingMode.EXCLUDE;
	private Set<String> treatAsEmptyValues = new HashSet<>();

	private boolean shouldGuessDataTypes = true;

	public CSVConversionConfig(String[] headers) {
		for (String columnHeader : headers) {
			if (columnHeader != null) {
				CSVMapping mapping = new CSVMapping();
				mapping.setEventAttributeName(columnHeader);
				conversionMap.put(columnHeader, mapping);	
			}
		}
		//TODO make configurable
		treatAsEmptyValues.add("");
		treatAsEmptyValues.add("NULL");
		treatAsEmptyValues.add("null");
		treatAsEmptyValues.add("NOT_SET");
	}

	public XFactory getFactory() {
		return factory;
	}

	public void setFactory(XFactory factory) {
		this.factory = factory;
	}

	public String[] getCaseColumns() {
		return caseColumns;
	}

	public void setCaseColumns(String[] caseColumns) {
		this.caseColumns = caseColumns;
		for (String caseColumn: caseColumns) {
			getConversionMap().get(caseColumn).traceAttributeName = "concept:name";
		}
	}

	public String[] getEventNameColumns() {
		return eventNameColumns;
	}

	public void setEventNameColumns(String[] eventNameColumns) {
		this.eventNameColumns = eventNameColumns;
		for (String eventColumn: eventNameColumns) {
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
			getConversionMap().get(completionTimeColumn).eventAttributeName = "time:timestamp";
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