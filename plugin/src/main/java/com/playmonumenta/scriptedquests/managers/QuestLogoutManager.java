package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestLogout;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class QuestLogoutManager {
	ArrayList<QuestLogout> mLogouts = new ArrayList<QuestLogout>();

	/* If sender is non-null, it will be sent debugging information */
	public void reload(Plugin plugin, CommandSender sender) {
		String logoutLocation = plugin.getDataFolder() + File.separator +  "logout";
		mLogouts = new ArrayList<QuestLogout>();
		ArrayList<File> listOfFiles;
		int numFiles = 0;

		// Attempt to load all JSON files in subdirectories of "logout"
		try {
			File directory = new File(logoutLocation);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			listOfFiles = FileUtils.getFilesInDirectory(logoutLocation, ".json");
		} catch (IOException e) {
			plugin.getLogger().severe("Caught exception trying to reload logout quests: " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload logout quests: " + e);
			}
			return;
		}

		Collections.sort(listOfFiles);
		for (File file : listOfFiles) {
			try {
				String content = FileUtils.readFile(file.getPath());
				if (content != null && !content.isEmpty()) {
					Gson gson = new Gson();
					JsonObject object = gson.fromJson(content, JsonObject.class);
					if (object == null) {
						throw new Exception("Failed to parse file '" + file.getPath() + "' as JSON object");
					}

					// Load this file into a QuestLogout object
					mLogouts.add(new QuestLogout(object));

					numFiles++;
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load logout quest file '" + file.getPath() + "'");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}

		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Loaded " + Integer.toString(numFiles) + " logout quest files");
		}
	}

	public QuestLogoutManager(Plugin plugin) {
		reload(plugin, null);
	}

	public boolean logoutEvent(Plugin plugin, PlayerQuitEvent event) {
		/* Try each available logout-triggered quest */
		for (QuestLogout logout : mLogouts) {
			/* Stop after the first matching quest */
			if (logout.logoutEvent(plugin, event)) {
				return true;
			}
		}

		return false;
	}
}
