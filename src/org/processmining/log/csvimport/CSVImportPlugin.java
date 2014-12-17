package org.processmining.log.csvimport;

import java.io.InputStream;

import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

/**
 * @author F. Mannhardt
 *
 */
@Plugin(name = "Import a CSV file and convert it to XES", parameterLabels = { "Filename" }, returnLabels = { "Imported CSV File" }, returnTypes = { CSVFile.class })
@UIImportPlugin(description = "CSV File (XES Conversion with Log package)", extensions = { "csv", "zip", "csv.gz", "txt" })
public final class CSVImportPlugin extends AbstractImportPlugin {

	@Override
	protected CSVFile importFromStream(final PluginContext context, final InputStream input, final String filename,
			final long fileSizeInBytes) throws Exception {
		context.getFutureResult(0).setLabel("Imported CSV: "+filename);
		return new CSVFileReference(getFile().toPath(), filename, fileSizeInBytes);
	}

}
