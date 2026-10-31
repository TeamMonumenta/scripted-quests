package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestCompass;
import com.playmonumenta.scriptedquests.quests.components.CompassLocation;
import com.playmonumenta.scriptedquests.quests.components.DeathLocation;
import com.playmonumenta.scriptedquests.quests.components.QuestLocation;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import com.playmonumenta.scriptedquests.utils.WorldRegexMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.Nullable;

public class QuestCompassManager implements Listener {

	private static @Nullable QuestCompassManager INSTANCE = null;
	private static final String CYCLE_ALL_QUESTS_TAG = "SQ_cycle_all_quests";

	private final List<QuestCompass> mQuests = new ArrayList<>();
	private final Map<UUID, CompassCacheEntry> mCompassCache = new HashMap<>();
	public final Map<UUID, Integer> mCurrentIndex = new WeakHashMap<>();
	private final Plugin mPlugin;
	private @Nullable WorldRegexMatcher mWorldRegexMatcher = null;

	/* One command-specified waypoint per player */
	private final Map<UUID, ValidCompassEntry> mCommandWaypoints = new HashMap<>();

	public static class ValidCompassEntry {
		public final QuestLocation mLocation;
		public final String mQuestName;
		public final String mTitle;
		public final boolean mAllowTranslations;
		public final CompassEntryType mType;
		public final int[] mMarkersIndex;

		private ValidCompassEntry(QuestLocation loc, String questName, String title, boolean allowTranslations, CompassEntryType type) {
			mLocation = loc;
			mQuestName = questName;
			mTitle = title;
			mAllowTranslations = allowTranslations;
			mType = type;
			mMarkersIndex = new int[]{1, 1};
		}

		private ValidCompassEntry(QuestLocation loc, String questName, String title, boolean allowTranslations, CompassEntryType type, int[] markersIndex) {
			mLocation = loc;
			mQuestName = questName;
			mTitle = title;
			mAllowTranslations = allowTranslations;
			mType = type;
			mMarkersIndex = markersIndex;
		}

		private void directPlayer(WaypointManager mgr, Player player, boolean isRemovable) {
			if (isRemovable) {
				MessagingUtils.sendClickableMessage(player, mTitle + ": " + mLocation.getMessage(), mAllowTranslations, "/waypoint remove @s", HoverEvent.showText(Component.text("Click to remove this waypoint.")));
			} else {
				MessagingUtils.sendRawMessage(player, mTitle + ": " + mLocation.getMessage(), false, mAllowTranslations);
			}

			mgr.setWaypoint(player, mLocation);
		}
	}

	public enum CompassEntryType {
		Quest(),
		Death(),
		Waypoint()
	}

	private record CompassCacheEntry(int mLastRefresh, List<ValidCompassEntry> mEntries) {
			private CompassCacheEntry(Player player, List<ValidCompassEntry> entries) {
				this(player.getTicksLived(), entries);
			}

			private boolean isStillValid(Player player) {
				return Math.abs(player.getTicksLived() - mLastRefresh) < 200;
			}
		}

	public QuestCompassManager(Plugin plugin) {
		INSTANCE = this;
		mPlugin = plugin;
		reload(plugin, null);
	}

