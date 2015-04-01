package org.processmining.log.csvimport.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csvimport.CSVConversion.Datatype;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVErrorHandlingMode;
import org.processmining.log.csvimport.config.CSVImportConfig;

import au.com.bytecode.opencsv.CSVReader;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.google.common.collect.Lists;

/**
 * UI for the configuration of the actual conversion
 * 
 * @author F. Mannhardt
 *
 */
public final class ConversionConfigUI extends JPanel implements AutoCloseable {

	private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	private final class ChangeListenerImpl implements ActionListener {

		private final int caseColumnIndex;
		private ProMComboBox<String> caseColumnCbx;

		public ChangeListenerImpl(int caseColumnIndex, ProMComboBox<String> caseColumnCbx) {
			this.caseColumnIndex = caseColumnIndex;
			this.caseColumnCbx = caseColumnCbx;
		}

		public void actionPerformed(ActionEvent e) {
			updateSettings();
		}

		public void updateSettings() {
			conversionConfig.datatypeMapping.remove(findColumnIndex(headers, conversionConfig.completionTimeColumn));
			conversionConfig.caseColumns = updateCaseArray();
			conversionConfig.eventNameColumn = eventColumnCbx.getSelectedItem().toString();
			conversionConfig.startTimeColumn = startTimeColumnCbx.getSelectedItem().toString();
			conversionConfig.completionTimeColumn = completionTimeColumnCbx.getSelectedItem().toString();
			conversionConfig.datatypeMapping.put(findColumnIndex(headers, conversionConfig.completionTimeColumn),
					Datatype.TIME);
			previewFrame.refresh();
		}

		private final String[] updateCaseArray() {
			String column = caseColumnCbx.getSelectedItem().toString();
			if (column.equals("")) {
				while (caseColumnCbxStack.size() > (caseColumnIndex + 1) && caseColumnCbxStack.size() > 1) {
					ProMComboBox<String> toRemove = caseColumnCbxStack.pop();
					caseColumnBox.remove(toRemove);
				}
				ConversionConfigUI.this.validate();
			} else {
				if (caseColumnCbxStack.size() == (caseColumnIndex + 1)) {
					ProMComboBox<String> comboBox = createNewComboBox();
					comboBox.addActionListener(new ChangeListenerImpl(caseColumnIndex + 1, comboBox));
					caseColumnBox.add(comboBox);
					ConversionConfigUI.this.validate();
					caseColumnCbxStack.push(comboBox);
				}
			}

			int i = 0;
			String[] caseColumns = new String[caseColumnCbxStack.size() - 1];
			for (Iterator<ProMComboBox<String>> iterator = caseColumnCbxStack.descendingIterator(); iterator.hasNext();) {
				ProMComboBox<String> proMComboBox = iterator.next();
				String selectedItem = proMComboBox.getSelectedItem().toString();
				if (!selectedItem.isEmpty()) {
					caseColumns[i++] = selectedItem;
				}
			}
			return caseColumns;
		}

		private ProMComboBox<String> createNewComboBox() {
			ProMComboBox<String> proMComboBox = new ProMComboBox<>(headersInclEmpty);
			proMComboBox.setPreferredSize(null);
			proMComboBox.setMinimumSize(null);
			proMComboBox.setAlignmentX(LEFT_ALIGNMENT);
			return proMComboBox;
		}

	}

	private final class LoadCSVRecordsWorker extends SwingWorker<Void, Object[]> {
		protected Void doInBackground() throws Exception {
			String[] nextLine;
			int i = 0;
			while ((nextLine = reader.readNext()) != null && i < maxLoad) {
				publish(nextLine);
				i++;
			}
			return null;
		}

		protected void process(List<Object[]> chunks) {
			previewFrame.addRows(chunks);
			previewFrame.setTitle(String.format("CSV Preview (%s rows - scroll down to load more)", previewFrame
					.getPreviewTable().getModel().getRowCount()));
		}
	}

	private static final long serialVersionUID = 1L;

	private final CSVConversionConfig conversionConfig;

	private final String[] headers;
	private final String[] headersInclEmpty;

	private final JPanel caseColumnBox;
	private final Deque<ProMComboBox<String>> caseColumnCbxStack;

	private final ProMComboBox<String> eventColumnCbx;
	private final ProMComboBox<String> completionTimeColumnCbx;
	private final ProMComboBox<String> startTimeColumnCbx;

	private final ProMComboBox<Boolean> repairDataTypesCbx;
	private final ProMComboBox<Boolean> omitNULLCbx;
	private final ProMComboBox<CSVErrorHandlingMode> errorHandlingModeCbx;
	private final ProMTextField timeFormatField;

	private final CSVReader reader;
	private final CSVPreviewFrame previewFrame;
	private int maxLoad = 5000;

