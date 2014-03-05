package org.processmining.plugins.log.logfilters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class AttributeFilterParameters {

	private Map<String,Set<String>> filter;

	public AttributeFilterParameters(XLog log) {
		filter = new HashMap<String,Set<String>>();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				for (String key : event.getAttributes().keySet()) {
					if (!filter.containsKey(key)) {
						filter.put(key, new HashSet<String>());
					}
					filter.get(key).add(event.getAttributes().get(key).toString());
				}
			}
		}
	}
	
	public void setFilter(Map<String,Set<String>> filter) {
		this.filter = filter;
	}

	public Map<String,Set<String>> getFilter() {
		return filter;
	}
}