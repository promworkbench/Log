package org.processmining.log.csvimport.ui;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;

import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.framework.util.ui.widgets.WidgetColors;

/**
 * 
 */
public class ProMMultiSelectListWithComboBox extends JPanel {
	
	private static class DragListenerImpl<T> implements DragSourceListener, DragGestureListener {

		private final JList<T> list;
		private final DragSource ds = new DragSource();

		public DragListenerImpl(JList<T> list) {
			this.list = list;
			ds.createDefaultDragGestureRecognizer(list, DnDConstants.ACTION_MOVE, this);
		}

		public void dragGestureRecognized(DragGestureEvent dge) {
			StringSelection transferable = new StringSelection(Integer.toString(list.getSelectedIndex()));
			ds.startDrag(dge, DragSource.DefaultCopyDrop, transferable, this);
		}

		public void dragEnter(DragSourceDragEvent dsde) {
		}

		public void dragExit(DragSourceEvent dse) {
		}

		public void dragOver(DragSourceDragEvent dsde) {
		}

		public void dragDropEnd(DragSourceDropEvent dsde) {
		}

		public void dropActionChanged(DragSourceDragEvent dsde) {
		}
	}

	private static class DropHandlerImpl<T> extends TransferHandler {

		private static final long serialVersionUID = -3468373344687124791L;

		private final DefaultListModel<T> listModel;

		public DropHandlerImpl(DefaultListModel<T> listModel) {
			this.listModel = listModel;
		}

		public boolean canImport(TransferHandler.TransferSupport support) {
			if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				return false;
			}
			JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
			if (dl.getIndex() == -1) {
				return false;
			} else {
				return true;
			}
		}

		public boolean importData(TransferHandler.TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}

			Transferable transferable = support.getTransferable();
			String indexString;
			try {
				indexString = (String) transferable.getTransferData(DataFlavor.stringFlavor);
			} catch (Exception e) {
				return false;
			}

			int sourceIndex = Integer.parseInt(indexString);
			JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
			int dropTargetIndex = dl.getIndex();
			T element = listModel.remove(sourceIndex);
			if (sourceIndex < dropTargetIndex) {
				listModel.insertElementAt(element, dropTargetIndex - 1);
			} else {
				listModel.insertElementAt(element, dropTargetIndex);
			}
			return true;
		}
	}

	
	private static final long serialVersionUID = -3989998064589278170L;

	private final JList<String> list;
	private final ProMComboBox<String> comboBox;
	private final DefaultListModel<String> listModel;
	
	public ProMMultiSelectListWithComboBox(ComboBoxModel<String> comboBoxModel) {
		super();
		setBackground(WidgetColors.COLOR_LIST_BG);
		comboBox = new ProMComboBox<>(comboBoxModel);
		comboBox.setPreferredSize(null);
		comboBox.setMaximumSize(null);
		comboBox.setMinimumSize(null);
		
		listModel = new DefaultListModel<>();
		list = new JList<>(listModel);
		list.addKeyListener(new KeyListener() {
			
			public void keyTyped(KeyEvent e) {
			}
			
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					for (int i: list.getSelectedIndices()) {
						listModel.remove(i);
					}
				}
			}
			
			public void keyPressed(KeyEvent e) {
			}
			
		});
		list.setDragEnabled(true);
		list.setDropMode(DropMode.INSERT);
		list.setTransferHandler(new DropHandlerImpl<>(listModel));
		list.setBackground(WidgetColors.COLOR_LIST_BG);
		list.setForeground(WidgetColors.COLOR_LIST_FG);
		list.setSelectionBackground(WidgetColors.COLOR_LIST_SELECTION_BG);
		list.setSelectionForeground(WidgetColors.COLOR_LIST_SELECTION_FG);
		
		final ProMScrollPane scroller = new ProMScrollPane(list);
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		new DragListenerImpl<>(list);
		comboBox.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				listModel.addElement((String) comboBox.getSelectedItem());
			}
		});
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(comboBox);
		add(scroller);
	}

	public JList<String> getList() {
		return list;
	}
	
	public ProMComboBox<String> getComboBox() {
		return comboBox;
	}
	
	public List<String> getElements() {
		ArrayList<String> elements = new ArrayList<>();
		for (int i = 0; i < listModel.getSize(); i++) {
			elements.add(listModel.get(i));
		}
		return elements;
	}

	public void addElement(String element) {
		listModel.addElement(element);
	}

	public ListModel<String> getListModel() {
		return listModel;
	}

}