package org.processmining.log.csvimport.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.framework.util.ui.widgets.ProMTableWithoutPanel;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVMapping;
import org.processmining.log.csvimport.config.CSVConversionConfig.Datatype;

/**
 * Frame showing a part of the CSV file.
 * 
 * @author F. Mannhardt
 *
 */
public final class CSVPreviewFrame extends JFrame {

	private final class MappingCellEditor extends AbstractCellEditor implements TableCellEditor {

		private static final long serialVersionUID = -8465152263165430372L;

		private TableCellEditor editor;

		public Object getCellEditorValue() {
			if (editor != null) {
				return editor.getCellEditorValue();
			}
			return null;
		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			if (value instanceof Datatype) {
				editor = new DefaultCellEditor(new JComboBox<>(new DefaultComboBoxModel<>(Datatype.values())));
			} else if (value instanceof Format) {
				editor = new DefaultCellEditor(new JTextField());
			}
			return editor.getTableCellEditorComponent(table, value, isSelected, row, column);
		}
	}

	private final class BatchUpdateDefaultTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		private BatchUpdateDefaultTableModel(Object[] columnNames, int rowCount) {
			super(columnNames, rowCount);
		}

		@SuppressWarnings("unchecked")
		public void addRows(List<Object[]> rowData) {
			int firstRow = dataVector.size();
			for (Object[] row : rowData) {
				dataVector.add(convertToVector(row));
			}
			int lastRow = dataVector.size() - 1;
			fireTableRowsInserted(firstRow, lastRow);
		}
	}

	public static class DataTypeTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private final CSVConversionConfig conversionConfig;
		private final String[] header;

		public DataTypeTableModel(CSVConversionConfig conversionConfig, String[] header) {
			this.conversionConfig = conversionConfig;
			this.header = header;
		}

		public int getRowCount() {
			return 2;
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			String columnHeader = header[columnIndex];
			CSVMapping csvMapping = conversionConfig.getConversionMap().get(columnHeader);
			switch (rowIndex) {
				case 0 :
					csvMapping.setDataType((Datatype) aValue);
					break;
				case 1 :
					switch (csvMapping.getDataType()) {
						case BOOLEAN :
							csvMapping.setFormat(new MessageFormat((String) aValue));
							break;
						case CONTINUOUS :
							csvMapping.setFormat(new DecimalFormat((String) aValue));
							break;
						case DISCRETE :
							DecimalFormat integerFormat = new DecimalFormat((String) aValue);
							integerFormat.setMaximumFractionDigits(0);
							integerFormat.setDecimalSeparatorAlwaysShown(false);
							integerFormat.setParseIntegerOnly(true);
							csvMapping.setFormat(integerFormat);
							break;
						case LITERAL :
							csvMapping.setFormat(new MessageFormat((String) aValue));
							break;
						case TIME :
							csvMapping.setFormat(new SimpleDateFormat((String) aValue));
							break;
					}
					break;
			}
			conversionConfig.getConversionMap().put(columnHeader, csvMapping);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			String columnHeader = header[columnIndex];
			CSVMapping csvMapping = conversionConfig.getConversionMap().get(columnHeader);
			switch (rowIndex) {
				case 0 :
					return csvMapping.getDataType();
				case 1 :
					switch (csvMapping.getDataType()) {
						case BOOLEAN :
							return ((MessageFormat) csvMapping.getFormat()).toPattern();
						case CONTINUOUS :
							return ((DecimalFormat) csvMapping.getFormat()).toPattern();
						case DISCRETE :
							return ((DecimalFormat) csvMapping.getFormat()).toPattern();
						case LITERAL :
							return ((MessageFormat) csvMapping.getFormat()).toPattern();
						case TIME :
							return ((SimpleDateFormat) csvMapping.getFormat()).toPattern();
					}
			}
			throw new IllegalStateException("Could not find value at row " + rowIndex + " column " + columnIndex);
		}

		public int getColumnCount() {
			return header.length;
		}

		public String getColumnName(int column) {
			return header[column];
		}

		public Class<?> getColumnClass(int columnIndex) {
			return super.getColumnClass(columnIndex);
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

	}

	private static final long serialVersionUID = 1L;

	private final BatchUpdateDefaultTableModel previewTableModel;
	private final JTable previewTable;
	private final JScrollPane mainScrollPane;

	private JTable datatypeTable;

	public CSVPreviewFrame() {
		this(null);
	}

	public CSVPreviewFrame(String[] header) {
		this(null, null);
	}

	@SuppressWarnings("serial")
	public CSVPreviewFrame(String[] header, CSVConversionConfig conversionConfig) {
		super();
		setTitle("CSV Import: Preview of the Import");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		setPreferredSize(new Dimension(600, 200));

		previewTableModel = new BatchUpdateDefaultTableModel(header, 0);
		previewTable = new ProMTableWithoutPanel(previewTableModel);
		previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		Enumeration<TableColumn> columns = previewTable.getColumnModel().getColumns();
		while (columns.hasMoreElements()) {
			final TableColumn column = columns.nextElement();
			column.setPreferredWidth(130);
			column.setCellEditor(new DefaultCellEditor(new JTextField()) {

				protected void fireEditingStopped() {
					this.cancelCellEditing();
					super.fireEditingStopped();
				}

				protected void fireEditingCanceled() {
					super.fireEditingCanceled();
				}

			});
		}
		previewTable.getColumnModel().setColumnSelectionAllowed(false);
		previewTable.getTableHeader().setReorderingAllowed(false);

		mainScrollPane = new ProMScrollPane(previewTable);
		getMainScrollPane().setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		if (conversionConfig != null) {
			TableModel dataModel = new DataTypeTableModel(conversionConfig, header);
			datatypeTable = new JTable(dataModel);
			datatypeTable.setTableHeader(null);
			datatypeTable.setDefaultEditor(Object.class, new MappingCellEditor());

			datatypeTable.setDefaultEditor(Format.class, new DefaultCellEditor(new JTextField()));
			datatypeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			datatypeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane dataTypeScrollpane = new JScrollPane(datatypeTable);
			dataTypeScrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			add(dataTypeScrollpane);

			previewTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {

				public void columnSelectionChanged(ListSelectionEvent e) {
				}

				public void columnRemoved(TableColumnModelEvent e) {
				}

				public void columnMoved(TableColumnModelEvent e) {
				}

				public void columnMarginChanged(ChangeEvent e) {
					final TableColumnModel tableColumnModel = previewTable.getColumnModel();
					TableColumnModel dataTypeColumnModel = datatypeTable.getColumnModel();
					for (int i = 0; i < tableColumnModel.getColumnCount(); i++) {
						int w = tableColumnModel.getColumn(i).getWidth();
						dataTypeColumnModel.getColumn(i).setMinWidth(w);
						dataTypeColumnModel.getColumn(i).setMaxWidth(w);
					}
					datatypeTable.doLayout();
					datatypeTable.repaint();
					repaint();
				}

				public void columnAdded(TableColumnModelEvent e) {
				}

			});

			getMainScrollPane().setHorizontalScrollBar(dataTypeScrollpane.getHorizontalScrollBar());
		}

		add(getMainScrollPane());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
	}

	public void showFrame(JComponent parent) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		if (gs.length > 1) {
			for (int i = 0; i < gs.length; i++) {
				if (gs[i] != parent.getGraphicsConfiguration().getDevice()) {
					JFrame dummy = new JFrame(gs[i].getDefaultConfiguration());
					setLocationRelativeTo(dummy);
					setExtendedState(Frame.MAXIMIZED_BOTH);
					setAlwaysOnTop(true);
					dummy.dispose();
					break;
				}
			}
		} else {
			setLocationRelativeTo(parent);
			setAlwaysOnTop(true);
			setPreferredSize(new Dimension(800, 800));
		}
		setVisible(true);
	}

	public void addRow(Object[] data) {
		previewTableModel.addRow(data);
	}

	public void addRows(List<Object[]> rows) {
		previewTableModel.addRows(rows);
	}

	public void setHeader(Object[] header) {
		if (header == null) {
			previewTable.setTableHeader(null);
		} else {
			previewTableModel.setColumnIdentifiers(header);
		}
	}

	public void clear() {
		previewTableModel.getDataVector().clear();
		previewTable.repaint();
	}

	public void refresh() {
		if (datatypeTable != null) {
			datatypeTable.repaint();
		}
	}

	public JTable getPreviewTable() {
		return previewTable;
	}

	public JScrollPane getMainScrollPane() {
		return mainScrollPane;
	}

}
