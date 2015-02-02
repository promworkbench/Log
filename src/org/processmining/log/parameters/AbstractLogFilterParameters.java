package org.processmining.log.parameters;

import org.deckfour.xes.classification.XEventClassifier;

public abstract class AbstractLogFilterParameters implements LogFilterParameters {

	private XEventClassifier classifier;

	public XEventClassifier getClassifier() {
		return classifier;
	}

	public void setClassifier(XEventClassifier classifier) {
		this.classifier = classifier;
	}
}
