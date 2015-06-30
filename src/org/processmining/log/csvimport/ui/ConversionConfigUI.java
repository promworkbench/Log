package org.processmining.log.csvimport.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMListSortableWithComboBox;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.ICSVReader;
import org.processmining.log.csv.config.CSVConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVMapping;

import com.fluxicon.slickerbox.components.SlickerButton;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.google.common.collect.Lists;

/**
 * UI for the configuration of the actual conversion
 * 
 * @author F. Mannhardt
 *
 */
public final class ConversionConfigUI extends CSVConfigurationPanel implements AutoCloseable {

	private static final int COLUMN_WIDTH = 360;

	private static final int DATA_TYPE_FORMAT_AUTO_DETECT_NUM_LINES = 100;
	
	private static final Set<String> CASE_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = 1113995381788343439L;
	{
		add("case");
		add("trace");
		add("traceid");
		add("caseid");
	}};
	
	private static final Set<String> EVENT_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = -4218883319932959922L;
	{
		add("event");
		add("eventname");
		add("activity");
		add("eventid");
		add("activityid");
	}};
	
	private static final Set<String> COMPLETION_TIME_COLUMN_IDS = new HashSet<String>() {
		private static final long serialVersionUID = 6419129336151793063L;
	{
		add("completiontime");
		add("time");
		add("date");
		add("enddate");
		add("timestamp");
		add("datetime");
		add("date");
		add("eventtime");
		add("tijd");
		add("datum");
	}};

	private final class ChangeListenerImpl implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			updateSettings();
		}

		public void updateSettings() {
			if (conversionConfig.getCompletionTimeColumn() != null) {
				conversionConfig.getConversionMap().put(conversionConfig.getCompletionTimeColumn(), new CSVMapping());
			}
			if (conversionConfig.getStartTimeColumn() != null) {
				conversionConfig.getConversionMap().put(conversionConfig.getStartTimeColumn(), new CSVMapping());
			}
			conversionConfig.setCaseColumns(caseComboBox.getElements().toArray(
					new String[caseComboBox.getElements().size()]));
			conversionConfig.setEventNameColumns(eventComboBox.getElements().toArray(
					new String[eventComboBox.getElements().size()]));
			conversionConfig.setStartTimeColumn(startTimeColumnCbx.getSelectedItem().toString());
			conversionConfig.setCompletionTimeColumn(completionTimeColumnCbx.getSelectedItem().toString());
			previewFrame.refresh();
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

	private final ProMListSortableWithComboBox<String> caseComboBox;
	private final ProMListSortableWithComboBox<String> eventComboBox;
	private final ProMComboBox<String> completionTimeColumnCbx;
	private final ProMComboBox<String> startTimeColumnCbx;

	private final ICSVReader reader;
	private final CSVPreviewFrame previewFrame;
	private int maxLoad = 1000;

	public ConversionConfigUI(final CSVFile csv, final CSVConfig importConfig, CSVConversionConfig conversionConfig)
			throws IOException {
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		setMaximumSize(new Dimension(COLUMN_WIDTH * 2, Short.MAX_VALUE));
		reader = csv.createReader(importConfig);
		headers = reader.readNext();
		headersInclEmpty = Lists.asList("", headers).toArray(new String[headers.length + 1]);
		this.conversionConfig = conversionConfig;
		final ChangeListenerImpl changeListener = new ChangeListenerImpl();

		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		;

		JLabel standardAttributesLabel = SlickerFactory.instance().createLabel(
				"<HTML><H2>Mapping to Standard XES Attributes</H2></HTML>");
		JButton showPreviewButton = new SlickerButton("Toggle Preview");
		showPreviewButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				togglePreviewFrame();
			}
		});

		caseComboBox = new ProMListSortableWithComboBox<>(new DefaultComboBoxModel<>(headers));
		JLabel caseLabel = createLabel(
				"Case Column (Optional)",
				"Groups events into traces, and is mapped to 'concept:name' of the trace. Select one or more columns, re-order by drag & drop.");
		caseComboBox.getListModel().addListDataListener(new ListDataListener() {

			public void intervalRemoved(ListDataEvent e) {
				changeListener.updateSettings();
			}

			public void intervalAdded(ListDataEvent e) {
				changeListener.updateSettings();
			}

			public void contentsChanged(ListDataEvent e) {
				changeListener.updateSettings();
			}
		});

		eventComboBox = new ProMListSortableWithComboBox<>(new DefaultComboBoxModel<>(headers));
		JLabel eventLabel = createLabel("Event Column (Optional)",
				"Mapped to 'concept:name' of the event. Select one or more columns, re-order by drag & drop.");
		eventComboBox.getListModel().addListDataListener(new ListDataListener() {

			public void intervalRemoved(ListDataEvent e) {
				changeListener.updateSettings();
			}

			public void intervalAdded(ListDataEvent e) {
				changeListener.updateSettings();
			}

			public void contentsChanged(ListDataEvent e) {
				changeListener.updateSettings();
			}
		});

		completionTimeColumnCbx = new ProMComboBox<>(headersInclEmpty);
		JLabel completionTimeLabel = createLabel("Completion Time (Optional)", "Mapped to 'time:timestamp'");
		completionTimeColumnCbx.addActionListener(changeListener);

		startTimeColumnCbx = new ProMComboBox<>(headersInclEmpty);
		JLabel startTimeLabel = createLabel("Start Time (Optional)",
				"Mapped to 'time:timestamp' of a separate start event");
		startTimeColumnCbx.addActionListener(changeListener);

		SequentialGroup verticalGroup = layout.createSequentialGroup();
		verticalGroup.addGroup(layout.createParallelGroup(Alignment.CENTER).addComponent(standardAttributesLabel)
				.addComponent(showPreviewButton));
		verticalGroup.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup().addComponent(caseLabel).addComponent(caseComboBox))
				.addGroup(layout.createSequentialGroup().addComponent(eventLabel).addComponent(eventComboBox)));
		verticalGroup
				.addGroup(layout
						.createParallelGroup()
						.addGroup(
								layout.createSequentialGroup().addComponent(completionTimeLabel)
										.addComponent(completionTimeColumnCbx))
						.addGroup(
								layout.createSequentialGroup().addComponent(startTimeLabel)
										.addComponent(startTimeColumnCbx)));

		ParallelGroup horizontalGroup = layout.createParallelGroup();
		horizontalGroup.addGroup(layout.createSequentialGroup().addComponent(standardAttributesLabel)
				.addComponent(showPreviewButton));
		horizontalGroup.addGroup(layout
				.createSequentialGroup()
				.addGroup(
						layout.createParallelGroup().addComponent(caseLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)
								.addComponent(caseComboBox, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH))
				.addGroup(
						layout.createParallelGroup().addComponent(eventLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)
								.addComponent(eventComboBox, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)));
		horizontalGroup.addGroup(layout
				.createSequentialGroup()
				.addGroup(
						layout.createParallelGroup()
								.addComponent(completionTimeLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)
								.addComponent(completionTimeColumnCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH))
				.addGroup(
						layout.createParallelGroup()
								.addComponent(startTimeLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)
								.addComponent(startTimeColumnCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)));

		layout.linkSize(eventLabel, caseLabel);
		layout.linkSize(completionTimeLabel, startTimeLabel);

		layout.setVerticalGroup(verticalGroup);
		layout.setHorizontalGroup(horizontalGroup);

		previewFrame = new CSVPreviewFrame(headers, conversionConfig);
		previewFrame.setTitle("CSV Preview & Conversion Configuration - Scroll down to load more rows");
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
		autoDetectDataTypes(csv, conversionConfig.getConversionMap(), importConfig);

		changeListener.updateSettings();
	}

	private void togglePreviewFrame() {
		if (!previewFrame.isVisible()) {
			previewFrame.showFrame(this);
			try {
				// Update Content
				new LoadCSVRecordsWorker().execute();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Error parsing CSV " + e.getMessage(), "CSV Parsing Error",
						JOptionPane.ERROR_MESSAGE);
			}
		} else {
			previewFrame.setVisible(false);
		}
	}

	private void autoDetectCaseColumn() {
		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if (CASE_COLUMN_IDS.contains(header.toLowerCase(Locale.US).trim())) {
				caseComboBox.addElement(header);
				return;
			}
		}
	}

	private void autoDetectEventColumn() {
		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if (EVENT_COLUMN_IDS.contains(header.toLowerCase(Locale.US).trim())) {
				eventComboBox.addElement(header);
				return;
			}
		}
	}

	private void autoDetectCompletionTimeColumn() {
		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if (COMPLETION_TIME_COLUMN_IDS.contains(header.toLowerCase(Locale.US).trim())) {
				completionTimeColumnCbx.setSelectedItem(header);
				return;
			}
		}
	}

	private void autoDetectDataTypes(CSVFile csv, Map<String, CSVMapping> conversionMap, CSVConfig csvConfig) throws IOException {
		try (ICSVReader reader = csv.createReader(csvConfig)) {
			String[] header = reader.readNext();
			Map<String, List<String>> valuesPerColumn = new HashMap<>();
			for (String h : header) {
				valuesPerColumn.put(h, new ArrayList<String>());
			}
			// now read 10 lines or so to guess the data type
			for (int i = 0; i < DATA_TYPE_FORMAT_AUTO_DETECT_NUM_LINES; i++) {
				String[] cells = reader.readNext();
				if (cells == null) {
					break;
				}
				for (int j = 0; j < cells.length; j++) {
					List<String> values = valuesPerColumn.get(header[j]);
					values.add(cells[j]);
					valuesPerColumn.put(header[j], values);
				}
			}
			// now we can guess the data type
			for (String h : header) {
				List<String> values = valuesPerColumn.get(h);
				// now we can guess the type
				// let's try the discrete values
				
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#addNotify()
	 */
	@Override
	public void addNotify() {
		super.addNotify();
		togglePreviewFrame();
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
