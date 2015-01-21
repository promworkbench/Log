package org.processmining.log.plugins;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.dialogs.LogCentralityDialog;
import org.processmining.log.models.LogCentrality;
import org.processmining.log.parameters.LogCentralityParameters;

@Plugin(name = "Create Happifiable Log", parameterLabels = { "Event Log", "Parameters" }, returnLabels = { "Happifiable Log" }, returnTypes = { LogCentrality.class })
public class LogCentralityPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(variantLabel = "Create Happifiable Log, UI", requiredParameterLabels = { 0 })
	public LogCentrality runDialog(UIPluginContext context, XLog log) {
		LogCentralityParameters parameters = new LogCentralityParameters(log);
		LogCentrality centrality = new LogCentrality(log);
		LogCentralityDialog dialog = new LogCentralityDialog(context, log, centrality, parameters);
		InteractionResult result = context.showWizard("Configure Creation of Happifiable Log", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		return runPrivate(context, log, centrality, parameters);
	}
	
	@PluginVariant(variantLabel = "Create Happifiable Log, Parameters", requiredParameterLabels = { 0, 1 })
	public LogCentrality runParameters(PluginContext context, XLog log, LogCentralityParameters parameters) {
		LogCentrality centrality = new LogCentrality(log);
		return runPrivate(context, log, centrality, parameters);
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(variantLabel = "Create Happifiable Log, Default", requiredParameterLabels = { 0 })
	public LogCentrality runDefault(PluginContext context, XLog log) {
		LogCentralityParameters parameters = new LogCentralityParameters(log);
		LogCentrality centrality = new LogCentrality(log);
		return runPrivate(context, log, centrality, parameters);
	}
	
	private LogCentrality runPrivate(PluginContext context, XLog log, LogCentrality centrality, LogCentralityParameters parameters) {
		context.getProgress().setMaximum(log.size());
		centrality.setClassifier(context, parameters);
		return centrality;
	}
}
