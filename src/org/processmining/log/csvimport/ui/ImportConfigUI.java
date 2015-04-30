package org.processmining.log.csvimport.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.mozilla.universalchardet.CharsetListener;
import org.mozilla.universalchardet.UniversalDetector;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csvimport.CSVQuoteCharacter;
import org.processmining.log.csvimport.CSVSeperator;
import org.processmining.log.csvimport.config.CSVImportConfig;

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
	private final CSVImportConfig importConfig;

	private final ProMComboBox<String> charsetCbx;
	private final ProMComboBox<CSVSeperator> separatorField;
	private final ProMComboBox<CSVQuoteCharacter> quoteField;
	//private final ProMComboBox<CSVQuoteCharacter> escapeField;

	private final CSVPreviewFrame previewFrame;

	public ImportConfigUI(final CSVFile csv) {
		super();
		this.csv = csv;
		this.importConfig = new CSVImportConfig();
		this.previewFrame = new CSVPreviewFrame();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JLabel header = new JLabel("Please choose parameters regarding the format of the CSV file and OpenXES.");
		header.setAlignmentY(LEFT_ALIGNMENT);
		add(header);

		add(Box.createVerticalStrut(20));

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

		separatorField = new ProMComboBox<>(CSVSeperator.values());
		separatorField.setPreferredSize(null);
		separatorField.setMinimumSize(null);
		separatorField.setSelectedItem(importConfig.separator);
		JLabel seperationLabel = SlickerFactory.instance().createLabel("Separator Character of the CSV");
		seperationLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(seperationLabel);
		add(separatorField);
		separatorField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.separator = ((CSVSeperator) separatorField.getSelectedItem());
				refreshPreview();
			}
		});
		separatorField.setAlignmentX(LEFT_ALIGNMENT);

		quoteField = new ProMComboBox<>(CSVQuoteCharacter.values());
		quoteField.setPreferredSize(null);
		quoteField.setMinimumSize(null);
		JLabel quoteLabel = SlickerFactory.instance().createLabel("Quote Character of the CSV");
		quoteLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(quoteLabel);
		add(quoteField);
		quoteField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.quoteChar = (CSVQuoteCharacter) quoteField.getSelectedItem();
				refreshPreview();
			}
		});	
		quoteField.setAlignmentX(LEFT_ALIGNMENT);
		
		/*
		escapeField = new ProMComboBox<>(CSVEscapeCharacter.values());
		escapeField.setPreferredSize(null);
		escapeField.setMinimumSize(null);
		JLabel escapeLabel = SlickerFactory.instance().createLabel("Escape Character of the CSV");
		escapeLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(escapeLabel);
		add(escapeField);
		escapeField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.escapeChar = (CSVEscapeCharacter) escapeField.getSelectedItem();
				refreshPreview();
			}
		});	
		quoteField.setAlignmentX(LEFT_ALIGNMENT);
		 */
		
		refreshPreview();
	}

	public void showPreviewFrame() {
		this.previewFrame.showFrame(getRootPane());
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

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#addNotify()
	 */
	@Override
	public void addNotify() {
		super.addNotify();
		previewFrame.showFrame(getRootPane());
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

	private void refreshPreview() {
		
		previewFrame.clear();

		// Update Header
		try {
			previewFrame.setHeader(csv.readHeader(importConfig));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error parsing CSV " + e.getMessage(), "CSV Parsing Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Update Content
		SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {

			protected Void doInBackground() throws Exception {

				try (CSVReader reader = csv.createReader(importConfig)) {
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

	public CSVImportConfig getImportConfig() {
		return importConfig;
	}

}
