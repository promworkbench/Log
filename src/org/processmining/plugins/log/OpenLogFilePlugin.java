package org.processmining.plugins.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

@Plugin(name = "Open XES Log File", parameterLabels = { "Filename" }, returnLabels = { "Log (single process)" }, returnTypes = { XLog.class })
@UIImportPlugin(description = "ProM log files", extensions = { "mxml", "xml", "gz", "zip", "xes", "xez" })
public class OpenLogFilePlugin extends AbstractImportPlugin {

	@Override
	protected Object importFromStream(PluginContext context, InputStream input, String filename, long fileSizeInBytes)
			throws Exception {
		context.getFutureResult(0).setLabel(filename);
		//	System.out.println("Open file");
		XParser parser;
		if (filename.toLowerCase().endsWith(".xes") || filename.toLowerCase().endsWith(".xez") || filename.toLowerCase().endsWith(".xes.gz")) {
			parser = new XesXmlParser();
		} else {
			parser = new XMxmlParser();
		}
		Collection<XLog> logs = parser.parse(new XContextMonitoredInputStream(input, fileSizeInBytes, context
				.getProgress()));

		// log sanity checks;
		// notify user if the log is awkward / does miss crucial information
		if (logs.size() == 0) {
			throw new Exception("No processes contained in log!");
		}

		XLog log = logs.iterator().next();
		XConceptExtension.instance().assignName(log, filename);

		if (log.isEmpty()) {
			throw new Exception("No process instances contained in log!");
		}

		return log;

	}

	/**
	 * This method returns an inputStream for a file. Note that the default
	 * implementation returns "new FileInputStream(file);"
	 * 
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	@Override
	protected InputStream getInputStream(File file) throws Exception {
		FileInputStream stream = new FileInputStream(file);
		if (file.getName().endsWith(".gz") || file.getName().endsWith(".xez")) {
			return new GZIPInputStream(stream);
		}
		if (file.getName().endsWith(".zip")) {
			ZipFile zip = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			ZipEntry zipEntry = entries.nextElement();
			if (entries.hasMoreElements()) {
				throw new InvalidParameterException("Zipped log files should not contain more than one entry.");
			}
			return zip.getInputStream(zipEntry);
		}
		return stream;
	}

}
