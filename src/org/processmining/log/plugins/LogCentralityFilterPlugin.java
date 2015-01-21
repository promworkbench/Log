package org.processmining.log.plugins;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.dialogs.LogCentralityFilterDialog;
import org.processmining.log.models.LogCentrality;
import org.processmining.log.parameters.LogCentralityFilterParameters;

@Plugin(name = "Happify Log", parameterLabels = { "Happifiable Log", "Parameters" }, returnLabels = { "Happified Log" }, returnTypes = { XLog.class })
public class LogCentralityFilterPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(variantLabel = "Happify Log, UI", requiredParameterLabels = { 0 })
	public XLog runDialog(UIPluginContext context, LogCentrality centrality) {
		LogCentralityFilterParameters parameters = new LogCentralityFilterParameters(centrality);
		LogCentralityFilterDialog dialog = new LogCentralityFilterDialog(context, centrality, parameters);
		InteractionResult result = context.showWizard("Configure Happification of Log", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		return runPrivate(context, centrality, parameters);
	}
	
	@PluginVariant(variantLabel = "Happify Log, Parameters", requiredParameterLabels = { 0, 1 })
	public XLog runParameters(PluginContext context, LogCentrality centrality, LogCentralityFilterParameters parameters) {
		return runPrivate(context, centrality, parameters);
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(variantLabel = "Happify Log, Default", requiredParameterLabels = { 0 })
	public XLog runDefault(PluginContext context, LogCentrality centrality) {
		LogCentralityFilterParameters parameters = new LogCentralityFilterParameters(centrality);
		return runPrivate(context, centrality, parameters);
	}
	
	private XLog runPrivate(PluginContext context, LogCentrality centrality, LogCentralityFilterParameters parameters) {
		context.getProgress().setMaximum(centrality.size());
		return centrality.filter(context, parameters);
	}
}
