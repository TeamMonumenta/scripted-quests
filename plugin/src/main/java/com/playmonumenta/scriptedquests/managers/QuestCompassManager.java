package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestCompass;
import com.playmonumenta.scriptedquests.quests.components.CompassLocation;
import com.playmonumenta.scriptedquests.quests.components.DeathLocation;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

public class QuestCompassManager {
	private static class ValidCompassEntry {
		private final CompassLocation mLocation;
		private final String mTitle;

		private ValidCompassEntry(CompassLocation loc, String title) {
			mLocation = loc;
			mTitle = title;
		}

		private void directPlayer(Player player) {
			MessagingUtils.sendRawMessage(player, mTitle + ": " + mLocation.getMessage());
			player.setCompassTarget(mLocation.getLocation());
		}
	}

	private static class CompassCacheEntry {
		private final int mLastRefresh;
		private final List<ValidCompassEntry> mEntries;

		private CompassCacheEntry(Player player, List<ValidCompassEntry> entries) {
			mLastRefresh = player.getTicksLived();
			mEntries = entries;
		}

		private boolean isStillValid(Player player) {
			return Math.abs(player.getTicksLived() - mLastRefresh) < 200;
		}
	}

	private List<QuestCompass> mQuests = new ArrayList<QuestCompass>();
	private final Map<UUID, CompassCacheEntry> mCompassCache = new HashMap<UUID, CompassCacheEntry>();

	public QuestCompassManager(Plugin plugin) {
		reload(plugin, null);
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		String compassLocation = plugin.getDataFolder() + File.separator +  "compass";
		mQuests = new ArrayList<QuestCompass>();
		ArrayList<File> listOfFiles;
		ArrayList<String> listOfQuests = new ArrayList<String>();
		int numQuestLocations = 0;
		int numFiles = 0;

		// Attempt to load all JSON files in subdirectories of "compass"
		try {
			File directory = new File(compassLocation);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			listOfFiles = FileUtils.getFilesInDirectory(compassLocation, ".json");
		} catch (IOException e) {
			plugin.getLogger().severe("Caught exception trying to reload quest compass: " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload quest compass: " + e);
			}
			return;
		}

		Collections.sort(listOfFiles);
		for (File file : listOfFiles) {
			try {
				// Load this file into a QuestNpc object
				QuestCompass quest = new QuestCompass(plugin.mWorld, file.getPath());

				// Keep track of statistics for pretty printing later
				int newLocations = quest.getMarkers().size();
				numQuestLocations += newLocations;
				numFiles++;
				listOfQuests.add(quest.getQuestName() + ":" + Integer.toString(newLocations));
				mQuests.add(quest);
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
			sender.sendMessage(ChatColor.GOLD + "Loaded " +
			                   Integer.toString(numQuestLocations) +
			                   " quest compass locations from " + Integer.toString(numFiles) + " files");

			if (numFiles <= 20) {
				Collections.sort(listOfQuests);
				String outMsg = "";
				for (String npc : listOfQuests) {
					if (outMsg.isEmpty()) {
						outMsg = npc;
					} else {
						outMsg = outMsg + ", " + npc;
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

	@SuppressWarnings("unchecked")
	private List<ValidCompassEntry> _getCurrentMarkerTitles(Player player) {
		/*
		 * First check the cache - if it is still valid, returned the cached data
		 * This dramatically improves performance when there are many compass entries
		 */
		CompassCacheEntry cachedEntry = mCompassCache.get(player.getUniqueId());
		if (cachedEntry != null && cachedEntry.isStillValid(player)) {
			return cachedEntry.mEntries;
		}


		/*
		 * No cached entry - need to recompute everything
		 */
		List<ValidCompassEntry> entries = new ArrayList<ValidCompassEntry>();
		for (QuestCompass quest : mQuests) {
			List<CompassLocation> questMarkers = quest.getMarkers(player);

			// Add all the valid markers/titles to the list
			for (int i = 0; i < questMarkers.size(); i++) {
				String title = ChatColor.AQUA + "" + ChatColor.BOLD + quest.getQuestName()
				               + ChatColor.RESET + "" + ChatColor.AQUA;

				if (questMarkers.size() > 1) {
					title += " [" + (i + 1) + "/" + questMarkers.size() + "]";
				}

				entries.add(new ValidCompassEntry(questMarkers.get(i), title));
			}
		}

		// Add player death locations
		if (player.hasMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY)) {
			List<DeathLocation> deathEntries =
			    (List<DeathLocation>)player.getMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY).get(0).value();
			for (int i = 0; i < deathEntries.size(); i++) {
				String title = ChatColor.RED + "" + ChatColor.BOLD + "Death"
				               + ChatColor.RESET + "" + ChatColor.AQUA;

				if (deathEntries.size() > 1) {
					title += " [" + (i + 1) + "/" + deathEntries.size() + "]";
				}

				entries.add(new ValidCompassEntry(deathEntries.get(i), title));
			}
		}

		// Cache this result for later
		mCompassCache.put(player.getUniqueId(), new CompassCacheEntry(player, entries));

		return entries;
	}

	private int _showCurrentQuest(Player player, int index) {
		List<ValidCompassEntry> entries = _getCurrentMarkerTitles(player);

		if (index >= entries.size()) {
			index = 0;
		}

		if (entries.size() == 0) {
			MessagingUtils.sendActionBarMessage(player, "You have no active quest.");
		} else {
			entries.get(index).directPlayer(player);
		}

		return index;
	}

	public void showCurrentQuest(Player player) {
		int index = ScoreboardUtils.getScoreboardValue(player, "locationIndex");

		_showCurrentQuest(player, index);
	}

	public void cycleQuestTracker(Player player) {
		int index = ScoreboardUtils.getScoreboardValue(player, "locationIndex") + 1;

		index = _showCurrentQuest(player, index);

		ScoreboardUtils.setScoreboardValue(player, "locationIndex", index);
	}
}
