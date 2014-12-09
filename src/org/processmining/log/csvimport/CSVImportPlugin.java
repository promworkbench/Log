package org.processmining.log.csvimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

/**
 * @author F. Mannhardt
 *
 */
@Plugin(name = "Import a CSV file and convert it to XES", parameterLabels = { "Filename" }, returnLabels = { "Imported CSV File" }, returnTypes = { CSVFileReference.class })
@UIImportPlugin(description = "CSV File (Convert to XES with Log Package)", extensions = { "csv", "zip", "csv.gz", "txt" })
public final class CSVImportPlugin extends AbstractImportPlugin {

	@Override
	protected CSVFile importFromStream(final PluginContext context, final InputStream input, final String filename,
			final long fileSizeInBytes) throws Exception {
		context.log("Copy CSV file to working area ...");
		context.getProgress().setCaption("Copy CSV file to working area ...");
		input.close();
		Path sourcePath = getFile().toPath();
		Path targetPath = Files.createTempDirectory("csvimport");
		recursiveDeleteOnShutdownHook(targetPath);
		Path targetFile = Files.copy(sourcePath, targetPath.resolve(sourcePath.getFileName()),
				StandardCopyOption.REPLACE_EXISTING);
		context.getProgress().setCaption("Sucess!");
		context.getFutureResult(0).setLabel("Imported CSV: "+filename);
		return new CSVFileReference(targetFile, filename, fileSizeInBytes, targetPath);
	}

	// Found here http://stackoverflow.com/questions/15022219/does-files-createtempdirectory-remove-the-directory-after-jvm-exits-normally/20280989#20280989
	private static void recursiveDeleteOnShutdownHook(final Path path) {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
							if (e == null) {
								Files.delete(dir);
								return FileVisitResult.CONTINUE;
							}
							throw e;
						}
					});
				} catch (IOException e) {
					throw new RuntimeException("Failed to delete " + path, e);
				}
			}
		}));
	}

}
