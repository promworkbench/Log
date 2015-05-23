package org.processmining.log.csvimport.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

import org.deckfour.xes.extension.XExtension;
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
			} else if (value instanceof String) {
				editor = new DefaultCellEditor(new JTextField());
			} else if (value instanceof XExtension || value == null) {
				editor = new DefaultCellEditor(new JComboBox<>(CSVMapping.AVAILABLE_EXTENSIONS));
			} else {
				throw new RuntimeException("Unkown value type " + value.getClass().getSimpleName());
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
			return 5;
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			String columnHeader = header[columnIndex];
			CSVMapping csvMapping = conversionConfig.getConversionMap().get(columnHeader);
			switch (rowIndex) {
				case 0 :
					Datatype newType = (Datatype) aValue;
					if (csvMapping.getDataType() != newType) {
						csvMapping.setPattern("");
						fireTableCellUpdated(1, columnIndex);
					}
					csvMapping.setDataType(newType);
					break;
				case 1 :
					csvMapping.setPattern((String) aValue);
					break;
				case 2 :
					if (aValue != null) {
						csvMapping.setExtension((XExtension) aValue);
					} else {
						csvMapping.setExtension(null);
					}
					break;
				case 3 :
					throw new IllegalStateException("Should not be able to edit this column!");
				case 4 :
					throw new IllegalStateException("Should not be able to edit this column!");
				default :
					throw new IllegalStateException("Could not find value at row " + rowIndex + " column "
							+ columnIndex);
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
					return csvMapping.getPattern();
				case 2 :
					return csvMapping.getExtension();
				case 3 :
					return csvMapping.getTraceAttributeName();
				case 4 :
					return csvMapping.getEventAttributeName();
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
			return rowIndex < 3 ? true : false;
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
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

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

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

		if (conversionConfig != null) {
			TableModel dataModel = new DataTypeTableModel(conversionConfig, header);
			datatypeTable = new JTable(dataModel);
			datatypeTable.setTableHeader(null);
			datatypeTable.setDefaultEditor(Object.class, new MappingCellEditor());
			datatypeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			datatypeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			ProMScrollPane dataTypeScrollpane = new ProMScrollPane(datatypeTable) {

				public Dimension getPreferredSize() {
					Dimension preferredSize = super.getPreferredSize();
					preferredSize.height = datatypeTable.getPreferredSize().height;
					preferredSize.width = Short.MAX_VALUE;
					return preferredSize;
				}

				public Dimension getMaximumSize() {
					return getPreferredSize();
				}
				
			};
			dataTypeScrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			getMainScrollPane().setHorizontalScrollBar(dataTypeScrollpane.getHorizontalScrollBar());

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

			JPanel leftPanel = new JPanel();
			leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
			leftPanel.add(new JLabel("Data Type"));
			leftPanel.add(new JLabel("Data Pattern"));
			leftPanel.add(new JLabel("XES Extension"));
			leftPanel.add(new JLabel("Trace Attribute"));
			leftPanel.add(new JLabel("Event Attribute"));
			leftPanel.add(Box.createVerticalGlue());
			mainPanel.add(leftPanel);

			rightPanel.add(dataTypeScrollpane);
		}


		rightPanel.add(mainScrollPane);
		mainPanel.add(rightPanel);
		getContentPane().add(mainPanel);

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
