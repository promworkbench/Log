package org.processmining.log.plugins;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.algorithms.LogFrequencyArrayAlgorithm;
import org.processmining.log.dialogs.LogFrequencyDialog;
import org.processmining.log.models.EventLogArray;
import org.processmining.log.models.LogFrequencyArray;
import org.processmining.log.parameters.LogFrequencyParameters;

@Plugin(name = "Create Frequency Distributions", parameterLabels = { "Event Logs" }, returnLabels = { "Log Frequency Distribution" }, returnTypes = { LogFrequencyArray.class }, userAccessible = true, help = "Log Frequency Distribution Plug-in")
public class LogFrequencyArrayPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Create Frequency Distributions, UI", requiredParameterLabels = { 0 })
	public LogFrequencyArray publicUI(UIPluginContext context, EventLogArray logs) {
		LogFrequencyParameters parameters = new LogFrequencyParameters(logs.getLog(0));
		LogFrequencyDialog dialog = new LogFrequencyDialog(logs.getLog(0), parameters);
		InteractionResult result = context.showWizard("Configure Frequency Distributions (classifier)", true, true,
				dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		return (new LogFrequencyArrayAlgorithm()).apply(context, logs, parameters);
	}

	@PluginVariant(variantLabel = "Create Frequency Distributions, Parameters", requiredParameterLabels = { 0 })
	public LogFrequencyArray publicParameters(PluginContext context, EventLogArray logs, LogFrequencyParameters parameters) {
		return (new LogFrequencyArrayAlgorithm()).apply(context, logs, parameters);
	}

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Eric Verbeek", email = "h.m.w.verbeek@tue.nl", website = "www.processmining.org")
	@PluginVariant(variantLabel = "Create Frequency Distributions, Default", requiredParameterLabels = { 0 })
	public LogFrequencyArray publicDefault(PluginContext context, EventLogArray logs) {
		LogFrequencyParameters parameters = new LogFrequencyParameters(logs.getLog(0));
		return (new LogFrequencyArrayAlgorithm()).apply(context, logs, parameters);
	}
}
