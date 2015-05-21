package org.processmining.log.csvimport.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMListSortableWithComboBox;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVEmptyCellHandlingMode;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVErrorHandlingMode;
import org.processmining.log.csvimport.config.CSVImportConfig;

import au.com.bytecode.opencsv.CSVReader;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * UI for the configuration of the actual conversion
 * 
 * @author F. Mannhardt
 *
 */
public final class ConversionConfigUI extends JPanel implements AutoCloseable {

	private static final int COLUMN_WIDTH = 360;
	
	private static final class XFactoryUI {
		
		private final XFactory factory;

		public XFactoryUI(XFactory factory) {
			super();
			this.factory = factory;
		}

		public XFactory getFactory() {
			return factory;
		}
		
		@Override
		public String toString() {
			return factory.getName();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((factory == null) ? 0 : factory.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof XFactoryUI))
				return false;
			XFactoryUI other = (XFactoryUI) obj;
			if (factory == null) {
				if (other.factory != null)
					return false;
			} else if (!factory.equals(other.factory))
				return false;
			return true;
		}

	}

	private final class ChangeListenerImpl implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			updateSettings();
		}

		public void updateSettings() {
			conversionConfig.getConversionMap().remove(
					findColumnIndex(headers, conversionConfig.getCompletionTimeColumn()));
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

	private final ProMComboBox<XFactoryUI> xFactoryChoice;

	private final ProMListSortableWithComboBox<String> caseComboBox;
	private final ProMListSortableWithComboBox<String> eventComboBox;
	private final ProMComboBox<String> completionTimeColumnCbx;
	private final ProMComboBox<String> startTimeColumnCbx;

	private final ProMComboBox<Boolean> repairDataTypesCbx;
	private final ProMComboBox<CSVEmptyCellHandlingMode> emptyCellHandlingModeCbx;
	private final ProMComboBox<CSVErrorHandlingMode> errorHandlingModeCbx;

	private final CSVReader reader;
	private final CSVPreviewFrame previewFrame;
	private int maxLoad = 5000;
		

	public ConversionConfigUI(final CSVFile csv, final CSVImportConfig importConfig) throws IOException {		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);		
		setMaximumSize(new Dimension(COLUMN_WIDTH * 2, Short.MAX_VALUE));
		reader = csv.createReader(importConfig);
		headers = reader.readNext();
		headersInclEmpty = Lists.asList("", headers).toArray(new String[headers.length + 1]);
		conversionConfig = new CSVConversionConfig(headers);
		final ChangeListenerImpl changeListener = new ChangeListenerImpl();
		
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);;

		JLabel standardAttributesLabel = SlickerFactory.instance().createLabel("Mapping to Standard XES Attributes");
		standardAttributesLabel.setFont(standardAttributesLabel.getFont().deriveFont(Font.BOLD, 18));

		caseComboBox = new ProMListSortableWithComboBox<>(new DefaultComboBoxModel<>(headers));
		JLabel caseLabel = createLabel("Case Column (Optional)", "Groups events into traces, and is mapped to 'concept:name' of the trace. Select one or more columns by choosing from the box below, re-order by drag & drop and remove with the 'DELETE' key.");
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
		JLabel eventLabel = createLabel("Event Column (Optional)", "Mapped to 'concept:name'. Select one or more columns by choosing from the box below, re-order by drag & drop and remove with the 'DELETE' key.");
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
		JLabel startTimeLabel = createLabel("Start Time (Optional)", "Mapped to 'time:timestamp' of a separate start event");
		startTimeColumnCbx.addActionListener(changeListener);


		JLabel conversionOptionsLabel = SlickerFactory.instance().createLabel(
				"Expert Conversion Options (Defaults are a good guess)");
		conversionOptionsLabel.setFont(conversionOptionsLabel.getFont().deriveFont(Font.BOLD, 20));
		
		xFactoryChoice = new ProMComboBox<>(Iterables.transform(getAvailableXFactories(), new Function<XFactory, XFactoryUI>() {

			public XFactoryUI apply(XFactory factory) {
				return new XFactoryUI(factory);
			}
			
		}));
		xFactoryChoice.setSelectedItem(new XFactoryUI(conversionConfig.getFactory()));
		JLabel xFactoryLabel = createLabel("XFactory",
				"Implementation that is be used to create the Log.");
		
		xFactoryChoice.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.setFactory(((XFactoryUI) xFactoryChoice.getSelectedItem()).getFactory());
			}
		});

		errorHandlingModeCbx = new ProMComboBox<>(CSVErrorHandlingMode.values());
		errorHandlingModeCbx.setSelectedItem(conversionConfig.getErrorHandlingMode());
		JLabel errorHandlingModeLabel = createLabel("Error Handling",
				"Stop conversion upon malformed input or try to import as much as possible?");
		errorHandlingModeCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.setErrorHandlingMode((CSVErrorHandlingMode) errorHandlingModeCbx.getSelectedItem());
			}
		});

		emptyCellHandlingModeCbx = new ProMComboBox<>(CSVEmptyCellHandlingMode.values());
		emptyCellHandlingModeCbx.setSelectedItem(conversionConfig.getEmptyCellHandlingMode());
		JLabel emptyCellHandlingModeLabel = createLabel("Empty Cells", "Exclude or include empty cells in the conversion.");
		emptyCellHandlingModeCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.setEmptyCellHandlingMode((CSVEmptyCellHandlingMode) emptyCellHandlingModeCbx
						.getSelectedItem());
			}
		});
		
		repairDataTypesCbx = new ProMComboBox<>(new Boolean[] { true, false });
		repairDataTypesCbx.setSelectedItem(conversionConfig.isShouldGuessDataTypes());
		JLabel repairDataTypesLabel = createLabel("Guess Attribute Types",
						"Should the plug-in make an attempt to guess the correct datatypes after conversion? Leave as 'true' in case you are unsure!");
		repairDataTypesCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.setShouldGuessDataTypes((Boolean) repairDataTypesCbx.getSelectedItem());
			}
		});

		
		SequentialGroup verticalGroup = layout.createSequentialGroup();
		verticalGroup.addComponent(standardAttributesLabel);
		verticalGroup.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup().addComponent(caseLabel).addComponent(caseComboBox))
				.addGroup(layout.createSequentialGroup().addComponent(eventLabel).addComponent(eventComboBox)));
		verticalGroup.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup().addComponent(completionTimeLabel).addComponent(completionTimeColumnCbx))
				.addGroup(layout.createSequentialGroup().addComponent(startTimeLabel).addComponent(startTimeColumnCbx)));
		verticalGroup.addComponent(conversionOptionsLabel);
		verticalGroup.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup().addComponent(errorHandlingModeLabel).addComponent(errorHandlingModeCbx))
				.addGroup(layout.createSequentialGroup().addComponent(xFactoryLabel).addComponent(xFactoryChoice)));
		verticalGroup.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup().addComponent(emptyCellHandlingModeLabel).addComponent(emptyCellHandlingModeCbx))
				.addGroup(layout.createSequentialGroup().addComponent(repairDataTypesLabel).addComponent(repairDataTypesCbx)));
		
		ParallelGroup horizontalGroup = layout.createParallelGroup();
		horizontalGroup.addComponent(standardAttributesLabel);
		horizontalGroup.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup().addComponent(caseLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH).addComponent(caseComboBox, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH))
				.addGroup(layout.createParallelGroup().addComponent(eventLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH).addComponent(eventComboBox, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)));
		horizontalGroup.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup().addComponent(completionTimeLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH).addComponent(completionTimeColumnCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH))
				.addGroup(layout.createParallelGroup().addComponent(startTimeLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH).addComponent(startTimeColumnCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)));
		horizontalGroup.addComponent(conversionOptionsLabel);
		horizontalGroup.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup().addComponent(errorHandlingModeLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH).addComponent(errorHandlingModeCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH ))
				.addGroup(layout.createParallelGroup().addComponent(xFactoryLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH).addComponent(xFactoryChoice, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)));
		horizontalGroup.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup().addComponent(emptyCellHandlingModeLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH).addComponent(emptyCellHandlingModeCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH))
				.addGroup(layout.createParallelGroup().addComponent(repairDataTypesLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH).addComponent(repairDataTypesCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)));
		
		layout.linkSize(eventLabel, caseLabel);
		layout.linkSize(completionTimeLabel, startTimeLabel);
		layout.linkSize(errorHandlingModeLabel, xFactoryLabel);
		layout.linkSize(emptyCellHandlingModeLabel, repairDataTypesLabel);

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
		changeListener.updateSettings();
	}

	private static JLabel createLabel(String caption, String description) {
		JLabel eventLabel = SlickerFactory.instance().createLabel(
				"<HTML><B>" + caption + "</B><BR><I>" + description + "</I></HTML>");
		eventLabel.setAlignmentX(LEFT_ALIGNMENT);
		eventLabel.setFont(eventLabel.getFont().deriveFont(Font.PLAIN));
		return eventLabel;
	}

	private Set<XFactory> getAvailableXFactories() {
		//Try to register XESLite Factories
		tryRegisterFactory("org.processmining.xeslite.lite.factory.XFactoryLiteImpl");
		tryRegisterFactory("org.processmining.xeslite.external.XFactoryExternalStore$MapDBDiskImpl");
		tryRegisterFactory("org.processmining.xeslite.external.XFactoryExternalStore$MapDBDiskWithoutCacheImpl");
		tryRegisterFactory("org.processmining.xeslite.external.XFactoryExternalStore$MapDBDiskSequentialAccessImpl");
		tryRegisterFactory("org.processmining.xeslite.external.XFactoryExternalStore$MapDBDiskSequentialAccessWithoutCacheImpl");
		return XFactoryRegistry.instance().getAvailable();
	}

	/**
	 * Tries to load the class and call the 'register' method.
	 * 
	 * @param className
	 */
	private void tryRegisterFactory(String className) {
		try {
			getClass().getClassLoader().loadClass(className).getDeclaredMethod("register").invoke(null);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
		}
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

			if ("case".equalsIgnoreCase(header) || "trace".equalsIgnoreCase(header)
					|| "traceid".equalsIgnoreCase(header) || "caseid".equalsIgnoreCase(header)) {
				caseComboBox.addElement(header);
				return;
			}

		}

	}

	private void autoDetectEventColumn() {

		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if ("event".equalsIgnoreCase(header) || "eventname".equalsIgnoreCase(header)
					|| "activity".equalsIgnoreCase(header) || "eventid".equalsIgnoreCase(header)
					|| "activityid".equalsIgnoreCase(header)) {
				eventComboBox.addElement(header);
				return;
			}
		}

	}

	private void autoDetectCompletionTimeColumn() {

		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if ("time".equalsIgnoreCase(header) || "tijd".equalsIgnoreCase(header)
					|| "timestamp".equalsIgnoreCase(header) || "endtime".equalsIgnoreCase(header)
					|| "completiontime".equalsIgnoreCase(header) || "date".equalsIgnoreCase(header)
					|| "datetime".equalsIgnoreCase(header) || "eventtime".equalsIgnoreCase(header)) {
				completionTimeColumnCbx.setSelectedItem(header);
				return;
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
