package org.processmining.plugins.log.exporting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XMxmlSerializer;
import org.deckfour.xes.out.XSerializer;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.utils.XUtils;

@Plugin(name = "Export Log to MXML File", level= PluginLevel.PeerReviewed, parameterLabels = { "Log",
		"File" }, returnLabels = {}, returnTypes = {}, userAccessible = true)
@UIExportPlugin(description = "MXML files", extension = "mxml")
public class ExportLogMxml {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(requiredParameterLabels = { 0, 1 }, variantLabel = "Export Log to XMXL File")
	public void export(UIPluginContext context, XLog log, File file) throws IOException {
		XUtils.renameLogWithProMLabel(context, log);
		export(log, file);
	}

	public static void export(XLog log, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		XSerializer logSerializer = new XMxmlSerializer();
		logSerializer.serialize(log, out);
		out.close();
	}
}
