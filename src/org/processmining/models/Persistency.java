package org.processmining.models;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.xstream.XAttributeConverter;
import org.deckfour.xes.xstream.XAttributeMapConverter;
import org.deckfour.xes.xstream.XEventConverter;
import org.deckfour.xes.xstream.XExtensionConverter;
import org.deckfour.xes.xstream.XLogConverter;
import org.deckfour.xes.xstream.XTraceConverter;
import org.processmining.framework.xstream.XStreamConverter;
import org.processmining.framework.xstream.XStreamPersistency;

public class Persistency {
	static {
		XStreamPersistency.addConverter(new XStreamConverter(new XAttributeConverter(), "XAttribute", XAttribute.class));
		XStreamPersistency.addConverter(new XStreamConverter(new XAttributeMapConverter(), "XAttributeMap", XAttributeMap.class));
		XStreamPersistency.addConverter(new XStreamConverter(new XEventConverter(), "XEvente", XEvent.class));
		XStreamPersistency.addConverter(new XStreamConverter(new XTraceConverter(), "XTrace", XTrace.class));
		XStreamPersistency.addConverter(new XStreamConverter(new XLogConverter(), "XLog", XLog.class));
		XStreamPersistency.addConverter(new XStreamConverter(new XExtensionConverter(), "XExtension", XExtension.class));
	}
}
