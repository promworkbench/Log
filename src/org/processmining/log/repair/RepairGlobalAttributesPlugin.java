package org.processmining.log.repair;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import org.deckfour.xes.classification.XEventAttributeClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XCostExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.util.XAttributeUtils;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public final class RepairGlobalAttributesPlugin {

	private static final Function<XAttribute, XAttribute> PROTOTYPE_TRANSFORMER = new Function<XAttribute, XAttribute>() {

		public XAttribute apply(XAttribute firstAttr) {
			return XAttributeUtils.derivePrototype(firstAttr);
		}
	};

	public interface GlobalInfo {
		Collection<XAttribute> getEventAttributes();

		Collection<XAttribute> getTraceAttributes();
	}

	@Plugin(name = "Repair Global Attributes (In Place)", parameterLabels = { "Event Log" }, returnLabels = {}, returnTypes = {}, userAccessible = true, mostSignificantResult = -1, categories = { PluginCategory.Enhancement }, //
	help = "Repairs the Event Log by detecting which attributes are global and updating the information about global attributes.")
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "F. Mannhardt", email = "f.mannhardt@tue.nl")
	public void repairLogInPlace(PluginContext context, XLog log) {

		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(log.size());
		doRepairLog(log);
	}

	@Plugin(name = "Repair Global Attributes", parameterLabels = { "Event Log" }, returnLabels = { "Repaired Log with Globals" }, returnTypes = { XLog.class }, userAccessible = true, mostSignificantResult = 1, categories = { PluginCategory.Enhancement }, //
	help = "Repairs the Event Log by detecting which attributes are global and updating the information about global attributes.")
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "F. Mannhardt", email = "f.mannhardt@tue.nl")
	public XLog repairLog(PluginContext context, XLog log) {

		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(log.size());

		XLog newLog = (XLog) log.clone();

		doRepairLog(newLog);

		return newLog;
	}

	public static void doRepairLog(XLog log) {
		GlobalInfo info = detectGlobals(log);

		for (XAttribute attr : info.getEventAttributes()) {
			if (isClassifierAttribute(attr)) {
				log.getClassifiers().add(new XEventAttributeClassifier(attr.getKey(), attr.getKey()));
			}
			log.getGlobalEventAttributes().add(attr);
		}

		log.getGlobalTraceAttributes().addAll(info.getTraceAttributes());
	}

	private static boolean isClassifierAttribute(XAttribute attribute) {
		switch (attribute.getKey()) {
			case XConceptExtension.KEY_INSTANCE :
			case XTimeExtension.KEY_TIMESTAMP :
			case XLifecycleExtension.KEY_MODEL :
			case XLifecycleExtension.KEY_TRANSITION :
			case XCostExtension.KEY_AMOUNT :
			case XCostExtension.KEY_CURRENCY :
			case XCostExtension.KEY_DRIVER :
			case XCostExtension.KEY_TOTAL :
			case XCostExtension.KEY_TYPE :
				return false;
		}
		return true;
	}

	public static GlobalInfo detectGlobals(XLog log) {

		Set<XAttribute> eventAttributes = new HashSet<>();
		Set<XAttribute> traceAttributes = new HashSet<>();

		for (ListIterator<XTrace> logIter = log.listIterator(); logIter.hasNext();) {
			int traceIndex = logIter.nextIndex();
			XTrace trace = logIter.next();
			if (traceIndex == 0) {
				traceAttributes.addAll(trace.getAttributes().values());
			} else {
				Iterator<XAttribute> it = traceAttributes.iterator();
				while (it.hasNext()) {
					if (!trace.getAttributes().containsKey(it.next().getKey())) {
						it.remove();
					}
				}
			}
			for (ListIterator<XEvent> eventIter = trace.listIterator(); eventIter.hasNext();) {
				int eventIndex = eventIter.nextIndex();
				XEvent event = eventIter.next();
				if (traceIndex == 0 && eventIndex == 0) {
					eventAttributes.addAll(event.getAttributes().values());
				} else {
					Iterator<XAttribute> it = eventAttributes.iterator();
					while (it.hasNext()) {
						if (!event.getAttributes().containsKey(it.next().getKey())) {
							it.remove();
						}
					}
				}
			}
		}

		final Collection<XAttribute> defaultEventAttributes = Collections2.transform(eventAttributes,
				PROTOTYPE_TRANSFORMER);
		final Collection<XAttribute> defaultTraceAttributes = Collections2.transform(traceAttributes,
				PROTOTYPE_TRANSFORMER);

		return new GlobalInfo() {

			public Collection<XAttribute> getEventAttributes() {
				return defaultEventAttributes;
			}

			public Collection<XAttribute> getTraceAttributes() {
				return defaultTraceAttributes;
			}

		};
	}

}
