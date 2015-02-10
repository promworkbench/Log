package org.processmining.log.dialogs;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import javax.swing.JPanel;

import org.deckfour.xes.model.XLog;
import org.processmining.log.parameters.LogFrequencyParameters;

public class LogFrequencyDialog extends JPanel {

	public LogFrequencyDialog(XLog eventLog, final LogFrequencyParameters parameters) {
		double size[][] = { { TableLayoutConstants.FILL }, { TableLayoutConstants.FILL, } };
		setLayout(new TableLayout(size));
		
		add(new ClassifierPanel(eventLog.getClassifiers(), parameters), "0, 0");

	}
}