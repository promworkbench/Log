package org.processmining.plugins.log.logfilters.impl;

public class AddArtificialStartEndParameters {

	public final static String START = "|start>";
	public final static String END = "[end]";
	
	/*
	 * Class name for start events.
	 */
	private String startClassName;
	/*
	 * Class names for end events.
	 */
	private String endClassName;
	/*
	 * Whether to add start events.
	 */
	private boolean addStartEvent;
	/*
	 * Whether to add end events.
	 */
	private boolean addEndEvent;
	
	public AddArtificialStartEndParameters() {
		/*
		 * Default settings.
		 */
		setStartClassName(START);
		setEndClassName(END);
		setAddStartEvent(true);
		setAddEndEvent(true);
	}

	public String getStartClassName() {
		return startClassName;
	}

	public void setStartClassName(String startClassName) {
		this.startClassName = startClassName;
	}

	public String getEndClassName() {
		return endClassName;
	}

	public void setEndClassName(String endClassName) {
		this.endClassName = endClassName;
	}

	public boolean isAddStartEvent() {
		return addStartEvent;
	}

	public void setAddStartEvent(boolean addStartEvent) {
		this.addStartEvent = addStartEvent;
	}

	public boolean isAddEndEvent() {
		return addEndEvent;
	}

	public void setAddEndEvent(boolean addEndEvent) {
		this.addEndEvent = addEndEvent;
	}
}
