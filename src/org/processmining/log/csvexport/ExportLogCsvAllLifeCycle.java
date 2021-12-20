package org.processmining.log.csvexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XSerializer;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Export Log to CSV File (all life-cycle events)", level= PluginLevel.PeerReviewed, parameterLabels = { "Log", "File" }, returnLabels = {}, returnTypes = {}, userAccessible = true)
@UIExportPlugin(description = "CSV files (all life-cycle events)", extension = "csv")
public final class ExportLogCsvAllLifeCycle {
	
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "F. Mannhardt, M. de Leoni, D. Fahland", email = "d.fahland@tue.nl")
	@PluginVariant(requiredParameterLabels = { 0, 1 }, variantLabel = "Export Log to CSV File (all life-cycle events)")
	public void export(UIPluginContext context, XLog log, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);

		XSerializer logSerializer = new XesCsvSerializerAllLifeCycles("yyyy/MM/dd HH:mm:ss.SSS");
		logSerializer.serialize(log, out);
		out.close();	
	}
}
