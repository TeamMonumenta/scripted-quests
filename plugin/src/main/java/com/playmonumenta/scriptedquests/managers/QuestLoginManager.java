package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestLogin;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class QuestLoginManager {
	ArrayList<QuestLogin> mLogins = new ArrayList<QuestLogin>();

	/* If sender is non-null, it will be sent debugging information */
	public void reload(Plugin plugin, CommandSender sender) {
		String loginLocation = plugin.getDataFolder() + File.separator +  "login";
		mLogins = new ArrayList<QuestLogin>();
		ArrayList<File> listOfFiles;
		int numFiles = 0;

		// Attempt to load all JSON files in subdirectories of "login"
		try {
			File directory = new File(loginLocation);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			listOfFiles = FileUtils.getFilesInDirectory(loginLocation, ".json");
		} catch (IOException e) {
			plugin.getLogger().severe("Caught exception trying to reload login quests: " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload login quests: " + e);
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

					// Load this file into a QuestLogin object
					mLogins.add(new QuestLogin(object));

					numFiles++;
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load login quest file '" + file.getPath() + "'");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}

		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Loaded " + Integer.toString(numFiles) + " login quest files");
		}
	}

	public QuestLoginManager(Plugin plugin) {
		reload(plugin, null);
	}

	public boolean loginEvent(Plugin plugin, PlayerJoinEvent event) {
		boolean success = false;

		/* Try each available login-triggered quest */
		for (QuestLogin login : mLogins) {
			/* Don't stop after the first matching quest */
			if (login.loginEvent(plugin, event)) {
				success = true;
			}
		}

		return success;
	}
}
