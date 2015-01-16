package org.processmining.log.csvexport;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.lang3.time.FastDateFormat;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.info.XAttributeInfo;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.logging.XLogging;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XSerializer;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * XES serialization to CSV including all trace/event attributes. The names of
 * trace attributes are prefixed with "trace_", those of event attributes are
 * prefixed with "event_".
 * 
 * @author F. Mannhardt
 * 
 */
public final class XesCsvSerializer implements XSerializer {

	private final FastDateFormat dateFormat;

	public XesCsvSerializer(String dateFormatString) {
		super();
		dateFormat = FastDateFormat.getInstance(dateFormatString);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deckfour.xes.out.XesSerializer#getDescription()
	 */
	public String getDescription() {
		return "XES CSV Serialization";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deckfour.xes.out.XesSerializer#getName()
	 */
	public String getName() {
		return "XES CSV";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deckfour.xes.out.XesSerializer#getAuthor()
	 */
	public String getAuthor() {
		return "F. Mannhardt";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deckfour.xes.out.XesSerializer#getSuffices()
	 */
	public String[] getSuffices() {
		return new String[] { "csv" };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.deckfour.xes.out.XesSerializer#serialize(org.deckfour.xes.model.XLog,
	 * java.io.OutputStream)
	 */
	public void serialize(XLog log, OutputStream out) throws IOException {
		XLogging.log("start serializing log to .csv", XLogging.Importance.DEBUG);
		long start = System.currentTimeMillis();

		CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, "UTF-8"));
		Map<String, Integer> columnMap = new HashMap<String, Integer>();

		String[] header = compileHeader(log, columnMap);
		writer.writeNext(header);

		for (XTrace trace : log) {
			writer.writeAll(compileTrace(trace, columnMap, header.length));
		}

		writer.close();
		String duration = " (" + (System.currentTimeMillis() - start) + " msec.)";
		XLogging.log("finished serializing log" + duration, XLogging.Importance.DEBUG);
	}

	private List<String[]> compileTrace(XTrace trace, Map<String, Integer> columnMap, int rowLength) {
		List<String[]> traceList = new ArrayList<String[]>();
		String[] currentRow = null;
		for (ListIterator<XEvent> iterator = trace.listIterator(); iterator.hasNext();) {
			XEvent event = iterator.next();
			StandardModel lifecycle = XLifecycleExtension.instance().extractStandardTransition(event);
			if (lifecycle == null || lifecycle == StandardModel.START) {
				XEvent completionEvent = null;
				if (lifecycle == StandardModel.START) {
					completionEvent = lookupCompletion(trace.listIterator(iterator.nextIndex()), event);
				}
				currentRow = compileEvent(trace, event, completionEvent, columnMap, rowLength, currentRow);
				traceList.add(currentRow);	
			}
		}
		return traceList;
	}

	private XEvent lookupCompletion(ListIterator<XEvent> listIterator, XEvent event) {
		XConceptExtension concept = XConceptExtension.instance();
		String eventInstance = concept.extractInstance(event);
		while (listIterator.hasNext()) {			
			XEvent e = listIterator.next();			
			if (eventInstance != null && eventInstance.equals(concept.extractInstance(e))) {
				StandardModel lifecycle = XLifecycleExtension.instance().extractStandardTransition(e);
				if (lifecycle == StandardModel.COMPLETE) {
					return e;
				}
			}
		}
		return null;
	}

	private String[] compileEvent(XTrace trace, XEvent event, XEvent completionEvent, Map<String, Integer> columnMap, int rowLength,
			String[] lastRow) {
		String[] row = new String[rowLength];
		row[0] = XConceptExtension.instance().extractName(trace);
		row[1] = XConceptExtension.instance().extractName(event);
		row[2] = dateFormat.format(XTimeExtension.instance().extractTimestamp(event));
		if (completionEvent != null) {
			row[3] = dateFormat.format(XTimeExtension.instance().extractTimestamp(completionEvent));	
		} else {
			row[3] = dateFormat.format(XTimeExtension.instance().extractTimestamp(event));
		}
		
		for (XAttribute attr : trace.getAttributes().values()) {
			if (!isStandardExtension(attr)) {
				assert columnMap.containsKey("trace_" + attr.getKey()) : "Column unkown " + attr.getKey();
				row[columnMap.get("trace_" + attr.getKey())] = convertAttribute(attr);	
			}
		}
		for (XAttribute attr : event.getAttributes().values()) {
			if (!isStandardExtension(attr)) {
				assert columnMap.containsKey("event_" + attr.getKey()) : "Column unkown " + attr.getKey();
				row[columnMap.get("event_" + attr.getKey())] = convertAttribute(attr);	
			}
		}
		if (lastRow != null) {
			for (int i = 0; i < row.length; i++) {
				if (row[i] == null) {
					row[i] = lastRow[i];
				}
			}
		}
		return row;
	}

	private String[] compileHeader(XLog log, Map<String, Integer> columnMap) {
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
		
		List<String> headerList = new ArrayList<String>();
		headerList.add("case");
		headerList.add("event");
		headerList.add("startTime");
		headerList.add("completeTime");

		int i = headerList.size() - 1;
		XAttributeInfo traceAttributeInfo = logInfo.getTraceAttributeInfo();
		for (XAttribute attr : traceAttributeInfo.getAttributes()) {
			if (!isStandardExtension(attr)) {
				i++;
				headerList.add(attr.getKey());
				columnMap.put("trace_" + attr.getKey(), i);				
			}
		}
		XAttributeInfo eventAttributeInfo = logInfo.getEventAttributeInfo();
		for (XAttribute attr : eventAttributeInfo.getAttributes()) {
			if (!isStandardExtension(attr)) {
				i++;
				if (headerList.contains(attr.getKey())) {
					headerList.add("event_" + attr.getKey());
					columnMap.put("event_" + attr.getKey(), i);
				} else {
					headerList.add(attr.getKey());
					columnMap.put("event_" + attr.getKey(), i);
				}
			}
		}
		return headerList.toArray(new String[headerList.size()]);
	}

	private boolean isStandardExtension(XAttribute attr) {
		return attr.getKey().equals(XConceptExtension.KEY_NAME) || attr.getKey().equals(XTimeExtension.KEY_TIMESTAMP);
	}

	/**
	 * Helper method, returns the String representation of the attribute
	 * 
	 * @param attribute
	 *            The attributes to convert
	 */
	protected String convertAttribute(XAttribute attribute) {
		if (attribute instanceof XAttributeTimestamp) {
			Date timestamp = ((XAttributeTimestamp) attribute).getValue();
			return dateFormat.format(timestamp);
		} else {
			return attribute.toString();
		}
	}

	/**
	 * toString() defaults to getName().
	 */
	public String toString() {
		return this.getName();
	}

}