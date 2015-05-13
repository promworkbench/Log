package org.processmining.log.csvimport.config;

import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;

/**
 * Configuration regarding the conversion of the CSV file.
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVConversionConfig {
	
	private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	
	public enum CSVErrorHandlingMode {
		ABORT_ON_ERROR, OMIT_TRACE_ON_ERROR, OMIT_EVENT_ON_ERROR, BEST_EFFORT
	}
	
	public enum CSVEmptyCellHandlingMode {
		INCLUDE, EXCLUDE 
	}
	
	public enum Datatype {
		LITERAL, DISCRETE, CONTINUOUS, TIME, BOOLEAN
	}
	
	public static class CSVMapping {
		
		private Datatype dataType = Datatype.LITERAL;
		private Format dataFormat;

		public Datatype getDataType() {
			return dataType;
		}

		public void setDataType(Datatype dataType) {
			this.dataType = dataType;
		}

		public Format getFormat() {
			return dataFormat;
		}

		public void setFormat(Format dataFormat) {
			this.dataFormat = dataFormat;
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
		for (String columnHeader: headers) {
			CSVMapping mapping = new CSVMapping();
			mapping.setFormat(new MessageFormat("{0}"));
			conversionMap.put(columnHeader, mapping);			
		}
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
	}

	public String[] getEventNameColumns() {
		return eventNameColumns;
	}

	public void setEventNameColumns(String[] eventNameColumns) {
		this.eventNameColumns = eventNameColumns;
	}

	public String getCompletionTimeColumn() {
		return completionTimeColumn;
	}

	public void setCompletionTimeColumn(String completionTimeColumn) {
		if (completionTimeColumn != null && !completionTimeColumn.isEmpty()) {
			getConversionMap().get(completionTimeColumn).dataType = Datatype.TIME;
			getConversionMap().get(completionTimeColumn).dataFormat = new SimpleDateFormat(DEFAULT_FORMAT);
		}
		this.completionTimeColumn = completionTimeColumn;
	}

	public String getStartTimeColumn() {
		return startTimeColumn;
	}

	public void setStartTimeColumn(String startTimeColumn) {
		if (startTimeColumn != null && !startTimeColumn.isEmpty()) {
			getConversionMap().get(startTimeColumn).dataType = Datatype.TIME;
			getConversionMap().get(startTimeColumn).dataFormat = new SimpleDateFormat(DEFAULT_FORMAT);
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