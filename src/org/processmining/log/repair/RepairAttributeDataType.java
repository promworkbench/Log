package org.processmining.log.repair;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;

import com.google.common.collect.Ordering;

/**
 * Tries to automatically guess the data type of all XES attributes and updates
 * the log accordingly.
 * <p>
 * PLEASE NOTE: This filter will update the original XLog instead of creating a
 * new XLog, to be able to process huge logs without exhausting the available
 * memory.
 * 
 * @author F. Mannhardt
 * 
 */
public final class RepairAttributeDataType {

	private static class ReviewTable {

		private Map<String, Class<? extends XAttribute>> attributeDataType;
		private final JTable datatypeTable;
		private final DefaultTableModel tableModel;

		@SuppressWarnings({ "unchecked", "serial" })
		public ReviewTable(final Map<String, Class<? extends XAttribute>> attributeDataType) {
			super();
			this.attributeDataType = attributeDataType;
			this.tableModel = new DefaultTableModel() {

				public void setValueAt(Object aValue, int row, int column) {
					super.setValueAt(aValue, row, column);
					attributeDataType.put(getColumnName(column), (Class<? extends XAttribute>) aValue);
				}

			};

			for (String attributeKey : Ordering.natural().immutableSortedCopy(attributeDataType.keySet())) {
				Class<? extends XAttribute> dataType = attributeDataType.get(attributeKey);
				tableModel.addColumn(attributeKey, new Class[] { dataType });
			}

			this.datatypeTable = new JTable(tableModel);
			JComboBox<Class<? extends XAttribute>> comboBox = new JComboBox<>(
					new DefaultComboBoxModel<Class<? extends XAttribute>>(new Class[] { XAttributeBoolean.class,
							XAttributeContinuous.class, XAttributeDiscrete.class, XAttributeLiteral.class,
							XAttributeTimestamp.class }));
			comboBox.setRenderer(new DefaultListCellRenderer() {

				@SuppressWarnings("rawtypes")
				public Component getListCellRendererComponent(JList<?> list, Object value, int index,
						boolean isSelected, boolean cellHasFocus) {
					JLabel superComponent = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
							cellHasFocus);
					superComponent.setText(((Class) value).getSimpleName());
					return superComponent;
				}

			});
			datatypeTable.setDefaultEditor(Object.class, new DefaultCellEditor(comboBox));
			datatypeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

