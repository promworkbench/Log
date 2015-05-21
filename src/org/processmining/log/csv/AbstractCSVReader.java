package org.processmining.log.csv;

import java.io.IOException;

public interface AbstractCSVReader extends AutoCloseable {

	String[] readNext() throws IOException;

	void close() throws IOException;

}
