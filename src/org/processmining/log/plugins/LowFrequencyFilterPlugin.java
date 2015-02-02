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
import org.processmining.log.parameters.LowFrequencyFilterParameters;

@Plugin(name = "Filter Out Low-Frequency Traces (Single Log)", parameterLabels = {"Event Log"}, returnLabels = { "Filtered Log" }, returnTypes = {XLog.class }, userAccessible = true, help = "Log Filtering Plug-in")
public class LowFrequencyFilterPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Filter Out Low-Frequency Traces (Single Log), UI", requiredParameterLabels = { 0 })
	public XLog publicUI(UIPluginContext context, XLog log) {
		LowFrequencyFilterParameters parameters = new LowFrequencyFilterParameters(log);
		LowFrequencyFilterDialog dialog = new LowFrequencyFilterDialog(log, parameters);
		InteractionResult result = context.showWizard("Configure Low-Frequency Filter", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		return (new LowFrequencyFilterAlgorithm()).apply(context, log, parameters);
	}
	
	@PluginVariant(variantLabel = "Filter Out Low-Frequency Traces (Single Log), Parameters", requiredParameterLabels = { 0 })
	public XLog publicParameters(PluginContext context, XLog log, LowFrequencyFilterParameters parameters) {
		return (new LowFrequencyFilterAlgorithm()).apply(context, log, parameters);
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Filter Out Low-Frequency Traces (Single Log), Default", requiredParameterLabels = { 0 })
	public XLog publicDefault(PluginContext context, XLog log) {
		LowFrequencyFilterParameters parameters = new LowFrequencyFilterParameters(log);
		return (new LowFrequencyFilterAlgorithm()).apply(context, log, parameters);
	}
}
