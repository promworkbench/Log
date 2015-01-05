package org.processmining.filtering;

import java.util.Arrays;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.filtering.filter.factories.FilterFactory;
import org.processmining.filtering.filter.interfaces.Filter;
import org.processmining.filtering.xflog.implementations.XFTraceImpl;

public class LeaveEventOutIfConceptNameContainsFilterImpl implements Filter<XTrace> {

	private String val;

	public LeaveEventOutIfConceptNameContainsFilterImpl(String val) {
		this.val = val;
	}

	@Override
	public XTrace apply(XTrace xEvents) {
		int[] keep = new int[0];
//		XEventClassifier classifier = new XEventNameClassifier();
		XConceptExtension conceptExtension = XConceptExtension.instance();
		for (int i = 0; i < xEvents.size(); i++) {
			if (!(conceptExtension.extractName(xEvents.get(i)).contains(val))) {
				keep = Arrays.copyOf(keep, keep.length + 1);
				keep[keep.length - 1] = i;
			}
			System.out.println(conceptExtension.extractName(xEvents.get(i)));

		}
		Filter<XEvent> eventFilter = FilterFactory.mirrorFilter();
		Filter<XAttributeMap> traceAttributeFilter = FilterFactory.mirrorFilter();
		return new XFTraceImpl(xEvents, keep, eventFilter, traceAttributeFilter);
	}

	public Object clone() {
		Object clone = null;
		try {
			clone = super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}
}
