package org.processmining.log.csvimport;

import java.nio.file.Path;

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

}
