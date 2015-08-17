package org.processmining.log.csvimport.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.ICSVReader;
import org.processmining.log.csv.config.CSVConfig;
import org.processmining.log.csv.config.CSVQuoteCharacter;
import org.processmining.log.csv.config.CSVSeperator;

import com.fluxicon.slickerbox.components.SlickerButton;

/**
 * UI for the import configuration (charset, separator, ..)
 * 
 * @author F. Mannhardt
 *
 */
public final class ImportConfigUI extends CSVConfigurationPanel {

	private static final long serialVersionUID = 2L;

	private static final int MAX_PREVIEW = 1000;

	private final CSVFile csv;
	private final CSVConfig importConfig;

	private final ProMComboBox<String> charsetCbx;
	private final ProMComboBox<CSVSeperator> separatorField;
	private final ProMComboBox<CSVQuoteCharacter> quoteField;
	//private final ProMComboBox<CSVQuoteCharacter> escapeField;

	private final CSVPreviewFrame previewFrame;

	private SwingWorker<Void, Object[]> worker;

	public ImportConfigUI(final CSVFile csv, final CSVConfig importConfig) {
		super();
		this.importConfig = importConfig;
		this.csv = csv;
		this.previewFrame = new CSVPreviewFrame();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JLabel header = new JLabel("<HTML><H2>CSV Parsing Parameters</H2></HTML>");
		header.setAlignmentX(LEFT_ALIGNMENT);
		header.setAlignmentY(TOP_ALIGNMENT);

		JButton showPreviewButton = new SlickerButton("Toggle Preview");
		showPreviewButton.setAlignmentY(TOP_ALIGNMENT);
		showPreviewButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				togglePreviewFrame();
			}
		});

		JPanel headerPanel = new JPanel();
		headerPanel.setOpaque(false);
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
		headerPanel.setAlignmentX(LEFT_ALIGNMENT);
		headerPanel.add(header);
		headerPanel.add(showPreviewButton);
		add(headerPanel);

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

		separatorField = new ProMComboBox<>(CSVSeperator.values());
		separatorField.setPreferredSize(null);
		separatorField.setMinimumSize(null);
		separatorField.setSelectedItem(importConfig.getSeparator());
		separatorField.setAlignmentX(LEFT_ALIGNMENT);
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

		quoteField = new ProMComboBox<>(CSVQuoteCharacter.values());
		quoteField.setPreferredSize(null);
		quoteField.setMinimumSize(null);
		quoteField.setAlignmentX(LEFT_ALIGNMENT);
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
		quoteField.setSelectedItem(importConfig.getQuoteChar());

		/*
		 * escapeField = new ProMComboBox<>(CSVEscapeCharacter.values());
		 * escapeField.setPreferredSize(null); escapeField.setMinimumSize(null);
		 * JLabel escapeLabel =
		 * SlickerFactory.instance().createLabel("Escape Character of the CSV");
		 * escapeLabel.setAlignmentX(LEFT_ALIGNMENT); add(escapeLabel);
		 * add(escapeField); escapeField.addActionListener(new ActionListener()
		 * {
		 * 
		 * public void actionPerformed(ActionEvent e) { importConfig.escapeChar
		 * = (CSVEscapeCharacter) escapeField.getSelectedItem();
		 * refreshPreview(); } }); escapeField.setAlignmentX(LEFT_ALIGNMENT);
		 */

		refreshPreview();
	}

	public void togglePreviewFrame() {
		if (previewFrame.isVisible()) {
			previewFrame.setVisible(false);
		} else {
			previewFrame.showFrame(getRootPane());
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

		if (worker != null) {
			worker.cancel(true);
		}

		previewFrame.clear();

		// Update Header
		try {
			previewFrame.setHeader(csv.readHeader(importConfig));
		} catch (IOException | ArrayIndexOutOfBoundsException e) {
			ProMUIHelper.showWarningMessage(this, "Error parsing CSV " + e.getMessage(), "CSV Parsing Error");
			return;
		}

		worker = new SwingWorker<Void, Object[]>() {

			protected Void doInBackground() throws Exception {

				try (ICSVReader reader = csv.createReader(importConfig)) {
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

	public CSVConfig getImportConfig() {
		return importConfig;
	}

}
