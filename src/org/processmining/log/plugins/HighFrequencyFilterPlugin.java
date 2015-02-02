package org.processmining.log.plugins;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.algorithms.HighFrequencyFilterAlgorithm;
import org.processmining.log.dialogs.HighFrequencyFilterDialog;
import org.processmining.log.parameters.HighFrequencyFilterParameters;

@Plugin(name = "Filter In High-Frequency Traces (Single Log)", parameterLabels = {"Event Log"}, returnLabels = { "Filtered Log" }, returnTypes = {XLog.class }, userAccessible = true, help = "Log Filtering Plug-in")
public class HighFrequencyFilterPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Filter In High-Frequency Traces (Single Log), UI", requiredParameterLabels = { 0 })
	public XLog publicUI(UIPluginContext context, XLog log) {
		HighFrequencyFilterParameters parameters = new HighFrequencyFilterParameters(log);
		HighFrequencyFilterDialog dialog = new HighFrequencyFilterDialog(log, parameters);
		InteractionResult result = context.showWizard("Configure High-Frequency Filter", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		return (new HighFrequencyFilterAlgorithm()).apply(context, log, parameters);
	}
	
	@PluginVariant(variantLabel = "Filter In High-Frequency Traces (Single Log), Parameters", requiredParameterLabels = { 0 })
	public XLog publicParameters(PluginContext context, XLog log, HighFrequencyFilterParameters parameters) {
		return (new HighFrequencyFilterAlgorithm()).apply(context, log, parameters);
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Filter In High-Frequency Traces (Single Log), Default", requiredParameterLabels = { 0 })
	public XLog publicDefault(PluginContext context, XLog log) {
		HighFrequencyFilterParameters parameters = new HighFrequencyFilterParameters(log);
		return (new HighFrequencyFilterAlgorithm()).apply(context, log, parameters);
	}
	
}
