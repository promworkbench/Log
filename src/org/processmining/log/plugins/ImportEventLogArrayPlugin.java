package org.processmining.log.plugins;

import java.io.File;
import java.io.InputStream;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.log.models.EventLogArray;
import org.processmining.log.models.impl.EventLogArrayFactory;

@Plugin(name = "Import Event Log Array from ELA file", parameterLabels = { "Filename" }, returnLabels = { "Event Log Array" }, returnTypes = { EventLogArray.class })
@UIImportPlugin(description = "ELA Event Log Array files", extensions = { "ela" })
public class ImportEventLogArrayPlugin extends AbstractImportPlugin {

	protected FileFilter getFileFilter() {
		return new FileNameExtensionFilter("ELA files", "ela");
	}

	protected Object importFromStream(PluginContext context, InputStream input, String filename, long fileSizeInBytes)
			throws Exception {
		EventLogArray logs = EventLogArrayFactory.createEventLogArray();
		File file = getFile();
		String parent = (file == null ? null : file.getParent());
		logs.importFromStream(context, input, parent);
		return logs;
	}
}
