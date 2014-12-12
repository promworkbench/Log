package org.processmining.log.csvimport;

import java.io.IOException;
import java.nio.file.Path;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Class holding a reference to a CSV file on disk. 
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVFileReference implements CSVFile {
	
	private final Path file;
	private final String filename;
	private long fileSizeInBytes;
	private final Path path;

	public CSVFileReference(Path file, String filename, long fileSizeInBytes, Path directory) {
		this.file = file;
		this.filename = filename;
		this.fileSizeInBytes = fileSizeInBytes;
		this.path = directory;
	}

	/* (non-Javadoc)
	 * @see org.processmining.log.csvimport.ICSVFile#getFile()
	 */
	@Override
	public Path getFile() {
		return file;
	}

	/* (non-Javadoc)
	 * @see org.processmining.log.csvimport.ICSVFile#getFilename()
	 */
	@Override
	public String getFilename() {
		return filename;
	}

	/* (non-Javadoc)
	 * @see org.processmining.log.csvimport.ICSVFile#getFileSizeInBytes()
	 */
	@Override
	public long getFileSizeInBytes() {
		return fileSizeInBytes;
	}

	/* (non-Javadoc)
	 * @see org.processmining.log.csvimport.ICSVFile#getDirectory()
	 */
	@Override
	public Path getDirectory() {
		return path;
	}

	public String[] readHeader(CSVImportConfig importConfig) throws IOException {
		try (CSVReader reader = CSVUtils.createCSVReader(CSVUtils.getCSVInputStream(this), importConfig)) {
			String[] header = reader.readNext();
			return header;
		} catch (IOException e) {
			throw new IOException(e);
		}		
	}

	public CSVReader createReader(CSVImportConfig importConfig) throws IOException {
		return CSVUtils.createCSVReader(CSVUtils.getCSVInputStream(this), importConfig);
	}

}