	public ConversionConfigUI(final CSVFile csv, final CSVImportConfig importConfig) throws IOException {
		conversionConfig = new CSVConversionConfig();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		reader = csv.createReader(importConfig);
		headers = reader.readNext();
		headersInclEmpty = Lists.asList("", headers).toArray(new String[headers.length + 1]);

		JLabel standardAttributesLabel = SlickerFactory.instance().createLabel("Mapping to 'standard' XES attributes");
		standardAttributesLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(standardAttributesLabel);

		caseColumnBox = new JPanel();
		caseColumnBox.setLayout(new BoxLayout(caseColumnBox, BoxLayout.Y_AXIS));
		caseColumnBox.setBackground(null);
		caseColumnBox.setOpaque(false);
		caseColumnBox.setAlignmentX(LEFT_ALIGNMENT);
		caseColumnCbxStack = new ArrayDeque<>();
		ProMComboBox<String> proMComboBox = new ProMComboBox<>(headersInclEmpty);
		proMComboBox.setPreferredSize(null);
		proMComboBox.setMinimumSize(null);
		proMComboBox.setAlignmentX(LEFT_ALIGNMENT);
		JLabel caseLabel = SlickerFactory.instance().createLabel("Case Column(s) (Optional - Groups events into traces)");
		caseLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(caseLabel);
		caseColumnBox.add(proMComboBox);
		add(caseColumnBox);

		ChangeListenerImpl changeListener = new ChangeListenerImpl(0, proMComboBox);
		proMComboBox.addActionListener(changeListener);
		caseColumnCbxStack.add(proMComboBox);

		eventColumnCbx = new ProMComboBox<>(headersInclEmpty);
		eventColumnCbx.setPreferredSize(null);
		eventColumnCbx.setMinimumSize(null);
		eventColumnCbx.setAlignmentX(LEFT_ALIGNMENT);
		JLabel eventLabel = SlickerFactory.instance().createLabel("Event Column (Optional - Mapped to 'concept:name')");
		eventLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(eventLabel);
		add(eventColumnCbx);
		eventColumnCbx.addActionListener(changeListener);

		completionTimeColumnCbx = new ProMComboBox<>(headersInclEmpty);
		completionTimeColumnCbx.setPreferredSize(null);
		completionTimeColumnCbx.setMinimumSize(null);
		completionTimeColumnCbx.setAlignmentX(LEFT_ALIGNMENT);
		JLabel completionTimeLabel = SlickerFactory.instance().createLabel(
				"Completion Time Column (Optional - Mapped to 'time:timestamp'");
		completionTimeLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(completionTimeLabel);
		add(completionTimeColumnCbx);
		completionTimeColumnCbx.addActionListener(changeListener);

		startTimeColumnCbx = new ProMComboBox<>(headersInclEmpty);
		startTimeColumnCbx.setPreferredSize(null);
		startTimeColumnCbx.setMinimumSize(null);
		startTimeColumnCbx.setAlignmentX(LEFT_ALIGNMENT);
		JLabel startTimeLabel = SlickerFactory
				.instance()
				.createLabel(
						"Start Time Column (Optional - Mapped to 'time:timestamp' of a separate event with automatically generated 'concept:instance'");
		startTimeLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(startTimeLabel);
		add(startTimeColumnCbx);
		startTimeColumnCbx.addActionListener(changeListener);

		add(Box.createVerticalStrut(10));

		JLabel conversionOptionsLabel = SlickerFactory.instance().createLabel(
				"Various Expert Conversion Options (If unsure DEFAULT is a good guess)");
		conversionOptionsLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(conversionOptionsLabel);

		timeFormatField = new ProMTextField(DEFAULT_FORMAT);
		timeFormatField.setAlignmentX(LEFT_ALIGNMENT);
		timeFormatField.setPreferredSize(null);
		timeFormatField.setMinimumSize(null);
		JLabel timeFormatLabel = SlickerFactory.instance().createLabel(
				"Time Format (Java 'Style'), there are several common time formats already built in.");
		timeFormatLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(timeFormatLabel);
		add(timeFormatField);
		timeFormatField.getDocument().addDocumentListener(new DocumentListener() {

			public void removeUpdate(DocumentEvent e) {
				conversionConfig.timeFormat = timeFormatField.getText().equals(DEFAULT_FORMAT) ? null : timeFormatField
						.getText();
			}

			public void insertUpdate(DocumentEvent e) {
				conversionConfig.timeFormat = timeFormatField.getText().equals(DEFAULT_FORMAT) ? null : timeFormatField
						.getText();
			}

			public void changedUpdate(DocumentEvent e) {
				conversionConfig.timeFormat = timeFormatField.getText().equals(DEFAULT_FORMAT) ? null : timeFormatField
						.getText();
			}
		});

		repairDataTypesCbx = new ProMComboBox<>(new Boolean[] { true, false });
		repairDataTypesCbx.setPreferredSize(null);
		repairDataTypesCbx.setMinimumSize(null);
		repairDataTypesCbx.setSelectedItem(conversionConfig.shouldGuessDataTypes);
		repairDataTypesCbx.setAlignmentX(LEFT_ALIGNMENT);
		JLabel repairDataTypesLabel = SlickerFactory
				.instance()
				.createLabel(
						"Guess Attribute Types: Should the plug-in make an attempt to guess the correct datatypes? Leave as 'true' in case you are unsure!");
		repairDataTypesLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(repairDataTypesLabel);
		add(repairDataTypesCbx);
		repairDataTypesCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.shouldGuessDataTypes = (Boolean) repairDataTypesCbx.getSelectedItem();
			}
		});

		omitNULLCbx = new ProMComboBox<>(new Boolean[] { true, false });
		omitNULLCbx.setPreferredSize(null);
		omitNULLCbx.setMinimumSize(null);
		omitNULLCbx.setSelectedItem(conversionConfig.omitNULL);
		omitNULLCbx.setAlignmentX(LEFT_ALIGNMENT);
		JLabel omitNULLLabel = SlickerFactory.instance().createLabel(
				"Omit NULL and Empty Cells: Don't create attributes for cells that are empty or contain the literal value 'NULL'?");
		omitNULLLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(omitNULLLabel);
		add(omitNULLCbx);
		omitNULLCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.omitNULL = (Boolean) omitNULLCbx.getSelectedItem();
			}
		});

		errorHandlingModeCbx = new ProMComboBox<>(CSVErrorHandlingMode.values());
		errorHandlingModeCbx.setPreferredSize(null);
		errorHandlingModeCbx.setMinimumSize(null);
		errorHandlingModeCbx.setSelectedItem(conversionConfig.errorHandlingMode);
		errorHandlingModeCbx.setAlignmentX(LEFT_ALIGNMENT);
		JLabel strictModeLabel = SlickerFactory.instance().createLabel(
				"Strict Mode: Stop conversion upon malformed input or try to import as much as possible?");
		strictModeLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(strictModeLabel);
		add(errorHandlingModeCbx);
		errorHandlingModeCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.errorHandlingMode = (CSVErrorHandlingMode) errorHandlingModeCbx.getSelectedItem();
			}
		});
		
		previewFrame = new CSVPreviewFrame(headers, conversionConfig);
		previewFrame.getMainScrollPane().getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

			public void adjustmentValueChanged(AdjustmentEvent e) {
				int maximum = e.getAdjustable().getMaximum();
				int current = e.getValue();
				if (Math.abs(maximum - current) < 1000 && !e.getValueIsAdjusting()) {
					new LoadCSVRecordsWorker().execute();
				}
			}
		});

		autoDetectCaseColumn();
		autoDetectEventColumn();
		autoDetectCompletionTimeColumn();		
		changeListener.updateSettings();
	}

	private void showPreviewFrame() {
		previewFrame.showFrame(this);
		try {
			// Update Content
			new LoadCSVRecordsWorker().execute();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error parsing CSV " + e.getMessage(), "CSV Parsing Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void autoDetectCaseColumn() {

		
		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if ("case".equalsIgnoreCase(header) 
					|| "trace".equalsIgnoreCase(header)
					|| "traceid".equalsIgnoreCase(header)
					|| "caseid".equalsIgnoreCase(header)) {
				caseColumnCbxStack.peek().setSelectedItem(header);
				return;
			}
						
		}

	}

	private void autoDetectEventColumn() {

		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if ("event".equalsIgnoreCase(header) 
					|| "activity".equalsIgnoreCase(header)
					|| "eventid".equalsIgnoreCase(header)
					|| "activityid".equalsIgnoreCase(header)) {
				eventColumnCbx.setSelectedItem(header);
				return;
			}
		}

	}

	private void autoDetectCompletionTimeColumn() {

		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if ("time".equalsIgnoreCase(header) 
					|| "timestamp".equalsIgnoreCase(header)
					|| "date".equalsIgnoreCase(header)
					|| "datetime".equalsIgnoreCase(header)
					|| "eventtime".equalsIgnoreCase(header)) {
				completionTimeColumnCbx.setSelectedItem(header);
				return;
			}
		}

	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#addNotify()
	 */
	@Override
	public void addNotify() {
		super.addNotify();
		showPreviewFrame();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#removeNotify()
	 */
	@Override
	public void removeNotify() {
		super.removeNotify();
		previewFrame.setVisible(false);
	}

	public CSVConversionConfig getConversionConfig() {
		return conversionConfig;
	}

	private int findColumnIndex(String[] header, String caseColumn) {
		int i = 0;
		for (String column : header) {
			if (column.equals(caseColumn)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.AutoCloseable#close()
	 */
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.toString());
		}
	}

}
