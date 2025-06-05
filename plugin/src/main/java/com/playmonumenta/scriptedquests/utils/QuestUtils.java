package com.playmonumenta.scriptedquests.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class QuestUtils {
	@FunctionalInterface
	public interface JsonLoadAction extends JsonLoadActionWithFile {
		/**
		 * Called with each file loaded into a JSON object
		 * Return value is identifying string to return to the user or null
		 */
		@Nullable String load(JsonObject object) throws Exception;

		@Override
		default @Nullable String load(JsonObject object, File file) throws Exception {
			return load(object);
		}
	}

	@FunctionalInterface
	public interface JsonLoadActionWithFile {
		/**
		 * Called with each file loaded into a JSON object
		 * Return value is identifying string to return to the user or null
		 */
		String load(JsonObject object, File file) throws Exception;
	}

	public static void loadScriptedQuests(Plugin plugin, String folderName, @Nullable Audience audience, JsonLoadAction action) {
		loadScriptedQuests(plugin, folderName, audience, (JsonLoadActionWithFile) action);
	}

	public static void loadScriptedQuests(Plugin plugin, String folderName, @Nullable Audience audience, JsonLoadActionWithFile action) {
		if (audience == null) {
			audience = Bukkit.getConsoleSender();
		}
		String folderLocation = plugin.getDataFolder() + File.separator + folderName;
		ArrayList<File> listOfFiles;
		ArrayList<String> listOfLabels = new ArrayList<>();
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
			audience.sendMessage(Component.text("Caught exception trying to reload " + folderName + ": " + e, NamedTextColor.RED));
			return;
		}

		Collections.sort(listOfFiles);
		for (File file : listOfFiles) {
			try {
				String label = loadScriptedQuestsFile(file, action);
				numFiles++;
				if (label != null) {
					listOfLabels.add(label);
				}
			} catch (Exception e) {
				audience.sendMessage(Component.text("Failed to load quest file '" + file.getPath() + "'", NamedTextColor.RED));
				MessagingUtils.sendStackTrace(audience, e);
			}
		}

		audience.sendMessage(Component.text("Loaded " + numFiles + " " + folderName + " files", NamedTextColor.GOLD));

		if (numFiles <= 20) {
			Collections.sort(listOfLabels);
			StringBuilder outMsg = new StringBuilder();
			for (String label : listOfLabels) {
				if (outMsg.isEmpty()) {
					outMsg = new StringBuilder(label);
				} else {
					outMsg.append(", ").append(label);
				}

				if (outMsg.length() > 1000) {
					audience.sendMessage(Component.text(outMsg.toString(), NamedTextColor.GOLD));
					outMsg = new StringBuilder();
				}
			}

			if (!outMsg.isEmpty()) {
				audience.sendMessage(Component.text(outMsg.toString(), NamedTextColor.GOLD));
			}
		}
	}

	/**
	 * (re)loads a single SQ file.
	 *
	 * @param file   The file to reload
	 * @param action Action to parse the file
	 * @return The value returned by {@code action}
	 * @throws Exception If there was an error loading the file (e.g. file does not exist, cannot be read, or cannot be parsed)
	 */
	public static @Nullable String loadScriptedQuestsFile(File file, JsonLoadActionWithFile action) throws Exception {
		String content = FileUtils.readFile(file.getPath());
		if (content.isEmpty()) {
			throw new Exception("Failed to parse file as JSON object");
		}

		Gson gson = new Gson();
		JsonObject object = gson.fromJson(content, JsonObject.class);
		if (object == null) {
			throw new Exception("Failed to parse file as JSON object");
		}

		return action.load(object, file);
	}

	public static void save(Plugin plugin, @Nullable CommandSender sender, JsonObject object, File file) {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(object);
			FileUtils.writeFile(file.getPath(), json);
		} catch (Exception e) {
			plugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(Component.text("Failed to save quest file '" + file.getPath() + "'", NamedTextColor.RED));
				MessagingUtils.sendStackTrace(sender, e);
			}
		}
	}
}
