package org.processmining.log.csv;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.processmining.log.csv.config.CSVConfig;

/**
 * @author F. Mannhardt
 * 
 */
public interface CSVFile {

	/**
	 * @return the complete path to the CSV file (including the file itself)
	 */
	Path getFile();

	/**
	 * @return the filename with extension
	 */
	String getFilename();

	/**
	 * @return input stream of this CSV file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	InputStream getInputStream() throws FileNotFoundException, IOException;

	/**
	 * Returns the first row of the CSV file.
	 * 
	 * @param config
	 * @return
	 * @throws IOException
	 */
	String[] readHeader(CSVConfig config) throws IOException;

	/**
	 * Returns a new {@link ICSVReader} that can be used to read through the
	 * input stream. The caller is responsible for calling
	 * {@link ICSVReader#close()} on the reader.
	 * 
	 * @param config
	 * @return
	 * @throws IOException
	 */
	ICSVReader createReader(CSVConfig config) throws IOException;

	/**
	 * @return the CSV reader/writer interface used for this {@link CSVFile}
	 */
	ICSV getCSV();

}