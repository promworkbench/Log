package org.processmining.log.parameters;

import org.processmining.log.models.LogCentrality;


public class LogCentralityFilterParameters {

	private int percentage;
	private boolean filterIn;
	private boolean tryConnections;

	public LogCentralityFilterParameters(LogCentrality centrality) {
		setPercentage(80);
		setFilterIn(true);
		setTryConnections(true);
	}

	public LogCentralityFilterParameters(LogCentralityFilterParameters parameters) {
		setPercentage(parameters.getPercentage());
		setFilterIn(parameters.isFilterIn());
		setTryConnections(parameters.isTryConnections());
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

	public boolean isTryConnections() {
		return tryConnections;
	}

	public void setTryConnections(boolean tryConnections) {
		this.tryConnections = tryConnections;
	}
	
	public boolean equals(Object object) {
		if (object instanceof LogCentralityFilterParameters) {
			LogCentralityFilterParameters parameters = (LogCentralityFilterParameters) object;
			return getPercentage() == parameters.getPercentage() &&
					isFilterIn() == parameters.isFilterIn() &&
					isTryConnections() == parameters.isTryConnections();
		}
		return false;
	}

}
