package org.processmining.log.csvimport.handler;

import java.util.Date;

import org.processmining.log.csv.CSVFile;
import org.processmining.log.csvimport.exception.CSVConversionException;

/**
 * Handler for the conversion following a visitor-like pattern.
 * 
 * @author F. Mannhardt
 *
 * @param <R>
 */
public interface CSVConversionHandler<R> {
	
	void startLog(CSVFile inputFile);

	void startTrace(String caseId);

	void endTrace(String caseId);

	void startEvent(String eventClass, Date completionTime, Date startTime);

	void startAttribute(String name, String value);

	void startAttribute(String name, long value);

	void startAttribute(String name, double value);
	
	void startAttribute(String name, Date value);
	
	void startAttribute(String name, boolean value);
	
	void endAttribute();

	void endEvent();
	
	void errorDetected(int line, Object content, Exception e) throws CSVConversionException;

	R getResult();

}
