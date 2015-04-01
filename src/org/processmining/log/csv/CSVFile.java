package org.processmining.log.csv;

import java.io.IOException;
import java.nio.file.Path;

import org.processmining.log.csvimport.config.CSVImportConfig;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @author F. Mannhardt
 *
 */
public interface CSVFile {

	/**
	 * @return the complete path to the CSV file
	 */
	Path getFile();

	/**
	 * @return the filename with extension
	 */
	String getFilename();

	/**
	 * @return the file size
	 */
	long getFileSizeInBytes();

	/**
	 * Returns the first row of the CSV file. 
	 * 
	 * @param config
	 * @return
	 * @throws IOException
	 */
	String[] readHeader(CSVImportConfig config) throws IOException;

	/**
	 * Returns a new {@link CSVReader} that can be used to read through the
	 * file. The caller is responsible for calling {@link CSVReader#close()} on
	 * the reader.
	 * 
	 * @param config 
	 * @return
	 * @throws IOException
	 */
	CSVReader createReader(CSVImportConfig config) throws IOException;

}