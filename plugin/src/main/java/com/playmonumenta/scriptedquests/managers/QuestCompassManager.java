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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class QuestCompassManager {

	private final List<QuestCompass> mQuests = new ArrayList<QuestCompass>();
	private final Map<UUID, CompassCacheEntry> mCompassCache = new HashMap<UUID, CompassCacheEntry>();
	public final Map<Player, Integer> mCurrentIndex = new WeakHashMap<>();
	private final Plugin mPlugin;

	/* One command-specified waypoint per player */
	private final Map<UUID, ValidCompassEntry> mCommandWaypoints = new HashMap<UUID, ValidCompassEntry>();

	public static class ValidCompassEntry {
		public final QuestLocation mLocation;
		public final String mTitle;
		public final boolean mAllowTranslations;
		public final CompassEntryType mType;
		public final int[] mMarkersIndex;

		private ValidCompassEntry(QuestLocation loc, String title, boolean allowTranslations, CompassEntryType type) {
			mLocation = loc;
			mTitle = title;
			mAllowTranslations = allowTranslations;
			mType = type;
			mMarkersIndex = new int[]{1, 1};
		}

		private ValidCompassEntry(QuestLocation loc, String title, boolean allowTranslations, CompassEntryType type, int[] markersIndex) {
			mLocation = loc;
			mTitle = title;
			mAllowTranslations = allowTranslations;
			mType = type;
			mMarkersIndex = markersIndex;
		}

		private void directPlayer(WaypointManager mgr, Player player, boolean isRemovable) {
			if (isRemovable) {
				MessagingUtils.sendClickableMessage(player, mTitle + ": " + mLocation.getMessage(), mAllowTranslations, "/waypoint remove @s", HoverEvent.showText(Component.text("Click to remove this waypoint.")));
			} else {
				MessagingUtils.sendRawMessage(player, mTitle + ": " + mLocation.getMessage(), mAllowTranslations);
			}
			if (!player.getWorld().getName().matches(mLocation.getWorldRegex())) {
				MessagingUtils.sendRawMessage(player, "&7(This location is on a &cdifferent world!&7 Find a way to the correct world before following the compass.)", mAllowTranslations);
			}

			mgr.setWaypoint(player, mLocation);
		}
	}

	public enum CompassEntryType {
		Quest(),
		Death(),
		Waypoint()
	}

	private static class CompassCacheEntry {
		public final int mLastRefresh;
		public final List<ValidCompassEntry> mEntries;

		private CompassCacheEntry(Player player, List<ValidCompassEntry> entries) {
			mLastRefresh = player.getTicksLived();
			mEntries = entries;
		}

		private boolean isStillValid(Player player) {
			return Math.abs(player.getTicksLived() - mLastRefresh) < 200;
		}
	}

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
	public List<ValidCompassEntry> getCurrentMarkerTitles(Player player) {
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
				String title = "§b§l" + quest.getQuestName() + "§r§b";

				if (questMarkers.size() > 1) {
					title += " [" + (i + 1) + "/" + questMarkers.size() + "]";
				}

				entries.add(new ValidCompassEntry(questMarkers.get(i), title, true, CompassEntryType.Quest, new int[]{i + 1, questMarkers.size()}));
			}
		}

		// Add player death locations
		if (player.hasMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY)) {
			List<DeathLocation> deathEntries =
			    (List<DeathLocation>)player.getMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY).get(0).value();
			for (int i = 0; i < deathEntries.size(); i++) {
				String title = "§c§lDeath§r§b";

				if (deathEntries.size() > 1) {
					title += " [" + (i + 1) + "/" + deathEntries.size() + "]";
				}

				entries.add(new ValidCompassEntry(deathEntries.get(i), title, false, CompassEntryType.Death, new int[]{i + 1, deathEntries.size()}));
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

	public int showCurrentQuest(Player player, int index) {
		List<ValidCompassEntry> entries = getCurrentMarkerTitles(player);

		if (index >= entries.size()) {
			index = 0;
			mCurrentIndex.put(player, index);
		}

		if (entries.isEmpty() || index == -1) {
			MessagingUtils.sendActionBarMessage(player, "You have no active quest.");
			mPlugin.mWaypointManager.setWaypoint(player, null);
		} else {
			entries.get(index).directPlayer(mPlugin.mWaypointManager, player, entries.get(index) == mCommandWaypoints.get(player.getUniqueId()));
		}

		return index;
	}

	public void showCurrentQuest(Player player) {
		Integer index = mCurrentIndex.getOrDefault(player, 0);

		showCurrentQuest(player, index);
	}

	public void cycleQuestTracker(Player player) {
		Integer index = mCurrentIndex.getOrDefault(player, 0);
		if (index < 0) {
			showCurrentQuest(player, index);
			return;
		}

		QuestCompassManager.CompassCacheEntry cacheEntryMap = mCompassCache.get(player.getUniqueId());
		if (cacheEntryMap != null) {
			ValidCompassEntry quest = cacheEntryMap.mEntries.get(index);
			if (quest.mMarkersIndex[0] == quest.mMarkersIndex[1]) {
				index += 1 - quest.mMarkersIndex[1];
			} else {
				index += 1;
			}
		}

		mCurrentIndex.put(player, showCurrentQuest(player, index));
		player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
	}

	/* One command-specified waypoint per player */
	public void setCommandWaypoint(Player player, List<Location> steps, String title, String message, String worldRegex) {
		invalidateCache(player);
		ValidCompassEntry entry = new ValidCompassEntry(new CompassLocation(null, message, steps, worldRegex), title, false, CompassEntryType.Waypoint);
		mCommandWaypoints.put(player.getUniqueId(), entry);
		mCurrentIndex.put(player, getCurrentMarkerTitles(player).indexOf(mCommandWaypoints.get(player.getUniqueId())));
		entry.directPlayer(mPlugin.mWaypointManager, player, true);
	}

	//Remove command-specified waypoint on a player
	public void removeCommandWaypoint(Player player) {
		if (mCommandWaypoints.containsKey(player.getUniqueId())) {
			mCommandWaypoints.remove(player.getUniqueId());
			invalidateCache(player);
			getCurrentMarkerTitles(player);
		}
	}

	public void invalidateCache(Player player) {
		mCompassCache.remove(player.getUniqueId());
	}
}
