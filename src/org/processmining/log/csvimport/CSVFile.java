package org.processmining.log.csvimport;

import java.nio.file.Path;

public interface CSVFile {

	Path getFile();

	String getFilename();

	long getFileSizeInBytes();

	Path getDirectory();

}