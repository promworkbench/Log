package org.processmining.log.csvimport.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.config.CSVConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVErrorHandlingMode;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVMapping;
import org.processmining.log.csvimport.config.CSVConversionConfig.ExtensionAttribute;
import org.processmining.log.csvimport.exception.CSVConversionException;
import org.processmining.log.utils.XUtils;

/**
 * Handler that creates an XLog from a CSV
 * 
 * @author F. Mannhardt
 *
 */
public final class XESConversionHandlerImpl implements CSVConversionHandler<XLog> {

	private static final int MAX_ERROR_LENGTH = 1 * 1024 * 1024;

	private final XFactory factory;
	private final CSVConversionConfig conversionConfig;
	private final StringBuilder conversionErrors;

	private XLog log = null;

	private XTrace currentTrace = null;
	private List<XEvent> currentEvents = new ArrayList<>();
	private boolean hasStartEvents = false;

	private XEvent currentEvent = null;
	private int instanceCounter = 0;
	private XEvent currentStartEvent;

	private boolean errorDetected = false;

	public XESConversionHandlerImpl(CSVConfig importConfig,
			CSVConversionConfig conversionConfig) {
		this.conversionConfig = conversionConfig;
		this.factory = conversionConfig.getFactory();
		this.conversionErrors = new StringBuilder();
	}
	
	@Override
	public String getConversionErrors() {
		if (conversionErrors.length() >= MAX_ERROR_LENGTH) {
			return conversionErrors.toString().concat("... (multiple error messages have been omitted to avoid running out of memory)");
		} else {
			return conversionErrors.toString();
		}
	}
	
	@Override
	public boolean hasConversionErrors() {
		return conversionErrors.length() != 0;
	}

	@Override
	public void startLog(CSVFile inputFile) {
		log = factory.createLog();
		if (conversionConfig.getEventNameColumns() != null) {
			log.getExtensions().add(XConceptExtension.instance());
			log.getClassifiers().add(XLogInfoImpl.NAME_CLASSIFIER);
		}
		if (conversionConfig.getCompletionTimeColumn() != null || conversionConfig.getStartTimeColumn() != null) {
			log.getExtensions().add(XTimeExtension.instance());
			log.getExtensions().add(XLifecycleExtension.instance());
			log.getClassifiers().add(XUtils.STANDARDCLASSIFIER);
		}		
		assignName(factory, log, inputFile.getFilename());
	}

	@Override
	public void startTrace(String caseId) {
		currentEvents.clear();
		hasStartEvents = false;
		errorDetected = false;
		currentTrace = factory.createTrace();
		assignName(factory, currentTrace, caseId);
	}

	@Override
	public void endTrace(String caseId) {
		if (errorDetected && conversionConfig.getErrorHandlingMode() == CSVErrorHandlingMode.OMIT_TRACE_ON_ERROR) {
			// Do not include the whole trace
			return;
		}
		if (hasStartEvents) {
			Collections.sort(currentEvents, new Comparator<XEvent>() {

				public int compare(XEvent o1, XEvent o2) {
					// assumes stable sorting so start events will be always before complete events
					return XTimeExtension.instance().extractTimestamp(o1)
							.compareTo(XTimeExtension.instance().extractTimestamp(o2));
				}
			});
		}
		currentTrace.addAll(currentEvents);
		log.add(currentTrace);
	}

	@Override
	public void startEvent(String eventClass, Date completionTime, Date startTime) {
		if (conversionConfig.getErrorHandlingMode() == CSVErrorHandlingMode.OMIT_EVENT_ON_ERROR) {
			// Include the other events in that trace
			errorDetected = false;
		}
		if (startTime != null && completionTime != null) {
			String instance = String.valueOf((instanceCounter++));
			hasStartEvents = true;

			currentStartEvent = factory.createEvent();
			if (eventClass != null) {
				assignName(factory, currentStartEvent, eventClass);
			}
			assignTimestamp(factory, currentStartEvent, startTime);
			assignInstance(factory, currentStartEvent, instance);
			assignLifecycleTransition(factory, currentStartEvent, XLifecycleExtension.StandardModel.START);

			currentEvent = factory.createEvent();
			if (eventClass != null) {
				assignName(factory, currentEvent, eventClass);
			}
			assignTimestamp(factory, currentEvent, completionTime);
			assignInstance(factory, currentEvent, instance);
			assignLifecycleTransition(factory, currentEvent, XLifecycleExtension.StandardModel.COMPLETE);
		} else {
			currentEvent = factory.createEvent();
			if (eventClass != null) {
				assignName(factory, currentEvent, eventClass);
			}
			if (completionTime != null) {
				assignTimestamp(factory, currentEvent, completionTime);
				assignLifecycleTransition(factory, currentEvent, XLifecycleExtension.StandardModel.COMPLETE);
			}
			if (startTime != null) {
				assignTimestamp(factory, currentEvent, startTime);
				assignLifecycleTransition(factory, currentStartEvent, XLifecycleExtension.StandardModel.START);
			}
		}
	}

