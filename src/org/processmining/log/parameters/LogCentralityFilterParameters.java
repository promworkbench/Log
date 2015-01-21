package org.processmining.log.parameters;

import org.processmining.log.models.LogCentrality;


public class LogCentralityFilterParameters {

	private int percentage;
	private boolean filterIn;

	public LogCentralityFilterParameters(LogCentrality centrality) {
		setPercentage(80);
		setFilterIn(true);
	}

	public void setPercentage(int percentage) {
		this.percentage = percentage;
	}

	public int getPercentage() {
		return percentage;
	}

	public void setFilterIn(boolean filterIn) {
		this.filterIn = filterIn;
	}

	public boolean isFilterIn() {
		return filterIn;
	}

}
