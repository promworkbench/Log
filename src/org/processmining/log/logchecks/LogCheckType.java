package org.processmining.log.logchecks;

import org.processmining.log.logchecks.impl.LogCheckEventClassifiersGlobal;
import org.processmining.log.logchecks.impl.LogCheckGlobalAttributes;

public enum LogCheckType {
	LOG_CHECK_EVENT_CLASSIFIERS_GLOBAL(LogCheckEventClassifiersGlobal.getInstance()),
	LOG_CHECK_GLOBAL_ATTRIBUTE(LogCheckGlobalAttributes.getInstance());
	
	
	private LogCheck logCheck;
	
	private LogCheckType(LogCheck logCheck) {
		this.logCheck = logCheck;
	}
	
	public LogCheck getLogCheck() {
		return logCheck;
	}
	
	
}
