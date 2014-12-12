package org.processmining.log.csvimport;

import java.nio.charset.Charset;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;

/**
 * Configuration for the import of the CSV
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVImportConfig {
	public XFactory factory = XFactoryRegistry.instance().currentDefault();
	public String charset = Charset.defaultCharset().name();
	public SeperatorChar separator = SeperatorChar.COMMA;
	public char quoteChar = '"';
}