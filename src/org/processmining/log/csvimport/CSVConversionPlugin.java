package org.processmining.log.csvimport;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
import org.processmining.log.csvimport.CSVConversion.ProgressListener;
import org.processmining.log.csvimport.exception.CSVConversionConfigException;
import org.processmining.log.csvimport.exception.CSVConversionException;
import org.processmining.log.repair.RepairAttributeDataType;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.io.Files;

/**
 * CSV to XES XLog conversion plug-in
 * 
 * @author F. Mannhardt
 * 
 */
public final class CSVConversionPlugin {

	@Plugin(name = "Convert CSV to XES", parameterLabels = { "CSV" }, returnLabels = { "XES Log" }, returnTypes = { XLog.class }, userAccessible = true, mostSignificantResult = 1,
	// categories = { PluginCategory.Enhancement },
	keywords = { "CSV", "XES", "Conversion" }, help = "Converts the CSV file to a XES XLog object.")
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = " F. Mannhardt", email = "f.mannhardt@tue.nl", website = "http://fmannhardt.de")
	public XLog convertCSVToXES(final UIPluginContext context, CSVFile csv) throws IOException, UserCancelledException {
		CSVImportConfig importConfig = CSVConversion.queryImportConfig(context, csv);
		CSVConversionConfig conversionConfig = CSVConversion.queryConversionConfig(context, csv, importConfig);
		CSVConversion csvConversion = new CSVConversion();
		try {
			XLog convertedLog = csvConversion.doConvertCSVToXES(new ProgressListener() {

				public Progress getProgress() {
					return context.getProgress();
				}

				public void log(String message) {
					context.log(message);

				}
			}, csv, importConfig, conversionConfig);
			if (conversionConfig.isRepairDataTypes) {
				RepairAttributeDataType repairTypes = new RepairAttributeDataType();
				Builder<DateFormat> dateFormatSet = ImmutableSet.<DateFormat>builder().addAll(
						CSVConversion.STANDARD_DATE_FORMATTERS);
				if (conversionConfig.timeFormat != null) {
					try {
						dateFormatSet.add(new SimpleDateFormat(conversionConfig.timeFormat));
					} catch (IllegalArgumentException e) {
						context.log(e);
					}
				}
				ImmutableSet<DateFormat> dateFormatters = dateFormatSet.build();
				context.log("Repairing the data types of event attributes in the log ...");
				repairTypes.doRepairEventAttributes(context, convertedLog, dateFormatters);
				context.log("Repairing the data types of trace attributes in the log ...");
				repairTypes.doRepairTraceAttributes(context, convertedLog, dateFormatters);
				context.getFutureResult(0).setLabel(
						Files.getNameWithoutExtension(csv.getFilename()) + " (converted @"
								+ DateFormat.getTimeInstance().format(new Date()) + ")");
			}
			return convertedLog;
		} catch (CSVConversionConfigException e) {
			JOptionPane.showMessageDialog(null, Joiner.on("\n caused by \n").join(Throwables.getCausalChain(e)),
					"Configuration Incomplete", JOptionPane.ERROR_MESSAGE);
			context.getFutureResult(0).cancel(false);
			return null;
		} catch (CSVConversionException e) {
			JOptionPane.showMessageDialog(null, Joiner.on("\ncaused by: ").join(Throwables.getCausalChain(e)),
					"Error Converting", JOptionPane.ERROR_MESSAGE);
			context.getFutureResult(0).cancel(false);
			return null;
		}
	}

}
