package org.processmining.log.algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.log.parameters.MergeLogsParameters;

public class MergeLogsAlgorithm {

	public XLog apply(PluginContext context, XLog mainLog, XLog subLog, MergeLogsParameters parameters) {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		DateFormat df = new SimpleDateFormat(parameters.getDateFormat());						
		
		long time = -System.currentTimeMillis();

		for (XTrace mainTrace: mainLog) {
			boolean doApply = true;
			if (doApply && parameters.getTraceId() != null) {
				/*
				 * User has selected specific main trace. Filter in only the trace that has that id as concept:name.
				 */
				String id = XConceptExtension.instance().extractName(mainTrace);
				doApply = (id != null && id.equals(parameters.getTraceId()));
			}
			if (doApply && parameters.getFromDate() != null && parameters.getToDate() != null) {
				/*
				 * User has selected from date and to date. Filter in only those traces that occur entirely in that interval.
				 */
				doApply = isBetween(mainTrace, parameters.getFromDate(), parameters.getToDate());
			}
			if (doApply && parameters.getSpecificDate() != null) {
				/*
				 * User has selected a specific date. Only filter in those traces that have this exact date.
				 */
				doApply = false;
				for (XEvent event : mainTrace) {
					Date date = XTimeExtension.instance().extractTimestamp(event);
					if (date.equals(parameters.getSpecificDate())) {
						doApply = true;
						continue;
					}
				}
			}
			if (doApply && parameters.getRequiredWords() != null) {
				/*
				 * User has selected required words. Filter in those traces that match one of these words.
				 */
				doApply = false;
				Collection<String> required = new HashSet<String>(Arrays.asList(parameters.getRequiredWords().split(",")));
				for (XEvent event : mainTrace) {
					for (XAttribute attribute : event.getAttributes().values()) {
						if (attribute instanceof XAttributeLiteral) {
							String value = ((XAttributeLiteral) attribute).getValue();
							if (required.contains(value)) {
								doApply = true;
								continue;
							}
						} else if (attribute instanceof XAttributeDiscrete) {
							long value = ((XAttributeDiscrete) attribute).getValue();
							if (required.contains(String.valueOf(value))) {
								doApply = true;
								continue;
							}
						} else if (attribute instanceof XAttributeContinuous) {
							double value = ((XAttributeContinuous) attribute).getValue();
							if (required.contains(String.valueOf(value))) {
								doApply = true;
								continue;
							}
						} else if (attribute instanceof XAttributeTimestamp) {
							Date value = ((XAttributeTimestamp) attribute).getValue();
							if (required.contains(df.format(value))) {
								doApply = true;
								continue;
							}
						}
					}
					if (doApply) {
						continue;
					}
				}
			}
			if (doApply && parameters.getForbiddenWords() != null) {
				/*
				 * User has selected forbidden words. Filter out those traces that match one of these words.
				 */
				doApply = true;
				Collection<String> forbidden = new HashSet<String>(Arrays.asList(parameters.getForbiddenWords().split(",")));
				for (XEvent event : mainTrace) {
					for (XAttribute attribute : event.getAttributes().values()) {
						if (attribute instanceof XAttributeLiteral) {
							String value = ((XAttributeLiteral) attribute).getValue();
							if (forbidden.contains(value)) {
								doApply = false;
								continue;
							}
						} else if (attribute instanceof XAttributeDiscrete) {
							long value = ((XAttributeDiscrete) attribute).getValue();
							if (forbidden.contains(String.valueOf(value))) {
								doApply = false;
								continue;
							}
						} else if (attribute instanceof XAttributeContinuous) {
							double value = ((XAttributeContinuous) attribute).getValue();
							if (forbidden.contains(String.valueOf(value))) {
								doApply = false;
								continue;
							}
						} else if (attribute instanceof XAttributeTimestamp) {
							Date value = ((XAttributeTimestamp) attribute).getValue();
							if (forbidden.contains(df.format(value))) {
								doApply = false;
								continue;
							}
						}
					}
					if (!doApply) {
						continue;
					}
				}
			}
			if (doApply) {
				/*
				 * Main trace has passed all filters. Add it with all corresponding sub traces to the resulting log.
				 */
				apply(context, mainTrace, mainLog, subLog, log, parameters);
			}
		}
		
		time += System.currentTimeMillis();
		context.log("Merging time :" +  convet_MS(time));
		return log;
	}

