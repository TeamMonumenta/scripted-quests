package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.races.Race;
import com.playmonumenta.scriptedquests.races.RaceFactory;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

public class RaceManager {
	public static final String PLAYER_RACE_TAG = "SQRacer";
	public static final String ARMOR_STAND_RACE_TAG = "RaceRing";
	public static final String ARMOR_STAND_ID_PREFIX_TAG = "RacerId_";

	private Plugin mPlugin;

	/*
	 * A race factory is an already-parsed JSON object that is used to build a race object for a player
	 */
	private final HashMap<String, RaceFactory> mRaceFactories = new HashMap<String, RaceFactory>();
	private final HashMap<UUID, Race> mActiveRaces = new HashMap<UUID, Race>();
	private BukkitRunnable mRunnable = null;

	public RaceManager(Plugin plugin) {
		mPlugin = plugin;
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mRaceFactories.clear();

		QuestUtils.loadScriptedQuests(plugin, "races", sender, (object) -> {
			// Load this file into a raceFactory
			RaceFactory racef = new RaceFactory(object, plugin, this);

			if (mRaceFactories.containsKey(racef.getLabel())) {
				throw new Exception(racef.getLabel() + "' already exists!");
			}

			mRaceFactories.put(racef.getLabel(), racef);
			return racef.getLabel();
		});

		/* Tick all the currently active races */
		if (mRunnable != null) {
			mRunnable.cancel();
		}
		mRunnable = new BukkitRunnable() {
			/* This temporary list is necessary to avoid ConcurrentModificationException's */
			private List<Race> mRaceTemp = new ArrayList<Race>();

			@Override
			public void run() {
				if (mActiveRaces.size() > 0) {
					mRaceTemp.clear();
					mRaceTemp.addAll(mActiveRaces.values());
					for (Race race : mRaceTemp) {
						race.tick();
					}
				}
			}
		};
		mRunnable.runTaskTimer(plugin, 0, 2);
	}

	public void removeIfNotActive(Entity entity) {
		Set<String> tags = entity.getScoreboardTags();
		if (tags.contains(ARMOR_STAND_RACE_TAG)) {
			UUID ownerId = null;
			for (String tag : tags) {
				if (tag.startsWith(ARMOR_STAND_ID_PREFIX_TAG)) {
					try {
						ownerId = UUID.fromString(tag.substring(ARMOR_STAND_ID_PREFIX_TAG.length()));
					} catch (Exception e) {
						mPlugin.getLogger().warning("Invalid Race ID tag on ring: " + tag);
					}
				}
			}
			if (!isRacing(ownerId)) {
				mPlugin.getLogger().fine("Removing stale race ring armor stand.");
				entity.remove();
			}
		}
	}

	public void onLogin(Player player) {
		if (!isRacing(player)) {
			player.removeScoreboardTag(PLAYER_RACE_TAG);
		}
	}

	public boolean isRacing(Player player) {
		return isRacing(player.getUniqueId());
	}

	public boolean isRacing(UUID playerId) {
		return mActiveRaces.containsKey(playerId);
	}

	public boolean isNotRacingOrAllowsDialogClick(Player player) {
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null) {
			return race.allowsDialogClick();
		}
		return true;
	}

	public boolean isNotRacingOrAllowsCode(Player player) {
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null) {
			return race.allowsCode();
		}
		return true;
	}

	public boolean isNotRacingOrAllowsClickables(Player player) {
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null) {
			return race.allowsClickables();
		}
		return true;
	}

	public boolean isNotRacingOrAllowsNpcInteraction(Player player) {
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null) {
			return race.allowsNpcInteraction();
		}
		return true;
	}

	public void cancelRace(Player player) {
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null) {
			race.lose();
		}
	}

	public void cancelRaceByDeath(Player player) {
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null && !race.isRingless()) {
			race.lose();
		}
	}

	public void winRace(Player player) {
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null) {
			race.win(race.getTimeElapsed());
		}
	}

	public void restartRaceByClick(Player player) {
		if (mActiveRaces.size() == 0) {
			return;
		}
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null && !race.isRingless() && !race.isStatless()) {
			race.restart();
		}
	}

	public void cancelRaceByClick(Player player) {
		if (mActiveRaces.size() == 0) {
			return;
		}
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null && !race.isRingless() && !race.isStatless()) {
			race.lose();
		}
	}

	public void cancelAllRaces() {
		for (Race race : mActiveRaces.values()) {
			race.abort();
		}
		mActiveRaces.clear();
	}

	public void sendLeaderboard(Player player, String raceLabel, int startLine) {
		RaceFactory raceFactory = mRaceFactories.get(raceLabel);
		if (raceFactory != null) {
			raceFactory.sendLeaderboard(player, startLine);
		}
	}

	/* This should ONLY be called by a Race that wants to remove itself! */
	public void removeRace(Player player) {
		mActiveRaces.remove(player.getUniqueId());
	}

	public void startRace(Player player, String raceLabel) {
		if (mActiveRaces.containsKey(player.getUniqueId())) {
			mPlugin.getLogger().severe("Attempted to start second race '" + raceLabel +
			                           "' for player '" + player.getName() + "'");
			player.sendMessage(ChatColor.RED + "You are already in a race!");
			return;
		}

		// Invalidate any active dialog for the player
		player.removeMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY, mPlugin);

		RaceFactory raceFactory = mRaceFactories.get(raceLabel);
		if (raceFactory == null) {
			mPlugin.getLogger().severe("Attempted to start nonexistent race '" + raceLabel + "'");
			return;
		}

		mActiveRaces.put(player.getUniqueId(), raceFactory.createRace(player));
	}
}
