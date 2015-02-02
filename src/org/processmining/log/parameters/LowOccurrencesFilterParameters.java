package org.processmining.log.parameters;

import org.deckfour.xes.model.XLog;
import org.processmining.log.utils.XUtils;

public class LowOccurrencesFilterParameters extends AbstractLogFilterParameters {

	private int threshold;

	public LowOccurrencesFilterParameters(XLog log) {
		/*
		 * Traces that occur at least 2 times will be retained.
		 */
		setThreshold(2);
		setClassifier(XUtils.getDefaultClassifier(log));
	}
	
	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}
}
