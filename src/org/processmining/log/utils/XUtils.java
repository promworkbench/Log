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

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
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

/**
 * Commonly used methods for handling XES logs
 * 
 * @author F. Mannhardt
 *
 */
public class XUtils {

	public static boolean containsEventWithName(String eventName, XTrace trace) {
		XConceptExtension instance = XConceptExtension.instance();
		for (XEvent xEvent : trace) {
			if (eventName.equals(instance.extractName(xEvent))) {
				return true;
			}
		}
		return false;
	}

	public static XEvent getLatestEventWithName(String eventName, XTrace trace) {
		XEvent latestEvent = null;
		XConceptExtension instance = XConceptExtension.instance();
		for (XEvent xEvent : trace) {
			if (eventName.equals(instance.extractName(xEvent))) {
				latestEvent = xEvent;
			}
		}
		return latestEvent;
	}

	public static NavigableSet<String> getAllEventNamesSorted(XLog log) {
		NavigableSet<String> eventNames = new TreeSet<String>();
		XConceptExtension extension = XConceptExtension.instance();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				eventNames.add(extension.extractName(event));
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

}