	@Override
	public void startAttribute(String name, String value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeLiteral(getNameFromConfig(name), value, getExtensionFromConfig(name)));
		}
	}

	@Override
	public void startAttribute(String name, long value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeDiscrete(getNameFromConfig(name), value, getExtensionFromConfig(name)));
		}
	}

	@Override
	public void startAttribute(String name, double value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeContinuous(getNameFromConfig(name), value, getExtensionFromConfig(name)));
		}
	}

	@Override
	public void startAttribute(String name, Date value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeTimestamp(getNameFromConfig(name), value, getExtensionFromConfig(name)));
		}
	}

	@Override
	public void startAttribute(String name, boolean value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeBoolean(getNameFromConfig(name), value, getExtensionFromConfig(name)));
		}
	}

	private XExtension getExtensionFromConfig(String name) {
		ExtensionAttribute extensionAttribute = getExtensionAttribute(name);
		return extensionAttribute == null ? null : extensionAttribute.extension;
	}
	
	private String getNameFromConfig(String columnName) {
		CSVMapping csvMapping = getMapping(columnName);
		if (csvMapping.getEventExtensionAttribute() != null && csvMapping.getEventExtensionAttribute() != CSVConversionConfig.NO_EXTENSION_ATTRIBUTE) {
			return csvMapping.getEventExtensionAttribute().key;
		} else if (csvMapping.getEventAttributeName() != null && !csvMapping.getEventAttributeName().isEmpty()) {
			return csvMapping.getEventAttributeName();
		} else {
			return columnName;
		}
	}

	private ExtensionAttribute getExtensionAttribute(String name) {
		return getMapping(name).getEventExtensionAttribute();
	}

	private CSVMapping getMapping(String name) {
		return conversionConfig.getConversionMap().get(name);
	}

	@Override
	public void endAttribute() {
		//No-op
	}

	@Override
	public void endEvent() {
		if (errorDetected && conversionConfig.getErrorHandlingMode() == CSVErrorHandlingMode.OMIT_EVENT_ON_ERROR) {
			// Do not include the event
			return;
		}
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
		assignAttribute(a, factory.createAttributeLiteral(XLifecycleExtension.KEY_TRANSITION, lifecycle.getEncoding(),
				XLifecycleExtension.instance()));
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

	@Override
	public void errorDetected(int line, Object content, Exception e) throws CSVConversionException {
		CSVErrorHandlingMode errorMode = conversionConfig.getErrorHandlingMode();
		errorDetected = true;
		switch (errorMode) {
			case BEST_EFFORT :
				if (conversionErrors.length() < MAX_ERROR_LENGTH) {
					conversionErrors.append("Line: " + line + ": Skipping attribute " + nullSafeToString(content)
							+ " Error: " + e + "\n");
				}
				break;
			case OMIT_EVENT_ON_ERROR :
				if (conversionErrors.length() < MAX_ERROR_LENGTH) {
					conversionErrors.append("Line: " + line + ": Skipping event, could not convert "
							+ nullSafeToString(content) + " Error: " + e + "\n");
				}
				break;
			case OMIT_TRACE_ON_ERROR :
				if (conversionErrors.length() < MAX_ERROR_LENGTH) {
					conversionErrors.append("Line: " + line + ": Skipping trace " + XUtils.getConceptName(currentTrace)
							+ ", could not convert" + nullSafeToString(content) + " Error: " + e + "\n");
				}
				break;
			default :
			case ABORT_ON_ERROR :
				throw new CSVConversionException("Error converting " + content + " at line " + line, e);
		}
	}

	private static String nullSafeToString(Object obj) {
		if (obj == null) {
			return "NULL";
		} else if (obj.getClass().isArray()) {
			return Arrays.toString((Object[]) obj);
		} else {
			return obj.toString();
		}
	}

	private boolean specialColumn(String columnName) {
		return columnName == null || 
				(XConceptExtension.KEY_NAME.equals(columnName) && !conversionConfig.getEventNameColumns().isEmpty()) || 
				(XTimeExtension.KEY_TIMESTAMP.equals(columnName) && conversionConfig.getCompletionTimeColumn() != null) ||
				(XConceptExtension.KEY_INSTANCE.equals(columnName) && conversionConfig.getStartTimeColumn() != null);
	}

}