				@SuppressWarnings("rawtypes")
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column) {
					JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
							column);
					c.setText(((Class) value).getSimpleName());
					return c;
				}

			});

			datatypeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			resizeColumnWidth(datatypeTable);
		}

		public Map<String, Class<? extends XAttribute>> getDataTypeMap() {
			return attributeDataType;
		}

		public JComponent getDatatypeTable() {
			JPanel workaroundPanel = new JPanel(new BorderLayout());
			ProMScrollPane scrollPane = new ProMScrollPane(datatypeTable);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			workaroundPanel.add(scrollPane, BorderLayout.CENTER);
			workaroundPanel.setPreferredSize(new Dimension(400, 200));
			return workaroundPanel;
		}

		public void resizeColumnWidth(JTable table) {
			final TableColumnModel columnModel = table.getColumnModel();
			for (int column = 0; column < table.getColumnCount(); column++) {
				int width = 150; // Min width
				for (int row = 0; row < table.getRowCount(); row++) {
					TableCellRenderer renderer = table.getCellRenderer(row, column);
					Component comp = table.prepareRenderer(renderer, row, column);
					width = Math.max(comp.getPreferredSize().width * 2, width);
				}
				columnModel.getColumn(column).setPreferredWidth(width);
			}
		}

	}
	
	public interface ReviewCallback {
		Map<String, Class<? extends XAttribute>> reviewDataTypes(Map<String, Class<? extends XAttribute>> guessedDataTypes);
	}

	public RepairAttributeDataType() {
		super();
	}
	
	public void doRepairEventAttributes(PluginContext context, XLog log, Set<DateFormat> dateFormats) {
		doRepairEventAttributes(context, log, dateFormats, null);
	}

	public void doRepairEventAttributes(PluginContext context, XLog log, Set<DateFormat> dateFormats, ReviewCallback reviewCallback) {
		
		Progress progBar = context.getProgress();
		progBar.setMinimum(0);
		progBar.setMaximum(log.size() * 2); // two pass
		progBar.setValue(0);

		Map<String, Class<? extends XAttribute>> guessedDataType = new HashMap<>();

		// Determine best datatype
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				buildDataTypeMap(event.getAttributes(), guessedDataType, dateFormats);
			}
			if (progBar.isCancelled()) {
				return;
			}
			progBar.inc();
		}
		
		if (reviewCallback != null) {
			guessedDataType = reviewCallback.reviewDataTypes(guessedDataType);
		}
		
		XFactory factory = XFactoryRegistry.instance().currentDefault();

		ListIterator<XTrace> traceIterator = log.listIterator();

		while (traceIterator.hasNext()) {

			XTrace trace = traceIterator.next();
			ListIterator<XEvent> eventIterator = trace.listIterator();
			
			while (eventIterator.hasNext()) {
				int eventIndex = eventIterator.nextIndex();
				XEvent event = eventIterator.next();
				XAttributeMap eventAttr = event.getAttributes();
				repairAttributes(context, factory, eventAttr, dateFormats, guessedDataType);
				trace.set(eventIndex, event);
			}
			if (progBar.isCancelled()) {
				return;
			}
			progBar.inc();
		}
		
	}
	
	public void doRepairTraceAttributes(PluginContext context, XLog log, Set<DateFormat> dateFormats) {
		doRepairTraceAttributes(context, log, dateFormats, null);
	}

	public void doRepairTraceAttributes(PluginContext context, XLog log, Set<DateFormat> dateFormats, ReviewCallback reviewCallback) {
		
		Progress progBar = context.getProgress();
		progBar.setMinimum(0);
		progBar.setMaximum(log.size() * 2); // two pass
		progBar.setValue(0);

		Map<String, Class<? extends XAttribute>> guessedDataType = new HashMap<>();

		// Determine best datatype
		for (XTrace trace : log) {
			buildDataTypeMap(trace.getAttributes(), guessedDataType, dateFormats);			
			if (progBar.isCancelled()) {
				return;
			}
			progBar.inc();
		}
		
		if (reviewCallback != null) {
			guessedDataType = reviewCallback.reviewDataTypes(guessedDataType);
		}
		
		XFactory factory = XFactoryRegistry.instance().currentDefault();

		ListIterator<XTrace> traceIterator = log.listIterator();

		while (traceIterator.hasNext()) {

			XTrace trace = traceIterator.next();
			XAttributeMap traceAttr = trace.getAttributes();
			repairAttributes(context, factory, traceAttr, dateFormats, guessedDataType);

			if (progBar.isCancelled()) {
				return;
			}
			progBar.inc();
		}	
		
	}
	
	/**
	 * Shows a wizard that allows the user to specify an additional custom date format.
	 * 
	 * @param context
	 * @return a set of DateFormats including the user specified format
	 */
	public static Set<DateFormat> queryDateFormats(UIPluginContext context) {
		
		 Set<DateFormat> dateFormats = new HashSet<DateFormat>() {

			 private static final long serialVersionUID = 1L;

				{
					add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
					add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
					add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
					add(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"));
					add(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"));
					add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
					add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
					add(new SimpleDateFormat("yyyy-MM-dd"));
				}
			};
		
		try {
			String dateFormat = ProMUIHelper.queryForString(context,
					"Specify a custom DateFormat pattern (Format as defined in Java SimpleDateFormat) that is used to parse literal attributes that contain dates (LEAVE BLANK OR CANCEL TO USE DEFAULTS)");
			if (dateFormat != null && !dateFormat.isEmpty()) {
				try {
					dateFormats.add(new SimpleDateFormat(dateFormat));
				} catch (IllegalArgumentException e) {
					JOptionPane.showMessageDialog(null, e.getMessage(), "Wrong Date Format",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		} catch (UserCancelledException e) {
		}
		
		return dateFormats;
	}

	/**
	 * 
	 * 
	 * @param context
	 * @param attributeDataType
	 * @return
	 */
	public static Map<String, Class<? extends XAttribute>> queryCustomDataTypes(UIPluginContext context, Map<String, Class<? extends XAttribute>> attributeDataType) {
		ReviewTable reviewPanel = new ReviewTable(attributeDataType);
		InteractionResult reviewResult = context.showConfiguration(
				"Review/Adjust the automatically determined data types", reviewPanel.getDatatypeTable());
		if (reviewResult == InteractionResult.FINISHED) {
			return reviewPanel.getDataTypeMap();
		}
		return attributeDataType;
	}

	private static void repairAttributes(PluginContext context, XFactory factory, XAttributeMap attrMap, Set<DateFormat> dateFormats, Map<String, Class<? extends XAttribute>> attributeDataType) {
		// Use entrySet here, to avoid a lot of 'put' operations, maybe the underlying map can optimize the replacement operation using 'entry.setValue'
		Iterator<Entry<String, XAttribute>> traceAttr = attrMap.entrySet().iterator();
		while (traceAttr.hasNext()) {
			Entry<String, XAttribute> entry = traceAttr.next();

			if (!isExtensionAttribute(entry.getValue())) {
				if (!(entry.getValue() instanceof XAttributeTimestamp)) {
					Class<? extends XAttribute> dataType = attributeDataType.get(entry.getKey());
					try {
						if (dataType != null) {
							entry.setValue(createAttribute(dataType, entry, factory, dateFormats));	
						} else {
							throw new RuntimeException(String.format("Could not find datatype of attribute %s. Available data types are %s", entry.getKey(), attributeDataType));
						}
					} catch (NumberFormatException e) {
						context.log("Could not convert value of attribute " + entry.getKey() + " to " + dataType,
								MessageLevel.ERROR);
						context.getFutureResult(0).cancel(true);
					}

				}
			}
		}
	}

	private static boolean isExtensionAttribute(XAttribute value) {
		return value.getExtension() != null;
	}

	private static void buildDataTypeMap(XAttributeMap attributes, Map<String, Class<? extends XAttribute>> attributeDataType, Set<DateFormat> dateFormats) {
		for (XAttribute attribute : attributes.values()) {

			if (!(attribute instanceof XAttributeTimestamp)) {

				String value = getAttrAsString(attribute);
				Class<? extends XAttribute> currentDataType = inferDataType(value, dateFormats);
				Class<? extends XAttribute> lastDataType = attributeDataType.get(attribute.getKey());

				if (lastDataType == null) {
					// First occurrence
					attributeDataType.put(attribute.getKey(), currentDataType);
				} else if (!lastDataType.equals(currentDataType)) {
					// Stored data type does not match new occurrence

					if (checkChangeBothWays(currentDataType, lastDataType, XAttributeBoolean.class,
							XAttributeDiscrete.class)) {
						// Mixed Boolean (e.g. 0,1) & Integer -> XAttributeDiscrete
						if (lastDataType != XAttributeDiscrete.class) {
							attributeDataType.put(attribute.getKey(), XAttributeDiscrete.class);	
						}
					} else if (checkChangeBothWays(currentDataType, lastDataType, XAttributeBoolean.class,
							XAttributeContinuous.class)) {
						// Mixed Boolean  (e.g. 0,1) & Float -> XAttributeContinuous
						if (lastDataType != XAttributeContinuous.class) {
							attributeDataType.put(attribute.getKey(), XAttributeContinuous.class);	
						}						
					} else if (checkChangeBothWays(currentDataType, lastDataType, XAttributeDiscrete.class,
							XAttributeContinuous.class)) {
						// Mixed Integer & Float -> XAttributeContinuous
						if (lastDataType != XAttributeContinuous.class) {
							attributeDataType.put(attribute.getKey(), XAttributeContinuous.class);
						}
					} else {
						// Fallback to Literal
						if (lastDataType != XAttributeLiteral.class) {
							attributeDataType.put(attribute.getKey(), XAttributeLiteral.class);	
						}
					}
				}

			}

		}
	}

	private static boolean checkChangeBothWays(Class<? extends XAttribute> dataType, Class<? extends XAttribute> lastDataType,
			Class<? extends XAttribute> class1, Class<? extends XAttribute> class2) {
		return (class1.equals(lastDataType) && class2.equals(dataType))
				|| (class2.equals(lastDataType) && class1.equals(dataType));
	}

	private static XAttribute createAttribute(Class<? extends XAttribute> dataType, Entry<String, XAttribute> entry,
			XFactory factory, Set<DateFormat> dateFormats) {
		if (XAttributeDiscrete.class.equals(dataType)) {
			return factory.createAttributeDiscrete(entry.getKey(), getAttrAsLong(entry.getValue()), null);
		} else if (XAttributeContinuous.class.equals(dataType)) {
			return factory.createAttributeContinuous(entry.getKey(), getAttrAsDouble(entry.getValue()), null);
		} else if (XAttributeBoolean.class.equals(dataType)) {
			return factory.createAttributeBoolean(entry.getKey(), getAttrAsBoolean(entry.getValue()), null);
		} else if (XAttributeLiteral.class.equals(dataType)) {
			return factory.createAttributeLiteral(entry.getKey(), getAttrAsString(entry.getValue()), null);
		} else if (XAttributeTimestamp.class.equals(dataType)) {
			return factory.createAttributeTimestamp(entry.getKey(), getAttrAsDate(entry.getValue(), dateFormats), null);
		} else {
			throw new IllegalArgumentException(String.format("Unexpected Attribute %s: Type %s instead %s", entry.getValue(), entry.getValue().getClass().getSimpleName(), dataType.getSimpleName()));
		}
	}

	private static Date getAttrAsDate(XAttribute value, Set<DateFormat> dateFormats) {
		if (value instanceof XAttributeLiteral) {
			Date date = parseDate(((XAttributeLiteral) value).getValue(), dateFormats);
			if (date == null) {
				throw new IllegalArgumentException("Unexpected Attribute " + value);
			}
			return date;
		} else {
			throw new IllegalArgumentException("Unexpected Attribute " + value);
		}
	}

	private static Date parseDate(String s, Set<DateFormat> dateFormats) {
		ParsePosition pos = new ParsePosition(0);
		for (DateFormat formatter : dateFormats) {
			pos.setIndex(0);
			Date date = formatter.parse(s, pos);
			if (date != null && pos.getIndex() == s.length()) {
				return date;
			}
		}
		return null;
	}

	private static String getAttrAsString(XAttribute value) {
		if (value instanceof XAttributeDiscrete) {
			return Long.toString(((XAttributeDiscrete) value).getValue());
		} else if (value instanceof XAttributeContinuous) {
			return Double.toString(((XAttributeContinuous) value).getValue());
		} else if (value instanceof XAttributeBoolean) {
			return Boolean.toString(((XAttributeBoolean) value).getValue());
		} else if (value instanceof XAttributeLiteral) {
			return ((XAttributeLiteral) value).getValue();
		} else {
			throw new IllegalArgumentException("Unexpected Attribute " + value);
		}
	}

	private static boolean getAttrAsBoolean(XAttribute value) {
		if (value instanceof XAttributeBoolean) {
			return ((XAttributeBoolean) value).getValue();
		} else if (value instanceof XAttributeLiteral) {
			String val = ((XAttributeLiteral) value).getValue();
			if ("0".equals(val) || "N".equalsIgnoreCase(val)) {
				return false;
			} else if ("1".equals(val) || "J".equalsIgnoreCase(val) || "Y".equalsIgnoreCase(val)) {
				return true;
			} else {
				return Boolean.valueOf(val);
			}
		} else if (value instanceof XAttributeDiscrete) {
			long val = ((XAttributeDiscrete) value).getValue();
			if (val != 0 && val != 1) {
				throw new IllegalArgumentException("Unexpected Attribute " + value);
			}
			return Boolean.valueOf(val == 0 ? true : false);
		} else {
			throw new IllegalArgumentException("Unexpected Attribute " + value);
		}
	}

	private static double getAttrAsDouble(XAttribute value) {
		if (value instanceof XAttributeDiscrete) {
			return ((XAttributeDiscrete) value).getValue();
		} else if (value instanceof XAttributeContinuous) {
			return ((XAttributeContinuous) value).getValue();
		} else if (value instanceof XAttributeLiteral) {
			return Double.valueOf(((XAttributeLiteral) value).getValue());
		} else {
			throw new IllegalArgumentException("Unexpected Attribute " + value);
		}
	}

	private static long getAttrAsLong(XAttribute value) {
		if (value instanceof XAttributeDiscrete) {
			return ((XAttributeDiscrete) value).getValue();
		} else if (value instanceof XAttributeLiteral) {
			return Long.valueOf(((XAttributeLiteral) value).getValue());
		} else {
			throw new IllegalArgumentException("Unexpected Attribute " + value);
		}
	}

	private static Pattern DISCRETE_PATTERN = Pattern.compile("(-)?[0-9]{1,19}");
	private static Pattern CONTINUOUS_PATTERN = Pattern.compile("((-)?[0-9]*\\.[0-9]+)|((-)?[0-9]+(\\.[0-9]+)?(e|E)\\+[0-9]+)");
	private static Pattern BOOLEAN_PATTERN = Pattern.compile("(true)|(false)|(TRUE)|(FALSE)|(0)|(1)|(Y)|(N)|(J)");

	private static Class<? extends XAttribute> inferDataType(String value, Set<DateFormat> dateFormats) {
		if (BOOLEAN_PATTERN.matcher(value).matches()) {
			return XAttributeBoolean.class;
		} else if (DISCRETE_PATTERN.matcher(value).matches()) {
			try {
				Long.parseLong(value);
				return XAttributeDiscrete.class;
			} catch (NumberFormatException e) {
				return XAttributeLiteral.class;
			}
		} else if (CONTINUOUS_PATTERN.matcher(value).matches()) {
			return XAttributeContinuous.class;
		} else if (parseDate(value, dateFormats) != null) {
			return XAttributeTimestamp.class;
		} else {
			return XAttributeLiteral.class;
		}
	}

}
