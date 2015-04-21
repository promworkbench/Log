package org.processmining.log.csvimport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.processmining.log.csv.CSVFile;
import org.processmining.log.csvimport.config.CSVImportConfig;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.io.Files;

/**
 * Utilities of handling CSV files with OpenCSV.
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVUtils {

	private static final int BUFFER_SIZE = 8192 * 4;

	private CSVUtils() {
		super();
	}

	public static CSVReader createCSVReader(InputStream is, CSVImportConfig importConfig)
			throws UnsupportedEncodingException {
		if (importConfig.quoteChar == null) {
			return new CSVReader(new BufferedReader(new InputStreamReader(is, importConfig.charset), BUFFER_SIZE),
					importConfig.separator.getSeperatorChar(), CSVParser.DEFAULT_QUOTE_CHARACTER,
					CSVParser.DEFAULT_ESCAPE_CHARACTER, 0, false, false, true);
		} else {
			return new CSVReader(new BufferedReader(new InputStreamReader(is, importConfig.charset), BUFFER_SIZE),
					importConfig.separator.getSeperatorChar(), importConfig.quoteChar.getQuoteChar());
		}

	}

	public static CSVWriter createCSVWriter(OutputStream os, CSVImportConfig importConfig)
			throws UnsupportedEncodingException {
		return new CSVWriter(new BufferedWriter(new OutputStreamWriter(os, importConfig.charset), BUFFER_SIZE),
				importConfig.separator.getSeperatorChar(), importConfig.quoteChar.getQuoteChar());
	}

	public static InputStream getCSVInputStream(CSVFile csv) throws IOException {
		String ext = Files.getFileExtension(csv.getFile().toFile().getName());
		if (ext.equalsIgnoreCase("csv") || ext.equalsIgnoreCase("txt")) {
			return new FileInputStream(csv.getFile().toFile());
		} else if (ext.equalsIgnoreCase("zip")) {
			ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(csv.getFile().toFile()));
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			if (nextEntry == null) {
				throw new IOException("ZIP files does not contain any files");
			}
			return zipInputStream;
		} else if (ext.equalsIgnoreCase("gz")) {
			return new GZIPInputStream(new FileInputStream(csv.getFile().toFile()));
		}
		throw new UnsupportedOperationException("Unsupported file type " + ext);
	}

}