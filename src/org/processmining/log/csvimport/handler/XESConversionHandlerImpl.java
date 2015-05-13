package org.processmining.log.csvimport.handler;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csvimport.CSVConversion.ProgressListener;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVErrorHandlingMode;
import org.processmining.log.csvimport.config.CSVImportConfig;
import org.processmining.log.csvimport.exception.CSVConversionException;
import org.processmining.log.utils.XUtils;

/**
 * Handler to create an XLog
 * 
 * @author F. Mannhardt
 *
 */
public final class XESConversionHandlerImpl implements CSVConversionHandler<XLog> {

	private static final int MAX_ERROR_LENGTH = 16 * 1024 * 1024;

	private final XFactory factory;
	private final CSVConversionConfig conversionConfig;
	private final StringBuilder conversionErrors;
	private final ProgressListener progress;

	private XLog log = null;

	private XTrace currentTrace = null;
	private List<XEvent> currentEvents = new ArrayList<>();
	private boolean hasStartEvents = false;

	private XEvent currentEvent = null;
	private int instanceCounter = 0;
	private XEvent currentStartEvent;

	private boolean errorDetected = false;

	public XESConversionHandlerImpl(ProgressListener progress, CSVImportConfig importConfig,
			CSVConversionConfig conversionConfig) {
		this.progress = progress;
		this.conversionConfig = conversionConfig;
		this.factory = conversionConfig.getFactory();
		this.conversionErrors = new StringBuilder();
	}

	public void startLog(CSVFile inputFile) {
		log = factory.createLog();
		if (conversionConfig.getEventNameColumns() != null) {
			log.getExtensions().add(XConceptExtension.instance());
		}
		if (conversionConfig.getCompletionTimeColumn() != null || conversionConfig.getStartTimeColumn() != null) {
			log.getExtensions().add(XTimeExtension.instance());
			log.getExtensions().add(XLifecycleExtension.instance());
		}
		//TODO add globals?
		assignName(factory, log, inputFile.getFilename());
	}

	public void startTrace(String caseId) {
		currentEvents.clear();
		hasStartEvents = false;
		errorDetected = false;
		currentTrace = factory.createTrace();
		assignName(factory, currentTrace, caseId);
	}

	public void endTrace(String caseId) {
		if (errorDetected && conversionConfig.getErrorHandlingMode() == CSVErrorHandlingMode.OMIT_TRACE_ON_ERROR) {
			// Do not include the whole trace
			return;
		}
		if (hasStartEvents) {
			Collections.sort(currentEvents, new Comparator<XEvent>() {

				public int compare(XEvent o1, XEvent o2) {
					return XTimeExtension.instance().extractTimestamp(o1)
							.compareTo(XTimeExtension.instance().extractTimestamp(o2));
				}
			});
		}
		currentTrace.addAll(currentEvents);
		log.add(currentTrace);
	}

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

	public void startAttribute(String name, String value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeLiteral(name, value, null));
		}
	}

	public void startAttribute(String name, long value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeDiscrete(name, value, null));
		}
	}

	public void startAttribute(String name, double value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeContinuous(name, value, null));
		}
	}

	public void startAttribute(String name, Date value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeTimestamp(name, value, null));
		}
	}

	public void startAttribute(String name, boolean value) {
		if (!specialColumn(name)) {
			assignAttribute(currentEvent, factory.createAttributeBoolean(name, value, null));
		}
	}

	public void endAttribute() {
		//No-op
	}

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
		progress.log(conversionErrors.toString());
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

	public void errorDetected(int line, Object content, Exception e) throws CSVConversionException {
		CSVErrorHandlingMode errorMode = conversionConfig.getErrorHandlingMode();
		errorDetected = true;
		switch (errorMode) {
			case BEST_EFFORT :
				if (conversionErrors.length() < MAX_ERROR_LENGTH) {
					conversionErrors.append("Line: " + line + ": Skipping attribute " + nulLSafeToString(content)
							+ " Error: " + e + "\n");
				}
				break;
			case OMIT_EVENT_ON_ERROR :
				if (conversionErrors.length() < MAX_ERROR_LENGTH) {
					conversionErrors.append("Line: " + line + ": Skipping event, could not convert "
							+ nulLSafeToString(content) + " Error: " + e + "\n");
				}
				break;
			case OMIT_TRACE_ON_ERROR :
				if (conversionErrors.length() < MAX_ERROR_LENGTH) {
					conversionErrors.append("Line: " + line + ": Skipping trace " + XUtils.getConceptName(currentTrace)
							+ ", could not convert" + nulLSafeToString(content) + " Error: " + e + "\n");
				}
				break;
			default :
			case ABORT_ON_ERROR :
				throw new CSVConversionException("Error converting " + content + " at line " + line, e);
		}
	}

	private static String nulLSafeToString(Object obj) {
		if (obj == null) {
			return "NULL";
		} else if (obj.getClass().isArray()) {
			return Arrays.toString((Object[]) obj);
		} else {
			return obj.toString();
		}
	}

	private static boolean specialColumn(String columnName) {
		return XConceptExtension.KEY_NAME.equals(columnName) || XTimeExtension.KEY_TIMESTAMP.equals(columnName)
				|| XConceptExtension.KEY_INSTANCE.equals(columnName);
	}

}