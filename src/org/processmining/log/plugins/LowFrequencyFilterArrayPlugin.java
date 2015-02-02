package org.processmining.log.plugins;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.algorithms.LowFrequencyFilterAlgorithm;
import org.processmining.log.dialogs.LowFrequencyFilterDialog;
import org.processmining.log.models.EventLogArray;
import org.processmining.log.models.impl.EventLogArrayFactory;
import org.processmining.log.parameters.LowFrequencyFilterParameters;

@Plugin(name = "Filter Out Low-Frequency Traces (Multiple Logs)", parameterLabels = { "Event Logs" }, returnLabels = { "Filtered Logs" }, returnTypes = { EventLogArray.class }, userAccessible = true, help = "Log Filtering Plug-in")
public class LowFrequencyFilterArrayPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Filter Out Low-Frequency Traces (Multiple Logs), UI", requiredParameterLabels = { 0 })
	public EventLogArray publicUIArray(UIPluginContext context, EventLogArray logs) {
		if (logs.getSize() > 0) {
			XLog log = logs.getLog(0);
			LowFrequencyFilterParameters parameters = new LowFrequencyFilterParameters(log);
			LowFrequencyFilterDialog dialog = new LowFrequencyFilterDialog(log, parameters);
			InteractionResult result = context.showWizard("Configure Low-Frequency Filter", true, true, dialog);
			if (result != InteractionResult.FINISHED) {
				return null;
			}
			EventLogArray filteredLogs = EventLogArrayFactory.createEventLogArray();
			filteredLogs.init();
			for (int i = 0; i < logs.getSize(); i++) {
				filteredLogs.addLog((new LowFrequencyFilterAlgorithm()).apply(context, logs.getLog(i), parameters));
			}
			return filteredLogs;
		}
		return null;
	}

	@PluginVariant(variantLabel = "Filter Out Low-Frequency Traces (Multiple Logs), Parameters", requiredParameterLabels = { 0 })
	public EventLogArray publicParameters(PluginContext context, EventLogArray logs,
			LowFrequencyFilterParameters parameters) {
		if (logs.getSize() > 0) {
			EventLogArray filteredLogs = EventLogArrayFactory.createEventLogArray();
			filteredLogs.init();
			for (int i = 0; i < logs.getSize(); i++) {
				filteredLogs.addLog((new LowFrequencyFilterAlgorithm()).apply(context, logs.getLog(i), parameters));
			}
			return filteredLogs;
		}
		return null;
	}

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Filter Out Low-Frequency Traces (Multiple Logs), Default", requiredParameterLabels = { 0 })
	public EventLogArray publicDefault(PluginContext context, EventLogArray logs) {
		if (logs.getSize() > 0) {
			LowFrequencyFilterParameters parameters = new LowFrequencyFilterParameters(logs.getLog(0));
			EventLogArray filteredLogs = EventLogArrayFactory.createEventLogArray();
			filteredLogs.init();
			for (int i = 0; i < logs.getSize(); i++) {
				filteredLogs.addLog((new LowFrequencyFilterAlgorithm()).apply(context, logs.getLog(i), parameters));
			}
			return filteredLogs;
		}
		return null;
	}
}
