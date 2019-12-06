package org.processmining.log.parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class AddProcessAttributeParameters {

	private String processAttributeKey;
	private String[] attributeKeys;
	private Collection<String> processAttributeValues;
	private Collection<String> processAttributeValueFilter;
	private int maxCollectionSize;
	private boolean isBackward;
	
	public AddProcessAttributeParameters(XLog log) {
		setProcessAttributeKey("history");
		setBackward(true);
		setAttributeKeys(new String[]{ "concept:name" });
		setProcessAttributeValues(new ArrayList<String>());
		setMaxCollectionSize(1);
		Collection<String> filter = new HashSet<String>();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				XAttribute attribute = event.getAttributes().get("concept:name");
				if (attribute != null) {
					filter.add(attribute.toString());
				}
			}
		}
		setProcessAttributeValueFilter(filter);
	}
	
	public String getProcessAttributeKey() {
		return processAttributeKey;
	}
	
	public void setProcessAttributeKey(String processAttributeKey) {
		this.processAttributeKey = processAttributeKey;
	}
	
	public String[] getAttributeKeys() {
		return attributeKeys;
	}
	
	public void setAttributeKeys(String[] attributeKeys) {
		this.attributeKeys = attributeKeys;
	}

	public Collection<String> getProcessAttributeValues() {
		return processAttributeValues;
	}

	public void setProcessAttributeValues(Collection<String> processAttributeValues) {
		this.processAttributeValues = processAttributeValues;
	}

	public int getMaxCollectionSize() {
		return maxCollectionSize;
	}

	public void setMaxCollectionSize(int maxCollectionSize) {
		this.maxCollectionSize = maxCollectionSize;
	}

	public boolean isBackward() {
		return isBackward;
	}

	public void setBackward(boolean isBackward) {
		this.isBackward = isBackward;
	}

	public Collection<String> getProcessAttributeValueFilter() {
		return processAttributeValueFilter;
	}

	public void setProcessAttributeValueFilter(Collection<String> processAttributeValueFilter) {
		this.processAttributeValueFilter = processAttributeValueFilter;
	}
	
	
}
