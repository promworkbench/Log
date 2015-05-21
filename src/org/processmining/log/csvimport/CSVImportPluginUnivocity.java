package org.processmining.log.csvimport;

import java.io.InputStream;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.CSVFileReferenceUnivocity;

/**
 * Converts a {@link CSVFile} to {@link XLog}.
 * 
 * @author N. Tax
 *
 */
@Plugin(name = "Import a CSV file and convert it to XES (Univocity)", parameterLabels = { "Filename" }, returnLabels = { "Imported CSV File" }, returnTypes = { CSVFile.class })
@UIImportPlugin(description = "CSV File (XES Conversion with Log package) (Univocity)", extensions = { "csv", "zip", "csv.gz", "txt" })
public final class CSVImportPluginUnivocity extends AbstractImportPlugin {

	@Override
	protected CSVFile importFromStream(final PluginContext context, final InputStream input, final String filename,
			final long fileSizeInBytes) throws Exception {
		context.getFutureResult(0).setLabel("Imported CSV: "+filename);
		return new CSVFileReferenceUnivocity(getFile().toPath(), filename, fileSizeInBytes);
	}

}
