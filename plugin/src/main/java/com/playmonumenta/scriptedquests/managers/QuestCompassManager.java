package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestCompass;
import com.playmonumenta.scriptedquests.quests.components.CompassLocation;
import com.playmonumenta.scriptedquests.quests.components.DeathLocation;
import com.playmonumenta.scriptedquests.quests.components.QuestLocation;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class QuestCompassManager {
	private static class ValidCompassEntry {
		private final QuestLocation mLocation;
		private final String mTitle;
		private final boolean mAllowTranslations;

		private ValidCompassEntry(QuestLocation loc, String title, boolean allowTranslations) {
			mLocation = loc;
			mTitle = title;
			mAllowTranslations = allowTranslations;
		}

		private void directPlayer(WaypointManager mgr, Player player) {
			MessagingUtils.sendRawMessage(player, mTitle + ": " + mLocation.getMessage(), mAllowTranslations);
			mgr.setWaypoint(player, mLocation);
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

	private final List<QuestCompass> mQuests = new ArrayList<QuestCompass>();
	private final Map<UUID, CompassCacheEntry> mCompassCache = new HashMap<UUID, CompassCacheEntry>();
	private final Map<Player, Integer> mCurrentIndex = new WeakHashMap<>();
	private final Plugin mPlugin;

	/* One command-specified waypoint per player */
	private final Map<UUID, ValidCompassEntry> mCommandWaypoints = new HashMap<UUID, ValidCompassEntry>();

	public QuestCompassManager(Plugin plugin) {
		mPlugin = plugin;
		reload(plugin, null);
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mQuests.clear();
		QuestUtils.loadScriptedQuests(plugin, "compass", sender, (object) -> {
			QuestCompass quest = new QuestCompass(object);
			mQuests.add(quest);
			return quest.getQuestName() + ":" + Integer.toString(quest.getMarkers().size());
		});
	}

	@SuppressWarnings("unchecked")
	private List<ValidCompassEntry> getCurrentMarkerTitles(Player player) {
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

				entries.add(new ValidCompassEntry(questMarkers.get(i), title, true));
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

				entries.add(new ValidCompassEntry(deathEntries.get(i), title, false));
			}
		}

		//Get command-based waypoint (limited to 1) for the player
		ValidCompassEntry commandWaypoint = mCommandWaypoints.get(player.getUniqueId());
		if (commandWaypoint != null) {
			entries.add(commandWaypoint);
		}

		// Cache this result for later
		mCompassCache.put(player.getUniqueId(), new CompassCacheEntry(player, entries));

		return entries;
	}

	private int showCurrentQuest(Player player, int index) {
		List<ValidCompassEntry> entries = getCurrentMarkerTitles(player);

		if (index >= entries.size()) {
			index = 0;
		}

		if (entries.size() == 0) {
			MessagingUtils.sendActionBarMessage(player, "You have no active quest.");
		} else {
			entries.get(index).directPlayer(mPlugin.mWaypointManager, player);
		}

		return index;
	}

	public void showCurrentQuest(Player player) {
		Integer index = mCurrentIndex.getOrDefault(player, 0);

		showCurrentQuest(player, index);
	}

	public void cycleQuestTracker(Player player) {
		Integer index = mCurrentIndex.getOrDefault(player, 0);
		index += 1;

		index = showCurrentQuest(player, index);

		mCurrentIndex.put(player, index);
	}

	/* One command-specified waypoint per player */
	public void setCommandWaypoint(Player player, List<Location> steps, String title, String message) {
		ValidCompassEntry entry = new ValidCompassEntry(new CompassLocation(null, message, steps), title, false);
		mCommandWaypoints.put(player.getUniqueId(), entry);
		getCurrentMarkerTitles(player);
		entry.directPlayer(mPlugin.mWaypointManager, player);
	}

	//Remove command-specified waypoint on a player
	public void removeCommandWaypoint(Player player) {
		if (mCommandWaypoints.containsKey(player.getUniqueId())) {
			mCommandWaypoints.remove(player.getUniqueId());
			getCurrentMarkerTitles(player);
			mPlugin.mWaypointManager.setWaypoint(player, null);
		}
	}
}
