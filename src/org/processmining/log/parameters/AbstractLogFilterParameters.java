package org.processmining.log.parameters;

import org.deckfour.xes.classification.XEventClassifier;

public abstract class AbstractLogFilterParameters implements LogFilterParameters {

	private XEventClassifier classifier;
	private boolean tryConnections;

	private int messageLevel;

	public final static int MESSAGE = 1;
	public final static int WARNING = 2;
	public final static int ERROR = 3;
	public final static int DEBUG = 4;

	public AbstractLogFilterParameters(XEventClassifier classifier) {
		setClassifier(classifier);
	}

	public AbstractLogFilterParameters(AbstractLogFilterParameters parameters) {
		setClassifier(parameters.getClassifier());
		setTryConnections(true);
	}

	public XEventClassifier getClassifier() {
		return classifier;
	}

	public void setClassifier(XEventClassifier classifier) {
		this.classifier = classifier;
	}

	public void setMessageLevel(int level) {
		this.messageLevel = level;
	}

	public int getMessageLevel() {
		return messageLevel;
	}

	public void displayMessage(String text) {
		if (messageLevel >= MESSAGE) {
			System.out.println(text);
		}
	}

	public void displayWarning(String text) {
		if (messageLevel >= WARNING) {
			System.out.println(text);
		}
	}

	public void displayError(String text) {
		if (messageLevel >= ERROR) {
			System.err.println(text);
		}
	}

	public boolean isTryConnections() {
		return tryConnections;
	}

	public void setTryConnections(boolean useConnections) {
		this.tryConnections = useConnections;
	}

	public boolean equals(Object object) {
		if (object instanceof AbstractLogFilterParameters) {
			AbstractLogFilterParameters parameters = (AbstractLogFilterParameters) object;
			return getClassifier().equals(parameters.getClassifier()) &&
					isTryConnections() == parameters.isTryConnections();
		}
		return false;
	}
}
