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
import org.processmining.log.utils.XUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * XES serialization to CSV including all trace/event attributes. The names of
 * trace attributes are prefixed with "trace_", those of event attributes are
 * prefixed with "event_".
 *
 * @author F. Mannhardt, D. Fahland
 *
 */
public final class XesCsvSerializerAllLifeCycles implements XSerializer {

	private final FastDateFormat dateFormat;

	public XesCsvSerializerAllLifeCycles(String dateFormatString) {
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
			
			currentRow = compileEvent(trace, event, columnMap, rowLength, currentRow);
			traceList.add(currentRow);
		}
		return traceList;
	}

	private String[] compileEvent(XTrace trace, XEvent event,
			Map<String, Integer> columnMap, int rowLength, String[] lastRow) {

		String[] row = new String[rowLength];
		row[COL_CASE] = XConceptExtension.instance().extractName(trace);
		row[COL_EVENT] = XConceptExtension.instance().extractName(event);
		Date date = XTimeExtension.instance().extractTimestamp(event);
		if (date != null) {
			row[COL_TIME] = dateFormat.format(date);
		}
		StandardModel lifecycle = XLifecycleExtension.instance().extractStandardTransition(event);
		if (lifecycle != null) {
			row[COL_LIFECYCLE] = lifecycle.toString();
		}
		
		for (XAttribute attr : trace.getAttributes().values()) {
			if (!XUtils.isStandardExtensionAttribute(attr) || attr.getKey().startsWith("org:")) {
				assert columnMap.containsKey("trace_" + attr.getKey()) : "Column unkown " + attr.getKey();
				row[columnMap.get("trace_" + attr.getKey())] = convertAttribute(attr);
			}
		}
		for (XAttribute attr : event.getAttributes().values()) {
			if (!XUtils.isStandardExtensionAttribute(attr) || attr.getKey().startsWith("org:")) {
				assert columnMap.containsKey("event_" + attr.getKey()) : "Column unkown " + attr.getKey();
				row[columnMap.get("event_" + attr.getKey())] = convertAttribute(attr);
			}
		}
		
		// fill empty cells
		for (int i=0; i<rowLength; i++) {
			if (row[i] == null) row[i] = "";
		}
//		row[columnMap.get("final_dummy")] = " ";
		
		if (lastRow != null) {
			for (int i = 0; i < row.length; i++) {
				if (row[i] == null) {
					row[i] = lastRow[i];
				}
			}
		}
		return row;
	}
	
	private static int COL_CASE = 0;
	private static int COL_EVENT = 1;
	private static int COL_TIME = 2;
	private static int COL_LIFECYCLE = 3;
	
	private String[] compileHeader(XLog log, Map<String, Integer> columnMap) {
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);

		ArrayList<String> headerList = new ArrayList<String>();
		headerList.add("case");
		headerList.add("event");
		headerList.add("time");
		headerList.add(XLifecycleExtension.KEY_TRANSITION);

		int i = headerList.size() - 1;
		XAttributeInfo traceAttributeInfo = logInfo.getTraceAttributeInfo();
		for (XAttribute attr : traceAttributeInfo.getAttributes()) {
			if (!XUtils.isStandardExtensionAttribute(attr) || attr.getKey().startsWith("org:")) {
				i++;
				headerList.add(attr.getKey());
				columnMap.put("trace_" + attr.getKey(), i);
			}
		}
		XAttributeInfo eventAttributeInfo = logInfo.getEventAttributeInfo();
		for (XAttribute attr : eventAttributeInfo.getAttributes() ) {
			if (!XUtils.isStandardExtensionAttribute(attr) || attr.getKey().startsWith("org:")) {
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
		
		//headerList.add("final_dummy");
		//columnMap.put("final_dummy", headerList.size()-1);
		
		return headerList.toArray(new String[headerList.size()]);
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
			if (attribute.toString().isEmpty()) return " ";
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
