package org.processmining.log.parameters;

import org.deckfour.xes.model.XLog;
import org.processmining.log.utils.XUtils;

public class LowFrequencyFilterParameters extends AbstractLogFilterParameters {

	private int threshold;

	public LowFrequencyFilterParameters(XLog log) {
		/*
		 * The least-occurring traces that make up at least 5% of the log will be removed.
		 */
		setThreshold(5); 
		setClassifier(XUtils.getDefaultClassifier(log));
	}
	
	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

}
