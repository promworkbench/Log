package org.processmining.log.csvimport.config;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.processmining.log.csvimport.CSVConversion.Datatype;
import org.processmining.log.csvimport.exception.CSVConversionConfigException;

/**
 * Configuration regarding the conversion of the CSV file.
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVConversionConfig {
	
	public enum CSVErrorHandlingMode {
		ABORT_ON_ERROR, OMIT_TRACE_ON_ERROR, OMIT_EVENT_ON_ERROR, BEST_EFFORT
	}

	// Mapping to the XES standard extensions
	public String[] caseColumns;
	public String eventNameColumn;
	public String completionTimeColumn;
	public String startTimeColumn;

	// Various "expert" configuration options
	public boolean shouldGuessDataTypes = true;
	public CSVErrorHandlingMode errorHandlingMode = CSVErrorHandlingMode.OMIT_TRACE_ON_ERROR;
	public boolean omitNULL = true;
	
	public Map<Integer, Datatype> datatypeMapping = new HashMap<>();
	
	/**
	 * A format string that needs to be formatted according to {@link SimpleDateFormat}. 
	 */
	public String timeFormat;

	public void check() throws CSVConversionConfigException {
		try {
			if (timeFormat != null) {
				new SimpleDateFormat(timeFormat);
			}
		} catch (IllegalArgumentException e) {
			throw new CSVConversionConfigException("Invalid Time Format", e);
		}
	}

}