package com.playmonumenta.scriptedquests.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class QuestUtils {
	@FunctionalInterface
	public interface JsonLoadAction {
		/**
		 * Called with each file loaded into a JSON object
		 * Return value is identifying string to return to the user or null
		 */
		String load(JsonObject object) throws Exception;
	}

	public static void loadScriptedQuests(Plugin plugin, String folderName, CommandSender sender, JsonLoadAction action) {
		String folderLocation = plugin.getDataFolder() + File.separator + folderName;
		ArrayList<File> listOfFiles;
		ArrayList<String> listOfLabels = new ArrayList<String>();
		int numFiles = 0;

		// Attempt to load all JSON files in subdirectories
		try {
			File directory = new File(folderLocation);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			listOfFiles = FileUtils.getFilesInDirectory(folderLocation, ".json");
		} catch (IOException e) {
			plugin.getLogger().severe("Caught exception trying to reload " + folderName + ": " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload " + folderName + ": " + e);
			}
			return;
		}

		Collections.sort(listOfFiles);
		for (File file : listOfFiles) {
			try {
				String content = FileUtils.readFile(file.getPath());
				if (content == null || content.isEmpty()) {
					throw new Exception("Failed to parse file as JSON object");
				}

				Gson gson = new Gson();
				JsonObject object = gson.fromJson(content, JsonObject.class);
				if (object == null) {
					throw new Exception("Failed to parse file as JSON object");
				}

				String label = action.load(object);
				numFiles++;
				if (label != null) {
					listOfLabels.add(label);
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load quest file '" + file.getPath() + "'");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}

		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Loaded " + Integer.toString(numFiles) + " " + folderName + " files");

			if (numFiles <= 20) {
				Collections.sort(listOfLabels);
				String outMsg = "";
				for (String label : listOfLabels) {
					if (outMsg.isEmpty()) {
						outMsg = label;
					} else {
						outMsg = outMsg + ", " + label;
					}

					if (outMsg.length() > 1000) {
						sender.sendMessage(ChatColor.GOLD + outMsg);
						outMsg = "";
					}
				}

				if (!outMsg.isEmpty()) {
					sender.sendMessage(ChatColor.GOLD + outMsg);
				}
			}
		}
	}
}
