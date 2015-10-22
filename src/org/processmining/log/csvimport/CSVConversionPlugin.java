package org.processmining.log.csvimport;

import java.io.IOException;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
import org.processmining.log.csv.CSVFile;
import org.processmining.log.csv.config.CSVConfig;
import org.processmining.log.csvimport.CSVConversion.ConversionResult;
import org.processmining.log.csvimport.CSVConversion.ProgressListener;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.exception.CSVConversionConfigException;
import org.processmining.log.csvimport.exception.CSVConversionException;
import org.processmining.log.csvimport.handler.XESConversionHandlerImpl;
import org.processmining.log.csvimport.ui.ConversionConfigUI;
import org.processmining.log.csvimport.ui.ExpertConfigUI;
import org.processmining.log.csvimport.ui.ImportConfigUI;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;

/**
 * CSV to XES XLog conversion plug-in
 * 
 * @author F. Mannhardt
 * 
 */
public final class CSVConversionPlugin {

	@Plugin(name = "Convert CSV to XES", parameterLabels = { "CSV" }, returnLabels = { "XES Event Log" }, // 
	returnTypes = { XLog.class }, userAccessible = true, mostSignificantResult = 1,// 
	keywords = { "CSV", "OpenXES", "Conversion", "Import" }, help = "Converts the CSV file to a OpenXES XLog object.")
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = " F. Mannhardt, N. Tax, D.M.M. Schunselaar", // 
	email = "f.mannhardt@tue.nl, n.tax@tue.nl, d.m.m.schunselaar@tue.nl", pack="Log")
	public XLog convertCSVToXES(final UIPluginContext context, CSVFile csvFile) {

		InteractionResult result = InteractionResult.CONTINUE;

		try {
			CSVConfig importConfig = new CSVConfig(csvFile);
			CSVConversionConfig csvConversionConfig = null;

			int i = 0;
			while (result != InteractionResult.FINISHED) {
				switch (i) {
					case 0 :
						result = queryImportConfig(context, csvFile, importConfig);
						csvConversionConfig = new CSVConversionConfig(csvFile, importConfig);
						csvConversionConfig.autoDetect();
						break;
					case 1 :
						result = queryConversionConfig(context, csvFile, importConfig, csvConversionConfig);
						break;
					case 2 :
						result = queryExpertConfig(context, csvFile, importConfig, csvConversionConfig);
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
				ConversionResult<XLog> conversionResult = doConvertCSVToXes(context, csvFile, importConfig,
						csvConversionConfig, csvConversion);
				if (conversionResult.hasConversionErrors()) {
					ProMUIHelper.showWarningMessage(context, conversionResult.getConversionErrors(),
							"Warning: Some issues have been detected during conversion");
				}
				return conversionResult.getResult();
			} catch (CSVConversionException e) {
				String errorMessage = Joiner.on("\ncaused by\n").join(Throwables.getCausalChain(e));
				String stackTrace = Throwables.getStackTraceAsString(e);
				ProMUIHelper.showErrorMessage(context, errorMessage + "\n\n Stack Trace\n" + stackTrace,
						"Conversion Failed");
				context.getFutureResult(0).cancel(false);
				return null;
			}
		} catch (IOException e) {
			String errorMessage = Joiner.on("\ncaused by\n").join(Throwables.getCausalChain(e));
			String stackTrace = Throwables.getStackTraceAsString(e);
			ProMUIHelper.showErrorMessage(context, errorMessage + "\n\n Stack Trace\n" + stackTrace,
					"Conversion Failed");
			context.getFutureResult(0).cancel(false);
			return null;
		}

	}

	public ConversionResult<XLog> doConvertCSVToXes(final PluginContext context, CSVFile csvFile,
			CSVConfig importConfig, CSVConversionConfig conversionConfig, CSVConversion csvConversion)
			throws CSVConversionConfigException, CSVConversionException {

		ProgressListener progressListener = new ProgressListener() {

			public Progress getProgress() {
				return context.getProgress();
			}

			public void log(String message) {
				context.log(message);

			}
		};

		XESConversionHandlerImpl xesHandler = new XESConversionHandlerImpl(importConfig, conversionConfig);
		final ConversionResult<XLog> conversionResult = csvConversion.convertCSV(progressListener, importConfig,
				conversionConfig, csvFile, xesHandler);
		final XLog convertedLog = conversionResult.getResult();

		if (xesHandler.hasConversionErrors()) {
			context.log(xesHandler.getConversionErrors(), MessageLevel.WARNING);
		}

		return new ConversionResult<XLog>() {

			public boolean hasConversionErrors() {
				return conversionResult.hasConversionErrors();
			}

			public XLog getResult() {
				return convertedLog;
			}

			public String getConversionErrors() {
				return conversionResult.getConversionErrors();
			}
		};

	}

	public static InteractionResult queryExpertConfig(UIPluginContext context, CSVFile csv, CSVConfig importConfig,
			CSVConversionConfig converionConfig) {
		ExpertConfigUI expertConfigUI = new ExpertConfigUI(csv, importConfig, converionConfig);
		return context.showWizard("Configure Additional Conversion Settings", false, true, expertConfigUI);
	}

	public static InteractionResult queryImportConfig(UIPluginContext context, CSVFile csv, CSVConfig importConfig) {
		ImportConfigUI importConfigUI = new ImportConfigUI(csv, importConfig);
		return context.showWizard("Configure CSV Parser Settings", true, false, importConfigUI);
	}

	public static InteractionResult queryConversionConfig(UIPluginContext context, CSVFile csv, CSVConfig importConfig,
			CSVConversionConfig conversionConfig) throws IOException {
		try (ConversionConfigUI conversionConfigUI = new ConversionConfigUI(csv, importConfig, conversionConfig)) {
			return context.showWizard("Configure Conversion from CSV to XES", false, false, conversionConfigUI);
		}
	}

}
