package org.processmining.log.csv;

import java.io.IOException;
import java.nio.file.Path;

import org.processmining.log.csv.config.CSVConfig;

/**
 * {@link CSVFile} implementation that holds a reference to a CSV file on disk.
 *
 * @author N. Tax
 *
 */
public final class CSVFileReferenceUnivocityImpl extends AbstractCSVFile {
	
	private final CSVUnivocityImpl csv;

	public CSVFileReferenceUnivocityImpl(Path file, String filename, long fileSizeInBytes) {
		super(file, filename, fileSizeInBytes);
		csv = new CSVUnivocityImpl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.log.csv.CSVFile#readHeader(org.processmining.log.csvimport
	 * .CSVImportConfig)
	 */
	@Override
	public String[] readHeader(CSVConfig importConfig) throws IOException {
		String[] s = createReader(importConfig).readNext();
		return s;
	}

	/* (non-Javadoc)
	 * @see org.processmining.log.csv.CSVFile#createReader(org.processmining.log.csv.CSVConfig)
	 */
	@Override
	public ICSVReader createReader(CSVConfig config) throws IOException {
		return csv.createReader(getInputStream(), config);
	}

	/* (non-Javadoc)
	 * @see org.processmining.log.csv.CSVFile#getCSV()
	 */
	@Override
	public ICSV getCSV() {
		return csv;
	}
	
}
