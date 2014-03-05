package org.processmining.plugins.log.logfilters;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import org.processmining.framework.util.collection.AlphanumComparator;
import org.processmining.framework.util.ui.widgets.ProMList;

public class AttributeFilterDialog extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5477222861834208877L;
	private Map<String, ProMList<String>> lists;
	AttributeFilterParameters parameters;
	
	public AttributeFilterDialog(AttributeFilterParameters parameters) {
		this.parameters = parameters;
		Map<String, List<String>> values = new HashMap<String, List<String>>();
		for (String key : parameters.getFilter().keySet()) {
			values.put(key, new ArrayList<String>());
			values.get(key).addAll(parameters.getFilter().get(key));
			Collections.sort(values.get(key), new AlphanumComparator());
		}

		double size[][] = { { TableLayoutConstants.FILL }, { TableLayoutConstants.FILL } };
		setLayout(new TableLayout(size));

		setOpaque(false);
		
		lists = new HashMap<String, ProMList<String>>();
		JTabbedPane tabbedPane = new JTabbedPane();
		List<String> sortedKeys = new ArrayList<String>();
		sortedKeys.addAll(values.keySet());
		Collections.sort(sortedKeys, new AlphanumComparator());
		for (String key : sortedKeys) {
			DefaultListModel listModel = new DefaultListModel();
			int[] selected = new int[values.get(key).size()];
			int i = 0;
			for (String value: values.get(key)) {
				listModel.addElement(value);
				selected[i] = i;
				i++;
			}
			ProMList<String> list = new ProMList<String>("Select values", listModel);
			lists.put(key, list);
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			list.setSelectedIndices(selected);
			list.setPreferredSize(new Dimension(100, 100));
			
			tabbedPane.add(key, list);
		}
		this.add(tabbedPane, "0, 0");
	}
	
	public void applyFilter() {
		for (String key : lists.keySet()) {
			parameters.getFilter().get(key).clear();
			parameters.getFilter().get(key).addAll(lists.get(key).getSelectedValuesList());
		}
	}
}
