package org.processmining.log.plugins;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.algorithms.LogFrequencyAlgorithm;
import org.processmining.log.dialogs.LogFrequencyDialog;
import org.processmining.log.models.LogFrequency;
import org.processmining.log.parameters.LogFrequencyParameters;

@Plugin(name = "Create Frequency Distribution", parameterLabels = { "Event Log" }, returnLabels = { "Log Frequency Distribution" }, returnTypes = { LogFrequency.class }, userAccessible = true, help = "Log Frequency Distribution Plug-in")
public class LogFrequencyPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Create Frequency Distribution, UI", requiredParameterLabels = { 0 })
	public LogFrequency publicUI(UIPluginContext context, XLog log) {
		LogFrequencyParameters parameters = new LogFrequencyParameters(log);
		LogFrequencyDialog dialog = new LogFrequencyDialog(log, parameters);
		InteractionResult result = context.showWizard("Configure Frequency Distribution (classifier)", true, true,
				dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		return (new LogFrequencyAlgorithm()).apply(context, log, parameters);
	}

	@PluginVariant(variantLabel = "Create Frequency Distribution, Parameters", requiredParameterLabels = { 0 })
	public LogFrequency publicParameters(PluginContext context, XLog log, LogFrequencyParameters parameters) {
		return (new LogFrequencyAlgorithm()).apply(context, log, parameters);
	}

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Create Frequency Distribution, Default", requiredParameterLabels = { 0 })
	public LogFrequency publicDefault(PluginContext context, XLog log) {
		LogFrequencyParameters parameters = new LogFrequencyParameters(log);
		return (new LogFrequencyAlgorithm()).apply(context, log, parameters);
	}
}
