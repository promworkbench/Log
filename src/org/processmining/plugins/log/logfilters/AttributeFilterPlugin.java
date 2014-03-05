package org.processmining.plugins.log.logfilters;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Filter Log on Attribute Values", parameterLabels = { "Log", "Parameters" }, returnLabels = { "Log" }, returnTypes = { XLog.class })
public class AttributeFilterPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(variantLabel = "Filter Log on Attribute Values, UI", requiredParameterLabels = { 0 })
	public XLog filterDialog(UIPluginContext context, XLog log) {
		AttributeFilterParameters parameters = new AttributeFilterParameters(log);
		AttributeFilterDialog dialog = new AttributeFilterDialog(parameters);
		InteractionResult result = context.showWizard("Configure filter (values)", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		dialog.applyFilter();
		return filterPrivate(context, log, parameters);
	}
	
	@PluginVariant(variantLabel = "Filter Log on Attribute Values, Parameters", requiredParameterLabels = { 0 })
	public XLog filterParameters(PluginContext context, XLog log, AttributeFilterParameters parameters) {
		return filterPrivate(context, log, parameters);
	}
	
	private XLog filterPrivate(PluginContext context, XLog log, AttributeFilterParameters parameters) {
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XLog filteredLog = factory.createLog(log.getAttributes());
		filteredLog.getClassifiers().addAll(log.getClassifiers());
		filteredLog.getExtensions().addAll(log.getExtensions());
		filteredLog.getGlobalTraceAttributes().addAll(log.getGlobalTraceAttributes());
		filteredLog.getGlobalEventAttributes().addAll(log.getGlobalEventAttributes());
		for (XTrace trace : log) {
			XTrace filteredTrace = factory.createTrace(trace.getAttributes());
			for (XEvent event : trace) {
				boolean add = true;
				for (String key : event.getAttributes().keySet()) {
					String value = event.getAttributes().get(key).toString();
					if (!parameters.getFilter().get(key).contains(value)) {
						add = false;
						continue;
					}
				}
				if (add) {
					filteredTrace.add(event);
				}
			}
			filteredLog.add(filteredTrace);
		}
		return filteredLog;
	}
}
