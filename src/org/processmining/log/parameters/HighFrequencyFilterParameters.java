package org.processmining.log.parameters;

import org.deckfour.xes.model.XLog;
import org.processmining.log.utils.XUtils;

public class HighFrequencyFilterParameters extends AbstractLogFilterParameters {

	private int frequencyThreshold;
	
	private int distanceThreshold;

	public HighFrequencyFilterParameters(XLog log) {
		/*
		 * Keep at least 50% of the log. This determines the set of most-occurring traces.
		 */
		setFrequencyThreshold(50);
		/*
		 * Keep a trace if its distance to a most-occurring trace is less than 3.
		 */
		setDistanceThreshold(3);
		setClassifier(XUtils.getDefaultClassifier(log));
	}
	
	public int getFrequencyThreshold() {
		return frequencyThreshold;
	}

	public void setFrequencyThreshold(int frequencyThreshold) {
		this.frequencyThreshold = frequencyThreshold;
	}

	public int getDistanceThreshold() {
		return distanceThreshold;
	}

	public void setDistanceThreshold(int distanceThreshold) {
		this.distanceThreshold = distanceThreshold;
	}
}
