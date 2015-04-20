package org.processmining.log.csvimport.config;

import java.nio.charset.Charset;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.log.csvimport.CSVEscapeCharacter;
import org.processmining.log.csvimport.CSVQuoteCharacter;
import org.processmining.log.csvimport.CSVSeperator;

/**
 * Configuration for the import of the CSV
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVImportConfig {
	public XFactory factory = XFactoryRegistry.instance().currentDefault();
	public String charset = Charset.defaultCharset().name();
	public CSVSeperator separator = CSVSeperator.COMMA;
	public CSVQuoteCharacter quoteChar = CSVQuoteCharacter.QUOTE;
	public CSVEscapeCharacter escapeChar = CSVEscapeCharacter.QUOTE;
}