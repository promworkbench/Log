package org.processmining.plugins.log.exporting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XesXmlGZIPSerializer;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Export Log to compressed XES File", parameterLabels = { "Log",
		"File" }, returnLabels = {}, returnTypes = {}, userAccessible = true)
@UIExportPlugin(description = "Compressed XES files", extension = "xes.gz")
public class ExportLogXesGz {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(requiredParameterLabels = { 0, 1 }, variantLabel = "Export Log to compressed XES File")
	public void export(UIPluginContext context, XLog log, File file) throws IOException {
		export(log, file);
	}

	public static void export(XLog log, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		XSerializer logSerializer = new XesXmlGZIPSerializer();
		logSerializer.serialize(log, out);
		out.close();
	}
}
