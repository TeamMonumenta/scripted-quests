package pe.scriptedquests.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import pe.scriptedquests.Plugin;
import pe.scriptedquests.quests.QuestDeath;
import pe.scriptedquests.utils.FileUtils;
import pe.scriptedquests.utils.MessagingUtils;

public class QuestDeathManager {
	ArrayList<QuestDeath> mDeaths = new ArrayList<QuestDeath>();

	/* If sender is non-null, it will be sent debugging information */
	public void reload(Plugin plugin, CommandSender sender) {
		String deathLocation = plugin.getDataFolder() + File.separator +  "death";
		mDeaths = new ArrayList<QuestDeath>();
		ArrayList<File> listOfFiles;
		int numFiles = 0;

		// Attempt to load all JSON files in subdirectories of "death"
		try {
			File directory = new File(deathLocation);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			listOfFiles = FileUtils.getFilesInDirectory(deathLocation, ".json");
		} catch (IOException e) {
			plugin.getLogger().severe("Caught exception trying to reload death quests: " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload death quests: " + e);
			}
			return;
		}

		for (File file : listOfFiles) {
			try {
				String content = FileUtils.readFile(file.getPath());
				if (content != null && !content.isEmpty()) {
					Gson gson = new Gson();
					JsonObject object = gson.fromJson(content, JsonObject.class);
					if (object == null) {
						throw new Exception("Failed to parse file '" + file.getPath() + "' as JSON object");
					}

					// Load this file into a QuestDeath object
					mDeaths.add(new QuestDeath(object));

					numFiles++;
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load death quest file '" + file.getPath() + "'");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}

		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Loaded " + Integer.toString(numFiles) + " death quest files");
		}
	}

	public QuestDeathManager(Plugin plugin) {
		reload(plugin, null);
	}

	public boolean deathEvent(Plugin plugin, PlayerDeathEvent event) {
		/* Try each available death-triggered quest */
		for (QuestDeath death : mDeaths) {
			/* Stop after the first matching quest */
			if (death.deathEvent(plugin, event)) {
				return true;
			}
		}

		return false;
	}
}
