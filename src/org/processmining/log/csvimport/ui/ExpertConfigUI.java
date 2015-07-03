package org.processmining.log.csvimport.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JLabel;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.config.CSVConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVEmptyCellHandlingMode;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVErrorHandlingMode;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class ExpertConfigUI extends CSVConfigurationPanel {

	private static final long serialVersionUID = 7749368962812585099L;

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

	private final ProMComboBox<XFactoryUI> xFactoryChoice;
	private final ProMComboBox<Boolean> repairDataTypesCbx;
	private final ProMComboBox<CSVEmptyCellHandlingMode> emptyCellHandlingModeCbx;
	private final ProMComboBox<CSVErrorHandlingMode> errorHandlingModeCbx;

	public ExpertConfigUI(final CSVFile csv, final CSVConfig importConfig, final CSVConversionConfig conversionConfig) {
		super();
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		setMaximumSize(new Dimension(COLUMN_WIDTH * 2, Short.MAX_VALUE));

		JLabel conversionOptionsLabel = SlickerFactory.instance().createLabel(
				"Expert Conversion Options (Defaults are a good guess)");
		conversionOptionsLabel.setFont(conversionOptionsLabel.getFont().deriveFont(Font.BOLD, 20));

		xFactoryChoice = new ProMComboBox<>(Iterables.transform(getAvailableXFactories(),
				new Function<XFactory, XFactoryUI>() {

					public XFactoryUI apply(XFactory factory) {
						return new XFactoryUI(factory);
					}

				}));
		xFactoryChoice.setSelectedItem(new XFactoryUI(conversionConfig.getFactory()));
		JLabel xFactoryLabel = createLabel("XFactory", "Implementation that is be used to create the Log.");

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
		JLabel emptyCellHandlingModeLabel = createLabel(
				"Sparse / Dense Log",
				"Exclude (sparse) or include (dense) empty cells in the conversion. This affects how empty cells in the CSV are handled. "
				+ "Some plug-ins require the log to be dense, i.e., all attributes are defined for each event. "
				+ "In other cases it might be more efficient or even required to only add attributes to events if the attributes actually contain data.");
		emptyCellHandlingModeCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.setEmptyCellHandlingMode((CSVEmptyCellHandlingMode) emptyCellHandlingModeCbx
						.getSelectedItem());
			}
		});

		repairDataTypesCbx = new ProMComboBox<>(new Boolean[] { true, false });
		repairDataTypesCbx.setSelectedItem(conversionConfig.isShouldGuessDataTypes());
		JLabel repairDataTypesLabel = createLabel(
				"Guess Attribute Types",
				"Should the plug-in make an attempt to guess the correct datatypes after conversion? Leave as 'true' in case you are unsure!");
		repairDataTypesCbx.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				conversionConfig.setShouldGuessDataTypes((Boolean) repairDataTypesCbx.getSelectedItem());
			}
		});

		SequentialGroup verticalGroup = layout.createSequentialGroup();
		verticalGroup.addGroup(layout
				.createParallelGroup()
				.addGroup(
						layout.createSequentialGroup().addComponent(errorHandlingModeLabel)
								.addComponent(errorHandlingModeCbx))
				.addGroup(layout.createSequentialGroup().addComponent(xFactoryLabel).addComponent(xFactoryChoice)));
		verticalGroup.addGroup(layout
				.createParallelGroup()
				.addGroup(
						layout.createSequentialGroup().addComponent(emptyCellHandlingModeLabel)
								.addComponent(emptyCellHandlingModeCbx))
				.addGroup(
						layout.createSequentialGroup().addComponent(repairDataTypesLabel)
								.addComponent(repairDataTypesCbx)));

		ParallelGroup horizontalGroup = layout.createParallelGroup();
		horizontalGroup.addGroup(layout
				.createSequentialGroup()
				.addGroup(
						layout.createParallelGroup()
								.addComponent(errorHandlingModeLabel, Alignment.LEADING, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)
								.addComponent(errorHandlingModeCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH))
				.addGroup(
						layout.createParallelGroup()
								.addComponent(xFactoryLabel, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)
								.addComponent(xFactoryChoice, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)));
		horizontalGroup.addGroup(layout
				.createSequentialGroup()
				.addGroup(
						layout.createParallelGroup()
								.addComponent(emptyCellHandlingModeLabel, Alignment.LEADING, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)
								.addComponent(emptyCellHandlingModeCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH))
				.addGroup(
						layout.createParallelGroup()
								.addComponent(repairDataTypesLabel, Alignment.LEADING, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)
								.addComponent(repairDataTypesCbx, COLUMN_WIDTH, COLUMN_WIDTH, COLUMN_WIDTH)));

		layout.linkSize(errorHandlingModeLabel, xFactoryLabel);
		layout.linkSize(emptyCellHandlingModeLabel, repairDataTypesLabel);
		
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);

		layout.setVerticalGroup(verticalGroup);
		layout.setHorizontalGroup(horizontalGroup);
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

}
