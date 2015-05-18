package org.processmining.log.csvimport.config;

import java.nio.charset.Charset;

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
	
	private String charset = Charset.defaultCharset().name();
	private CSVSeperator separator = CSVSeperator.COMMA;
	private CSVQuoteCharacter quoteChar = CSVQuoteCharacter.DOUBLE_QUOTE;
	private CSVEscapeCharacter escapeChar = CSVEscapeCharacter.QUOTE;

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public CSVSeperator getSeparator() {
		return separator;
	}

	public void setSeparator(CSVSeperator separator) {
		this.separator = separator;
	}

	public CSVQuoteCharacter getQuoteChar() {
		return quoteChar;
	}

	public void setQuoteChar(CSVQuoteCharacter quoteChar) {
		this.quoteChar = quoteChar;
	}

	public CSVEscapeCharacter getEscapeChar() {
		return escapeChar;
	}

	public void setEscapeChar(CSVEscapeCharacter escapeChar) {
		this.escapeChar = escapeChar;
	}
}