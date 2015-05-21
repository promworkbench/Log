package org.processmining.log.csvimport;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csvimport.CSVConversion.ProgressListener;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.config.CSVConversionConfig.CSVMapping;
import org.processmining.log.csvimport.config.CSVConversionConfig.Datatype;
import org.processmining.log.csvimport.config.CSVImportConfig;
import org.processmining.log.csvimport.exception.CSVConversionConfigException;
import org.processmining.log.csvimport.exception.CSVConversionException;
import org.processmining.log.repair.RepairAttributeDataType;
import org.processmining.log.repair.RepairAttributeDataType.ReviewCallback;

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

	@Plugin(name = "Convert CSV to XES", parameterLabels = { "CSV" }, returnLabels = { "XES Log" }, returnTypes = { XLog.class }, userAccessible = true, mostSignificantResult = 1, keywords = {
			"CSV", "XES", "Conversion" }, help = "Converts the CSV file to a XES XLog object.")
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = " F. Mannhardt", email = "f.mannhardt@tue.nl", website = "http://fmannhardt.de")
	public XLog convertCSVToXES(final UIPluginContext context, CSVFile csv) throws IOException, UserCancelledException {

		InteractionResult result = InteractionResult.CONTINUE;
		
		CSVImportConfig importConfig = new CSVImportConfig();
		CSVConversionConfig csvConversionConfig = null;
		
		int i = 0;
		while (result != InteractionResult.FINISHED) {
			switch (i) {
				case 0:
					result = CSVConversion.queryImportConfig(context, csv, importConfig);
					csvConversionConfig = new CSVConversionConfig(csv.readHeader(importConfig));
					break;
				case 1:
					result = CSVConversion.queryConversionConfig(context, csv, importConfig, csvConversionConfig);
					break;
				case 2:
					result = CSVConversion.queryExpertConfig(context, csv, importConfig, csvConversionConfig);
					break;
			}
			if (result == InteractionResult.NEXT || result == InteractionResult.CONTINUE) {
				i++;
			} else if (result == InteractionResult.PREV) {
				i--;
			} else if (result == InteractionResult.CANCEL) {
				return null;
			}
		}
		
		CSVConversion csvConversion = new CSVConversion();
		try {
			return doConvertCSVToXes(context, csv, importConfig, csvConversionConfig, csvConversion);
		} catch (CSVConversionException e) {
			String errorMessage = Joiner.on("\n caused by \n").join(Throwables.getCausalChain(e));
			ProMUIHelper.showErrorMessage(context, errorMessage, "Conversion Failed");
			context.getFutureResult(0).cancel(false);
			return null;
		}
	}

	public XLog doConvertCSVToXes(final PluginContext context, CSVFile csv, CSVImportConfig importConfig,
			CSVConversionConfig conversionConfig, CSVConversion csvConversion) throws CSVConversionConfigException, CSVConversionException {
			XLog convertedLog = csvConversion.doConvertCSVToXES(new ProgressListener() {

				public Progress getProgress() {
					return context.getProgress();
				}

				public void log(String message) {
					context.log(message);

				}
			}, csv, importConfig, conversionConfig);
			if (conversionConfig.isShouldGuessDataTypes()) {
				RepairAttributeDataType repairTypes = new RepairAttributeDataType();
				Builder<DateFormat> dateFormatSet = ImmutableSet.<DateFormat>builder().addAll(
						CSVConversion.STANDARD_DATE_FORMATTERS);
				// Add all the custom date formats from the configuration
				final Map<String, CSVMapping> conversionMap = conversionConfig.getConversionMap();
				for (Entry<String, CSVMapping> mapping: conversionMap.entrySet()) {
					if (mapping.getValue().getDataType() == Datatype.TIME && mapping.getValue().getFormat() != null) {
						dateFormatSet.add((DateFormat) mapping.getValue().getFormat());
					}
				}
				ImmutableSet<DateFormat> dateFormatters = dateFormatSet.build();
				context.log("Repairing the data types of event attributes in the log ...");
				repairTypes.doRepairEventAttributes(context, convertedLog, dateFormatters, new ReviewCallback() {
					
					public Map<String, Class<? extends XAttribute>> reviewDataTypes(
							Map<String, Class<? extends XAttribute>> guessedDataTypes) {
						//TODO only use guessed type when not overwritten by user defined type
						return guessedDataTypes;
					}
				});
				context.getFutureResult(0).setLabel(
						Files.getNameWithoutExtension(csv.getFilename()) + " (converted @"
								+ DateFormat.getTimeInstance().format(new Date()) + ")");
			}
			return convertedLog;

	}

}
