package org.processmining.log.csv;

import java.io.IOException;
import java.nio.file.Path;

import org.processmining.log.csvimport.CSVUtils;
import org.processmining.log.csvimport.config.CSVImportConfig;

import au.com.bytecode.opencsv.CSVReader;

/**
 * {@link CSVFile} implementation that holds a reference to a CSV file on disk.
 *
 * @author F. Mannhardt
 *
 */
public final class CSVFileReference implements CSVFile {

	private final Path file;
	private final String filename;
	private final long fileSizeInBytes;

	public CSVFileReference(Path file, String filename, long fileSizeInBytes) {
		this.file = file;
		this.filename = filename;
		this.fileSizeInBytes = fileSizeInBytes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.log.csvimport.ICSVFile#getFile()
	 */
	@Override
	public Path getFile() {
		return file;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.log.csvimport.ICSVFile#getFilename()
	 */
	@Override
	public String getFilename() {
		return filename;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.log.csvimport.ICSVFile#getFileSizeInBytes()
	 */
	@Override
	public long getFileSizeInBytes() {
		return fileSizeInBytes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.log.csv.CSVFile#readHeader(org.processmining.log.csvimport
	 * .CSVImportConfig)
	 */
	@Override
	public String[] readHeader(CSVImportConfig importConfig) throws IOException {
		try (CSVReader reader = CSVUtils.createCSVReader(CSVUtils.getCSVInputStream(this), importConfig)) {
			String[] header = reader.readNext();
			return header;
		} catch (IOException e) {
			throw new IOException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.log.csv.CSVFile#createReader(org.processmining.log.
	 * csvimport.CSVImportConfig)
	 */
	@Override
	public CSVReader createReader(CSVImportConfig importConfig) throws IOException {
		return CSVUtils.createCSVReader(CSVUtils.getCSVInputStream(this), importConfig);
	}

}
