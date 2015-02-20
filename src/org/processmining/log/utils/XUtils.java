/*
 * Copyright (c) 2014 F. Mannhardt (f.mannhardt@tue.nl)
 * 
 * LICENSE:
 * 
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.processmining.log.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContainer;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeID;
import org.deckfour.xes.model.XAttributeList;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XesXmlSerializer;

/**
 * Commonly used methods for handling XES logs
 * 
 * @author F. Mannhardt
 *
 */
public class XUtils {
	
	public static final XEventClassifier STANDARDCLASSIFIER = new XEventAndClassifier(new XEventNameClassifier(), new XEventLifeTransClassifier());
	
	/**
	 * Added by Eric Verbeek
	 * 
	 * Returns a default classifier to use with an event log.
	 * If the log contains classifiers, then the first classifier is returned.
	 * Otherwise, the standard MXML classifier is constructed and returned.
	 * 
	 * @param log
	 * @return A default classifier to use with the provided log.
	 */
	public static XEventClassifier getDefaultClassifier(XLog log) {
		if (log.getClassifiers().isEmpty()) {
			return STANDARDCLASSIFIER;
		}
		return log.getClassifiers().get(0);
	}
	
	public static String getConceptName(XAttributable element) {
		return XConceptExtension.instance().extractName(element);
	}
	
	public static Date getTimestamp(XEvent event) {
		return XTimeExtension.instance().extractTimestamp(event);
	}
	
	public static void saveLog(XLog log, File file) throws FileNotFoundException, IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			XSerializer logSerializer = new XesXmlSerializer();
			logSerializer.serialize(log, out);
		}
	}

	public static boolean containsEventWithName(String eventName, XTrace trace) {
		for (XEvent xEvent : trace) {
			if (eventName.equals(getConceptName(xEvent))) {
				return true;
			}
		}
		return false;
	}

	public static XEvent getLatestEventWithName(String eventName, XTrace trace) {
		XEvent latestEvent = null;
		for (XEvent xEvent : trace) {
			if (eventName.equals(getConceptName(xEvent))) {
				latestEvent = xEvent;
			}
		}
		return latestEvent;
	}

	public static NavigableSet<String> getAllEventNamesSorted(XLog log) {
		NavigableSet<String> eventNames = new TreeSet<>();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				eventNames.add(getConceptName(event));
			}
		}
		return eventNames;
	}

	public static String stringifyEvent(XEvent e, XEventClassifier classifier) {
		return classifier.getClassIdentity(e);
	}

	public static String stringifyEvent(XEvent e) {
		return stringifyEvent(e, new XEventNameClassifier());
	}

	public static String stringifyTrace(XTrace t, XEventClassifier classifier) {
		StringBuilder sBuilder = new StringBuilder("[");
		Iterator<XEvent> iterator = t.iterator();
		while (iterator.hasNext()) {
			sBuilder.append(stringifyEvent(iterator.next(), classifier));
			if (iterator.hasNext()) {
				sBuilder.append(",");
			}
		}
		sBuilder.append("]");
		return sBuilder.toString();
	}

	public static String stringifyTrace(XTrace t) {
		return stringifyTrace(t, new XEventNameClassifier());
	}

	public static String stringifyLog(XLog l, XEventClassifier classifier) {
		StringBuilder sBuilder = new StringBuilder("[");
		Iterator<XTrace> iterator = l.iterator();
		while (iterator.hasNext()) {
			sBuilder.append(stringifyTrace(iterator.next(), classifier));
			if (iterator.hasNext()) {
				sBuilder.append(",\n");
			}
		}
		sBuilder.append("]");
		return sBuilder.toString();
	}

	public static String stringifyLog(XLog l) {
		return stringifyLog(l, new XEventNameClassifier());
	}

	public static String stringifyAttributes(XAttributeMap map) {
		StringBuilder sBuilder = new StringBuilder("{");
		Iterator<XAttribute> iterator = map.values().iterator();
		while (iterator.hasNext()) {
			XAttribute a = iterator.next();
			sBuilder.append(a.getKey() + " -> " + a.toString());
			if (iterator.hasNext()) {
				sBuilder.append(",\n");
			}
		}
		sBuilder.append("}");
		return sBuilder.toString();
	}

	public static XAttribute cloneAttributeWithChangedKey(XAttribute oldAttribute, String key) {
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		if (oldAttribute instanceof XAttributeList) {
			XAttributeList newAttribute = factory.createAttributeList(key, oldAttribute.getExtension());
			for (XAttribute a : ((XAttributeList) oldAttribute).getCollection()) {
				newAttribute.addToCollection(a);
			}
			return newAttribute;
		} else if (oldAttribute instanceof XAttributeContainer) {
			XAttributeContainer newAttribute = factory.createAttributeContainer(key, oldAttribute.getExtension());
			for (XAttribute a : ((XAttributeContainer) oldAttribute).getCollection()) {
				newAttribute.addToCollection(a);
			}
			return newAttribute;
		} else if (oldAttribute instanceof XAttributeLiteral) {
			return factory.createAttributeLiteral(key, ((XAttributeLiteral) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeBoolean) {
			return factory.createAttributeBoolean(key, ((XAttributeBoolean) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeContinuous) {
			return factory.createAttributeContinuous(key, ((XAttributeContinuous) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeDiscrete) {
			return factory.createAttributeDiscrete(key, ((XAttributeDiscrete) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeTimestamp) {
			return factory.createAttributeTimestamp(key, ((XAttributeTimestamp) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeID) {
			return factory
					.createAttributeID(key, ((XAttributeID) oldAttribute).getValue(), oldAttribute.getExtension());
		} else {
			throw new IllegalArgumentException("Unexpected attribute type!");
		}
	}
	

	public static Object getAttributeValue(XAttribute attribute) {
		if (attribute instanceof XAttributeList) {
			return ((XAttributeList)attribute).getCollection();
		} else if (attribute instanceof XAttributeContainer) {
			return ((XAttributeContainer)attribute).getCollection();
		} else if (attribute instanceof XAttributeLiteral) {
			return ((XAttributeLiteral)attribute).getValue();
		} else if (attribute instanceof XAttributeBoolean) {
			return ((XAttributeBoolean)attribute).getValue();
		} else if (attribute instanceof XAttributeContinuous) {
			return ((XAttributeContinuous)attribute).getValue();
		} else if (attribute instanceof XAttributeDiscrete) {
			return ((XAttributeDiscrete)attribute).getValue();
		} else if (attribute instanceof XAttributeTimestamp) {
			return ((XAttributeTimestamp)attribute).getValue();
		} else if (attribute instanceof XAttributeID) {
			return ((XAttributeID)attribute).getValue();
		} else {
			throw new IllegalArgumentException("Unexpected attribute type!");
		}
	}

}