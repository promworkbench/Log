package org.processmining.log.parameters;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.log.utils.XUtils;

public class LogCentralityParameters {

	private XEventClassifier classifier;

	public LogCentralityParameters(XLog log) {
		setClassifier(XUtils.getDefaultClassifier(log));
	}


	public void setClassifier(XEventClassifier classifier) {
		this.classifier = classifier;
	}

	public XEventClassifier getClassifier() {
		return classifier;
	}

}
