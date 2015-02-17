package org.processmining.log.parameters;


public interface LogFilterParameters extends ClassifierParameter {

	public void setMessageLevel(int level);
	
	public int getMessageLevel();
	
	public boolean isTryConnections();

	public void setTryConnections(boolean useConnections);

}
