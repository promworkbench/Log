package org.processmining.log.csvimport.ui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
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
import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
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

	public interface SeparatorListener {
		public void separatorDetected(CSVSeperator separator);
	}

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
		header.setFont(header.getFont().deriveFont(Font.BOLD, 18));
		header.setAlignmentY(LEFT_ALIGNMENT);
		add(header);

		add(Box.createVerticalStrut(20));

		charsetCbx = new ProMComboBox<>(Charset.availableCharsets().keySet());
		charsetCbx.setSelectedItem(importConfig.getCharset());
		charsetCbx.setPreferredSize(null);
		charsetCbx.setMinimumSize(null);
		JLabel charsetLabel = createLabel("Charset", "");
		charsetLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(charsetLabel);
		add(charsetCbx);
		charsetCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.setCharset(charsetCbx.getSelectedItem().toString());
				refreshPreview();
			}
		});
		charsetCbx.setAlignmentX(LEFT_ALIGNMENT);

		autoDetectCharset(csv, new CharsetListener() {

			public void report(String charset) {
				if (charset != null) {
					charsetCbx.setSelectedItem(charset);	
				}
			}
		});

		separatorField = new ProMComboBox<>(CSVSeperator.values());
		separatorField.setPreferredSize(null);
		separatorField.setMinimumSize(null);
		separatorField.setSelectedItem(importConfig.getSeparator());
		JLabel seperationLabel = createLabel("Separator Character", "");
		seperationLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(seperationLabel);
		add(separatorField);
		separatorField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.setSeparator(((CSVSeperator) separatorField.getSelectedItem()));
				refreshPreview();
			}
		});
		separatorField.setAlignmentX(LEFT_ALIGNMENT);
		
		try (BufferedReader reader = new BufferedReader(new FileReader(csv.getFile().toFile()))) {
			separatorField.setSelectedItem(autoDetectSeparator(reader.readLine()));
		} catch (IOException e1) {
			ProMUIHelper.showErrorMessage(this, e1.toString(), "Error reading CSV while determining separator character");
		}

		quoteField = new ProMComboBox<>(CSVQuoteCharacter.values());
		quoteField.setPreferredSize(null);
		quoteField.setMinimumSize(null);
		JLabel quoteLabel = createLabel("Quote Character", "");
		quoteLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(quoteLabel);
		add(quoteField);
		quoteField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				importConfig.setQuoteChar((CSVQuoteCharacter) quoteField.getSelectedItem());
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
	
	private static JLabel createLabel(String caption, String description) {
		JLabel eventLabel = SlickerFactory.instance().createLabel(
				"<HTML><B>" + caption + "</B><BR><I>" + description + "</I></HTML>");
		eventLabel.setAlignmentX(LEFT_ALIGNMENT);
		eventLabel.setFont(eventLabel.getFont().deriveFont(Font.PLAIN));
		return eventLabel;
	}

	private void autoDetectCharset(final CSVFile file, final CharsetListener listener) {

		final UniversalDetector detector = new UniversalDetector(null);

		SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {

			protected Void doInBackground() throws Exception {

				try (FileInputStream fis = new FileInputStream(file.getFile().toFile())) {
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
	
	private CSVSeperator autoDetectSeparator(final String headerRow) {
		if (headerRow.contains("\t")) {
			return CSVSeperator.TAB;
		} else if (headerRow.contains(";")) {
			return CSVSeperator.SEMICOLON;
		} else {
			return CSVSeperator.COMMA;	
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
