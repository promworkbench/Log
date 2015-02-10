package org.processmining.log.parameters;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.log.utils.XUtils;

public class LogFrequencyParameters implements ClassifierParameter {

	private XEventClassifier classifier;
	
	public LogFrequencyParameters(XLog log) {
		setClassifier(XUtils.getDefaultClassifier(log));
	}
	
	public void setClassifier(XEventClassifier classifier) {
		this.classifier = classifier;
	}

	public XEventClassifier getClassifier() {
		return classifier;
	}

}
