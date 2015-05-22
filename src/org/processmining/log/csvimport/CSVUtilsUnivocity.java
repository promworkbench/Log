package org.processmining.log.csvimport;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.processmining.log.csvimport.config.CSVImportConfig;

import au.com.bytecode.opencsv.CSVWriter;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * Utilities of handling CSV files with UniVocity CSV parser.
 * 
 * @author N. Tax
 *
 */

public class CSVUtilsUnivocity {

	private static final int BUFFER_SIZE = 8192 * 4;

	private CSVUtilsUnivocity() {
		super();
	}

	public static CsvParser createCSVReader(InputStream is, CSVImportConfig importConfig) throws UnsupportedEncodingException{
		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setDelimiter(importConfig.getSeparator().getSeperatorChar());
		settings.getFormat().setQuote(importConfig.getQuoteChar().getQuoteChar());
		settings.getFormat().setCharToEscapeQuoteEscaping(importConfig.getEscapeChar().getEscapeChar());
		CsvParser parser = new CsvParser(settings);
		parser.beginParsing(new BufferedReader(new InputStreamReader(is, importConfig.getCharset()), BUFFER_SIZE));
		return parser;
	}

	public static CSVWriter createCSVWriter(OutputStream os, CSVImportConfig importConfig) {
		// TODO: implement this method
		return null;
	}

}