	private void apply(PluginContext context, XTrace mainTrace, XLog mainLog, XLog subLog, XLog log,
			MergeLogsParameters parameters) {
		for (XTrace subTrace : subLog) {
			if (isBetween(mainTrace, subTrace)) {
				int match = checkMatch(mainTrace, subTrace);
				if (match > parameters.getRelated()) {
					XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace(mainTrace.getAttributes());
					int mainCtr = 0;
					int subCtr = 0;
					while (mainCtr < mainTrace.size() && subCtr < subTrace.size()) {
						Date mainDate = XTimeExtension.instance().extractTimestamp(mainTrace.get(mainCtr));
						if (mainDate == null) {
							trace.add(mainTrace.get(mainCtr));
							mainCtr++;
						} else {
							Date subDate = XTimeExtension.instance().extractTimestamp(subTrace.get(subCtr));
							if (subDate == null) {
								trace.add(subTrace.get(subCtr));
								subCtr++;
							} else if (subDate.before(mainDate)) {
								trace.add(subTrace.get(subCtr));
								subCtr++;
							} else {
								trace.add(mainTrace.get(mainCtr));
								mainCtr++;
							}
						}
					}
					log.add(trace);
				}
			}
		}
	}

	private int checkMatch(XTrace mainTrace, XTrace subTrace) {
		int match = 0;
		for (XEvent mainEvent : mainTrace) {
			for (XEvent subEvent : mainTrace) {
				match += checkMatch(mainEvent, subEvent);
			}
		}
		return match;
	}

	private int checkMatch(XEvent mainEvent, XEvent subEvent) {
		int match = 0;
		for (XAttribute mainAttribute : mainEvent.getAttributes().values()) {
			if (!(mainAttribute instanceof XAttributeTimestamp)) {
				for (XAttribute subAttribute : subEvent.getAttributes().values()) {
					if (mainAttribute.equals(subAttribute)) {
						match++;
					}
				}
			}
		}
		return match;
	}

	private boolean isBetween(XTrace trace, Date firstDate, Date lastDate) {
		Date firstTraceDate = getFirstDate(trace);
		Date lastTraceDate = getLastDate(trace);
		if (firstTraceDate == null || lastTraceDate == null) {
			return false;
		}
		return (!firstTraceDate.before(firstDate) && !lastTraceDate.after(lastDate));
	}

	private boolean isBetween(XTrace mainTrace, XTrace subTrace) {
		Date firstTraceDate = getFirstDate(mainTrace);
		Date lastTraceDate = getLastDate(mainTrace);
		if (firstTraceDate == null || lastTraceDate == null) {
			return false;
		}
		return isBetween(subTrace, firstTraceDate, lastTraceDate);
	}

	private Date getFirstDate(XTrace trace) {
		Date firstDate = null;
		for (XEvent event : trace) {
			Date date = XTimeExtension.instance().extractTimestamp(event);
			if (firstDate == null) {
				firstDate = date;
			} else if (date.before(firstDate)) {
				firstDate = date;
			}
		}
		return firstDate;
	}

	private Date getLastDate(XTrace trace) {
		Date lastDate = null;
		for (XEvent event : trace) {
			Date date = XTimeExtension.instance().extractTimestamp(event);
			if (lastDate == null) {
				lastDate = date;
			} else if (date.after(lastDate)) {
				lastDate = date;
			}
		}
		return lastDate;
	}

	private String convet_MS(long millis) {

		return String.format(
				"%d min, %d sec %d ms",
				TimeUnit.MILLISECONDS.toMinutes(millis),
				TimeUnit.MILLISECONDS.toSeconds(millis)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)), millis
						- TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)));
	}

}
