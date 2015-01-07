package org.processmining.log.csvimport;

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
	
	public String[] caseColumns;
	public String eventNameColumn;
	public String completionTimeColumn;
	public String startTimeColumn;

	public boolean isRepairDataTypes = true;
	public boolean strictMode = false;
	public boolean omitNULL = true;
	
	public Map<Integer, Datatype> datatypeMapping = new HashMap<>();
	
	/**
	 * A format string that needs to be formatted according to {@link SimpleDateFormat}. 
	 */
	public String timeFormat;

	public void check() throws CSVConversionConfigException {
		if (caseColumns == null || caseColumns.length == 0) {
			throw new CSVConversionConfigException("Configuration is missing the case column!");
		}
		if (eventNameColumn == null || eventNameColumn.isEmpty()) {
			throw new CSVConversionConfigException("Configuration is missing the event column!");
		}
		if (completionTimeColumn == null && startTimeColumn == null) {
			throw new CSVConversionConfigException("Configuration is missing both time columns!");
		}
		try {
			if (timeFormat != null) {
				new SimpleDateFormat(timeFormat);
			}
		} catch (IllegalArgumentException e) {
			throw new CSVConversionConfigException("Invalid Time Format", e);
		}
	}

}