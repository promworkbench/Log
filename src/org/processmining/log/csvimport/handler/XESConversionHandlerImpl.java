package org.processmining.log.csvimport.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.log.csvimport.CSVConversionConfig;
import org.processmining.log.csvimport.CSVFile;
import org.processmining.log.csvimport.CSVImportConfig;

/**
 * Handler to create an XLog
 * 
 * @author F. Mannhardt
 *
 */
public final class XESConversionHandlerImpl implements CSVConversionHandler<XLog> {

	private final XFactory factory;

	private XLog log = null;
	
	private XTrace currentTrace = null;
	private List<XEvent> currentEvents = new ArrayList<>();
	private boolean hasStartEvents = false;
	
	private XEvent currentEvent = null;
	private int instanceCounter = 0;
	private XEvent currentStartEvent;

	public XESConversionHandlerImpl(CSVImportConfig importConfig, CSVConversionConfig conversionConfig) {
		this.factory = importConfig.factory;
	}

	public void startLog(CSVFile inputFile) {
		log = factory.createLog();
		assignName(factory, log, inputFile.getFilename());
	}

	public void startTrace(String caseId) {
		currentEvents.clear();
		hasStartEvents = false;
		currentTrace = factory.createTrace();
		assignName(factory, currentTrace, caseId);
	}

	public void endTrace(String caseId) {
		if (hasStartEvents) {
			Collections.sort(currentEvents, new Comparator<XEvent>() {

				public int compare(XEvent o1, XEvent o2) {					
					return XTimeExtension.instance().extractTimestamp(o1).compareTo(XTimeExtension.instance().extractTimestamp(o2));
				}
			});
		}
		currentTrace.addAll(currentEvents);		
		log.add(currentTrace);
	}

	public void startEvent(String eventClass, Date completionTime, Date startTime) {
		if (startTime != null) {			
			String instance = String.valueOf((instanceCounter++));
			hasStartEvents = true;
			
			currentStartEvent = factory.createEvent();
			assignName(factory, currentStartEvent, eventClass);
			assignTimestamp(factory, currentStartEvent, startTime);
			assignInstance(factory, currentStartEvent, instance);
			assignLifecycleTransition(factory, currentStartEvent, XLifecycleExtension.StandardModel.START);
			
			currentEvent = factory.createEvent();
			assignName(factory, currentEvent, eventClass);
			assignTimestamp(factory, currentEvent, completionTime);
			assignInstance(factory, currentEvent, instance);
			assignLifecycleTransition(factory, currentEvent, XLifecycleExtension.StandardModel.COMPLETE);
		} else {
			currentEvent = factory.createEvent();
			assignName(factory, currentEvent, eventClass);
			assignTimestamp(factory, currentEvent, completionTime);			
		}
	}

	public void startAttribute(String name, String value) {
		assignAttribute(currentEvent, factory.createAttributeLiteral(name, value, null));
	}

	public void startAttribute(String name, long value) {
		assignAttribute(currentEvent, factory.createAttributeDiscrete(name, value, null));
	}

	public void startAttribute(String name, double value) {
		assignAttribute(currentEvent, factory.createAttributeContinuous(name, value, null));
	}

	public void startAttribute(String name, Date value) {
		assignAttribute(currentEvent, factory.createAttributeTimestamp(name, value, null));
	}

	public void startAttribute(String name, boolean value) {
		assignAttribute(currentEvent, factory.createAttributeBoolean(name, value, null));
	}

	public void endAttribute() {
		//No-op
	}

	public void endEvent() {
		if (currentStartEvent != null) {
			currentEvents.add(currentStartEvent);
			currentStartEvent = null;
		}
		currentEvents.add(currentEvent);
		currentEvent = null;
	}

	public XLog getResult() {
		return log;
	}

	private static void assignAttribute(XAttributable a, XAttribute value) {
		a.getAttributes().put(value.getKey(), value);
	}
	
	private static void assignLifecycleTransition(XFactory factory, XAttributable a, StandardModel lifecycle) {
		assignAttribute(a,
				factory.createAttributeLiteral(XLifecycleExtension.KEY_TRANSITION, lifecycle.getEncoding(), XLifecycleExtension.instance()));
	}
	
	private static void assignInstance(XFactory factory, XAttributable a, String value) {
		assignAttribute(a,
				factory.createAttributeLiteral(XConceptExtension.KEY_INSTANCE, value, XConceptExtension.instance()));
	}

	private static void assignTimestamp(XFactory factory, XAttributable a, Date value) {
		assignAttribute(a,
				factory.createAttributeTimestamp(XTimeExtension.KEY_TIMESTAMP, value, XTimeExtension.instance()));
	}

	private static void assignName(XFactory factory, XAttributable a, String value) {
		assignAttribute(a,
				factory.createAttributeLiteral(XConceptExtension.KEY_NAME, value, XConceptExtension.instance()));
	}

}