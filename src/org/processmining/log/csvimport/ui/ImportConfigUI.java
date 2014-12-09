package org.processmining.log.csvimport.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.mozilla.universalchardet.CharsetListener;
import org.mozilla.universalchardet.UniversalDetector;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.log.csvimport.CSVConversion.ImportConfig;
import org.processmining.log.csvimport.CSVUtils;
import org.processmining.log.csvimport.CSVFile;
import org.processmining.log.csvimport.SeperatorChar;

import au.com.bytecode.opencsv.CSVReader;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * UI for the import configuration (charset, separator, ..) 
 * 
 * @author F. Mannhardt
 *
 */
public final class ImportConfigUI extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int MAX_PREVIEW = 5000;

	private final CSVFile csv;
	private final ImportConfig importConfig;

	private final ProMComboBox<XFactory> xFactoryChoice;
	private final ProMComboBox<String> charsetCbx;
	private final ProMComboBox<SeperatorChar> separatorField;
	private final ProMTextField quoteField;

	private CSVPreviewFrame previewFrame;

	public ImportConfigUI(final CSVFile csv) {
		super();
		this.csv = csv;
		this.importConfig = new ImportConfig();
		this.previewFrame = new CSVPreviewFrame();
		this.previewFrame.showFrame(getRootPane());
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JLabel header = new JLabel("Please choose parameters regarding the format of the CSV file and OpenXES.");
		header.setAlignmentY(LEFT_ALIGNMENT);
		add(header);

		add(Box.createVerticalStrut(20));

		xFactoryChoice = new ProMComboBox<>(getAvailableXFactories());
		xFactoryChoice.setSelectedItem(importConfig.factory);
		xFactoryChoice.setPreferredSize(null);
		xFactoryChoice.setMinimumSize(null);
		JLabel xFactoryLabel = SlickerFactory.instance().createLabel(
				"Which XFactory implementation should be used to create the Log?");
		xFactoryLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(xFactoryLabel);
		add(xFactoryChoice);
		xFactoryChoice.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.factory = (XFactory) xFactoryChoice.getSelectedItem();
			}
		});
		xFactoryChoice.setAlignmentX(LEFT_ALIGNMENT);

		charsetCbx = new ProMComboBox<>(Charset.availableCharsets().keySet());
		charsetCbx.setSelectedItem(importConfig.charset);
		charsetCbx.setPreferredSize(null);
		charsetCbx.setMinimumSize(null);
		JLabel charsetLabel = SlickerFactory.instance().createLabel("Charset of the CSV");
		charsetLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(charsetLabel);
		add(charsetCbx);
		charsetCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.charset = charsetCbx.getSelectedItem().toString();
				refreshPreview();
			}
		});
		charsetCbx.setAlignmentX(LEFT_ALIGNMENT);

		autoDetectCharset(new CharsetListener() {

			public void report(String charset) {
				if (charset != null) {
					charsetCbx.setSelectedItem(charset);	
				}
			}
		});

		separatorField = new ProMComboBox<>(SeperatorChar.values());
		separatorField.setPreferredSize(null);
		separatorField.setMinimumSize(null);
		separatorField.setSelectedItem(importConfig.separator);
		JLabel seperationLabel = SlickerFactory.instance().createLabel("Separator Character of the CSV");
		seperationLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(seperationLabel);
		add(separatorField);
		separatorField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.separator = ((SeperatorChar) separatorField.getSelectedItem());
				refreshPreview();
			}
		});
		separatorField.setAlignmentX(LEFT_ALIGNMENT);

		quoteField = new ProMTextField("\"");
		quoteField.setPreferredSize(null);
		quoteField.setMinimumSize(null);
		quoteField.setText(String.valueOf(importConfig.quoteChar));
		JLabel quoteLabel = SlickerFactory.instance().createLabel("Quote Character of the CSV");
		quoteLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(quoteLabel);
		add(quoteField);
		quoteField.getDocument().addDocumentListener(new DocumentListener() {

			public void removeUpdate(DocumentEvent e) {
				update();
			}

			public void insertUpdate(DocumentEvent e) {
				update();
			}

			public void changedUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				if (quoteField.getText().length() == 1) {
					importConfig.quoteChar = quoteField.getText().charAt(0);
					refreshPreview();
				} else {
					importConfig.quoteChar = Character.UNASSIGNED;
					refreshPreview();
				}
			}

		});
		quoteField.setAlignmentX(LEFT_ALIGNMENT);

		refreshPreview();
	}

	private void autoDetectCharset(final CharsetListener listener) {

		final UniversalDetector detector = new UniversalDetector(null);

		SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {

			protected Void doInBackground() throws Exception {

				try (FileInputStream fis = new FileInputStream(csv.getFile().toFile())) {
					byte[] buf = new byte[4096];
					int nread;
					while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
						detector.handleData(buf, 0, nread);
					}
					detector.dataEnd();
				}

				return null;
			}

			protected void done() {
				if (!isCancelled()) {
					listener.report(detector.getDetectedCharset());
				}
			}

		};

		try {
			worker.execute();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error detectic charset of CSV " + e.getMessage(), "CSV Reading Error",
					JOptionPane.ERROR_MESSAGE);
		}

	}

	private Set<XFactory> getAvailableXFactories() {
		//Try to register XESLite Factories
		tryRegisterFactory("org.progressmining.xeslite.lite.factory.XFactoryLiteImpl");
		tryRegisterFactory("org.progressmining.xeslite.external.XFactoryExternalImpl$MapDBDiskImpl");
		tryRegisterFactory("org.progressmining.xeslite.external.XFactoryExternalImpl$MapDBDiskLowMemoryImpl");
		tryRegisterFactory("org.progressmining.xeslite.external.XFactoryExternalImpl$MapDBMemoryImpl");
		return XFactoryRegistry.instance().getAvailable();
	}

	private void tryRegisterFactory(String className) {
		try {
			getClass().getClassLoader().loadClass(className).getDeclaredMethod("register").invoke(null);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#removeNotify()
	 */
	public void removeNotify() {
		super.removeNotify();
		previewFrame.setVisible(false);
	}

	private void refreshPreview() {
		
		previewFrame.clear();

		// Update Header
		try (CSVReader reader = CSVUtils.createCSVReader(CSVUtils.getCSVInputStream(csv), importConfig)) {
			String[] header = reader.readNext();
			previewFrame.setHeader(header);
		} catch (IOException | UnsupportedOperationException e) {
			JOptionPane.showMessageDialog(this, "Error parsing CSV " + e.getMessage(), "CSV Parsing Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Update Content
		SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {

			protected Void doInBackground() throws Exception {

				try (CSVReader reader = CSVUtils.createCSVReader(CSVUtils.getCSVInputStream(csv), importConfig)) {
					// Skip header
					reader.readNext();
					String[] nextLine;
					int i = 0;
					while ((nextLine = reader.readNext()) != null && i < MAX_PREVIEW) {
						publish(nextLine);
						i++;
					}
				}

				return null;
			}

			protected void process(List<Object[]> chunks) {
				for (Object[] row : chunks) {
					previewFrame.addRow(row);
				}
			}

		};

		try {
			worker.execute();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error parsing CSV " + e.getMessage(), "CSV Parsing Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public ImportConfig getImportConfig() {
		return importConfig;
	}

}
