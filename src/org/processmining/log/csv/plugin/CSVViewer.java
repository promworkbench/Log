package org.processmining.log.csv.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.config.CSVConfig;

public class CSVViewer {

	@Plugin(name = "View CSV", level = PluginLevel.Regular, //
	parameterLabels = { "CSV" }, returnLabels = { "XES Event Log" }, // 
	returnTypes = { JComponent.class }, userAccessible = true)
	@Visualizer
	public JComponent viewCSV(final UIPluginContext context, final CSVFile csvFile) throws FileNotFoundException,
			IOException {
		return new CSVViewerPanel(csvFile, new CSVConfig(csvFile));
	}

}
