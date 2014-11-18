package org.processmining.plugins.log.logfilters.impl;

import org.deckfour.xes.extension.std.XClassExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.log.logfilters.LogFilter;
import org.processmining.plugins.log.logfilters.LogFilterException;
import org.processmining.plugins.log.logfilters.XTraceEditor;

@Plugin(name = "Add Artifical Start and End Events", parameterLabels = { "Log", "Parameters" }, returnLabels = { "Log (start and end events added)" }, returnTypes = { XLog.class })
public class AddArtificialStartEndFilter {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(requiredParameterLabels = { 0 }, variantLabel = "Default parameters")
	public XLog filterDefault(PluginContext context, XLog log) throws LogFilterException {
		return filterParameters(context, log, new AddArtificialStartEndParameters());
	}

	@PluginVariant(requiredParameterLabels = { 0, 1 }, variantLabel = "Provided parameters")
	public XLog filterParameters(PluginContext context, final XLog log, final AddArtificialStartEndParameters parameters)
			throws LogFilterException {
		/**
		 * Add for every trace a start and end event. Classes for these events
		 * will be set using the classifier extension. All classifiers in the
		 * log will be wrapped in a priority classifier, where the first
		 * classifier is the classifier classifier, and the second is the
		 * original classifier. As a result, the start and end events will be
		 * classified using the classifier classifier, and the other events
		 * using the original classifier (assuming that they do not contain the
		 * classifier:class attribute).
		 */

		if (parameters.isCheckClassExtension() && log.getExtensions().contains(XClassExtension.instance())) {
			/*
			 * Class extension already present in log. Return log.
			 */
			return log;
		}
		
		final XFactory factory = XFactoryRegistry.instance().currentDefault();

		/*
		 * Add a new start and end event in every trace. The start and end event
		 * use the Classify Extension to store their class names.
		 */
		XLog filteredLog = LogFilter.filter(context.getProgress(), 100, log, XLogInfoFactory.createLogInfo(log),
				new XTraceEditor() {

					public XTrace editTrace(XTrace trace) {
						if (parameters.isAddStartEvent()) {
							XEvent startEvent = factory.createEvent();
							/*
							 * Set the class name for this event.
							 */
							XClassExtension.instance().assignName(startEvent, parameters.getStartClassName());
							/*
							 * Copy in the global attributes.
							 */
							for (XAttribute attribute: log.getGlobalEventAttributes()) {
								startEvent.getAttributes().put(attribute.getKey(), attribute);
							}
							trace.add(0, startEvent);
						}
						if (parameters.isAddEndEvent()) {
							XEvent endEvent = factory.createEvent();
							XClassExtension.instance().assignName(endEvent, parameters.getEndClassName());
							for (XAttribute attribute: log.getGlobalEventAttributes()) {
								endEvent.getAttributes().put(attribute.getKey(), attribute);
							}
							trace.add(endEvent);
						}
						return trace;
					}
				});

		filteredLog.getExtensions().add(XClassExtension.instance());
		
		return filteredLog;
	}
}
