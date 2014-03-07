package org.processmining.plugins.log.logfilters;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Filter Log on Event Attribute Values", parameterLabels = { "Log", "Parameters" }, returnLabels = { "Log" }, returnTypes = { XLog.class })
public class AttributeFilterPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(variantLabel = "Filter Log on Event Attribute Values, UI", requiredParameterLabels = { 0 })
	public XLog filterDialog(UIPluginContext context, XLog log) {
		context.getProgress().setMaximum(3 * log.size());
		AttributeFilterParameters parameters = new AttributeFilterParameters(context, log);
		AttributeFilterDialog dialog = new AttributeFilterDialog(context, parameters, " (filtered on event attributes)");
		InteractionResult result = context.showWizard("Configure filter (values)", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		dialog.applyFilter();
		return filterPrivate(context, log, parameters);
	}
	
	@PluginVariant(variantLabel = "Filter Log on Event Attribute Values, Parameters", requiredParameterLabels = { 0 })
	public XLog filterParameters(PluginContext context, XLog log, AttributeFilterParameters parameters) {
		return filterPrivate(context, log, parameters);
	}
	
	private XLog filterPrivate(PluginContext context, XLog log, AttributeFilterParameters parameters) {
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XLog filteredLog = factory.createLog((XAttributeMap) log.getAttributes().clone());
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
				context.getProgress().inc();
			}
			filteredLog.add(filteredTrace);
		}
		XConceptExtension.instance().assignName(filteredLog, parameters.getName());
		context.getFutureResult(0).setLabel(parameters.getName());
		return filteredLog;
	}
}
