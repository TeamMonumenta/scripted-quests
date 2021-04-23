package com.playmonumenta.scriptedquests.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class FileUtils {
	public static String readFile(String fileName) throws Exception {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		File file;

		if (fileName == null || fileName.isEmpty()) {
			throw new Exception("Filename is null or empty");
		}

		file = new File(fileName);
		if (!file.exists()) {
			throw new FileNotFoundException("File '" + fileName + "' does not exist");
		}

		InputStreamReader reader = null;
		final int bufferSize = 1024;
		final char[] buffer = new char[bufferSize];
		final StringBuilder content = new StringBuilder();

		try {
			reader = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8);
			while (true) {
				int rsz = reader.read(buffer, 0, buffer.length);
				if (rsz < 0) {
					break;
				}
				content.append(buffer, 0, rsz);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		return content.toString();
	}

	public static void writeFile(String fileName, String contents) throws IOException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		File file = new File(fileName);

		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}

		/* Write the data to a temporary file in the same directory as the file */
		File tempFile = File.createTempFile(file.getName(), null, file.getParentFile());

		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8);
			writer.write(contents);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}

		Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		tempFile.renameTo(file);
	}


	/**
	 * Returns a list of all files in the directory that are both regular files
	 * AND end with the specified string
	 */
	public static ArrayList<File> getFilesInDirectory(String folderPath,
			String endsWith) throws IOException {
		ArrayList<File> matchedFiles = new ArrayList<File>();

		Files.walk(Paths.get(folderPath), 100, FileVisitOption.FOLLOW_LINKS).forEach(path -> {
			if (path.toString().toLowerCase().endsWith(endsWith)) {
				// Note - this will pass directories that end with .json back to the caller too
				matchedFiles.add(path.toFile());
			}
		});

		return matchedFiles;
	}
}