	public static QuestCompassManager getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("Attempted to access QuestCompassManager before it loaded.");
		}
		return INSTANCE;
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mQuests.clear();
		QuestUtils.loadScriptedQuests(plugin, "compass", sender, (object) -> {
			QuestCompass quest = new QuestCompass(object);
			mQuests.add(quest);
			return quest.getQuestName() + ":" + quest.getMarkers().size();
		});

		Set<String> worldRegexes = new HashSet<>();
		for (QuestCompass quest : mQuests) {
			for (CompassLocation loc : quest.getMarkers()) {
				worldRegexes.add(loc.getWorldRegex());
			}
		}
		try {
			mWorldRegexMatcher = new WorldRegexMatcher(worldRegexes);
		} catch (Exception e) {
			MMLog.severe("Failed to build WorldRegexMatcher for compass", e);
			mWorldRegexMatcher = null;
		}
	}

	public @Nullable WorldRegexMatcher getWorldRegexMatcher() {
		return mWorldRegexMatcher;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void worldLoadEvent(WorldLoadEvent event) {
		WorldRegexMatcher matcher = mWorldRegexMatcher;
		if (matcher != null) {
			matcher.onLoadWorld(event.getWorld());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		WorldRegexMatcher matcher = mWorldRegexMatcher;
		if (matcher != null) {
			matcher.onUnloadWorld(event.getWorld());
		}
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
		List<ValidCompassEntry> entries = new ArrayList<>();
		for (QuestCompass quest : mQuests) {
			List<CompassLocation> questMarkers = quest.getMarkers(player);

			// Add all the valid markers/titles to the list
			for (int i = 0; i < questMarkers.size(); i++) {
				String title = "§b§l" + quest.getQuestName() + "§r§b";

				if (questMarkers.size() > 1) {
					title += " [" + (i + 1) + "/" + questMarkers.size() + "]";
				}

				entries.add(new ValidCompassEntry(questMarkers.get(i), quest.getQuestName(), title, true, CompassEntryType.Quest, new int[]{i + 1, questMarkers.size()}));
			}
		}

		// Add player death locations
		if (player.hasMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY)) {
			List<DeathLocation> deathEntries =
			    (List<DeathLocation>) player.getMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY).getFirst().value();
			if (deathEntries != null) {
				int i = 0;
				for (DeathLocation deathEntry : deathEntries) {
					String title = "§c§lDeath§r§b";

					if (deathEntries.size() > 1) {
						title += " [" + (i + 1) + "/" + deathEntries.size() + "]";
					}

					entries.add(new ValidCompassEntry(deathEntry, "", title, false, CompassEntryType.Death, new int[]{
						i + 1,
						deathEntries.size()
					}));
					i++;
				}
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
			mCurrentIndex.put(player.getUniqueId(), index);
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
		Integer index = mCurrentIndex.getOrDefault(player.getUniqueId(), 0);

		showCurrentQuest(player, index);
	}

	public boolean checkPlayerCyclesAllQuests(Player player) {
		return player.getScoreboardTags().contains(CYCLE_ALL_QUESTS_TAG);
	}

	public void setPlayerCyclesAllQuests(Player player, boolean value) {
		if (value) {
			player.addScoreboardTag(CYCLE_ALL_QUESTS_TAG);
		} else {
			player.removeScoreboardTag(CYCLE_ALL_QUESTS_TAG);
		}
	}

	public void cycleQuestTracker(Player player) {
		int oldIndex = mCurrentIndex.getOrDefault(player.getUniqueId(), 0);
		if (oldIndex < 0) {
			showCurrentQuest(player, oldIndex);
			return;
		}
		int newIndex = oldIndex;
		QuestCompassManager.CompassCacheEntry cacheEntryMap = mCompassCache.get(player.getUniqueId());
		if (cacheEntryMap != null && !cacheEntryMap.mEntries.isEmpty()) {
			if (checkPlayerCyclesAllQuests(player)) {
				newIndex = (newIndex + 1) % cacheEntryMap.mEntries.size();
			} else {
				if (newIndex >= cacheEntryMap.mEntries.size()) {
					newIndex = 0;
				}
				ValidCompassEntry quest = cacheEntryMap.mEntries.get(newIndex);
				if (quest.mMarkersIndex[0] == quest.mMarkersIndex[1]) {
					newIndex += 1 - quest.mMarkersIndex[1];
				} else {
					newIndex += 1;
				}
			}
		}
		if (oldIndex != newIndex) {
			// Only play page turn sound if the entry switched
			mCurrentIndex.put(player.getUniqueId(), showCurrentQuest(player, newIndex));
			player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
		} else {
			showCurrentQuest(player, newIndex);
		}
	}

	/* One command-specified waypoint per player */
	public void setCommandWaypoint(Player player, List<Location> steps, String title, String message, String worldRegex) {
		invalidateCache(player);
		ValidCompassEntry entry = new ValidCompassEntry(new CompassLocation(null, message, steps, worldRegex), "", title, false, CompassEntryType.Waypoint);
		mCommandWaypoints.put(player.getUniqueId(), entry);
		mCurrentIndex.put(player.getUniqueId(), getCurrentMarkerTitles(player).indexOf(mCommandWaypoints.get(player.getUniqueId())));
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
