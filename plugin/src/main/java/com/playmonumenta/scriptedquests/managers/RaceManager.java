package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.races.Race;
import com.playmonumenta.scriptedquests.races.RaceFactory;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RaceManager {
	private Plugin mPlugin;

	/*
	 * A race factory is an already-parsed JSON object that is used to build a race object for a player
	 */
	private HashMap<String, RaceFactory> mRaceFactories = new HashMap<String, RaceFactory>();
	private HashMap<UUID, Race> mActiveRaces = new HashMap<UUID, Race>();
	private BukkitRunnable mRunnable = null;

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mPlugin = plugin;
		String raceLocation = plugin.getDataFolder() + File.separator +  "races";
		mRaceFactories = new HashMap<String, RaceFactory>();
		ArrayList<File> listOfFiles;
		ArrayList<String> listOfRaceNames = new ArrayList<String>();
		int numRaces = 0;

		// Attempt to load all JSON files in subdirectories
		try {
			File directory = new File(raceLocation);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			listOfFiles = FileUtils.getFilesInDirectory(raceLocation, ".json");
		} catch (IOException e) {
			plugin.getLogger().severe("Caught exception trying to reload races: " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload races: " + e);
			}
			return;
		}

		Collections.sort(listOfFiles);
		for (File file : listOfFiles) {
			try {
				// Load this file into a raceFactory
				RaceFactory racef = new RaceFactory(file.getPath(), plugin, this);
				mRaceFactories.put(racef.getLabel(), racef);
				numRaces++;
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
			sender.sendMessage(ChatColor.GOLD + "Loaded " + Integer.toString(numRaces) + " races");

			if (numRaces <= 20) {
				Collections.sort(listOfRaceNames);
				String outMsg = "";
				for (String raceName : listOfRaceNames) {
					if (outMsg.isEmpty()) {
						outMsg = raceName;
					} else {
						outMsg = outMsg + ", " + raceName;
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

		/* Tick all the currently active races */
		if (mRunnable != null) {
			mRunnable.cancel();
		}
		mRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mActiveRaces.size() > 0) {
					for (Race race : mActiveRaces.values()) {
						race.tick();
					}
				}
			}
		};
		mRunnable.runTaskTimer(plugin, 0, 2);
	}

	public RaceManager(Plugin plugin) {
		reload(plugin, null);
	}

	public void cancelRace(Player player) {
		Race race = mActiveRaces.get(player.getUniqueId());
		if (race != null) {
			race.lose();
		}
	}

	public void sendLeaderboard(Player player, String raceLabel, int startLine) {
		RaceFactory raceFactory = mRaceFactories.get(raceLabel);
		if (raceFactory != null) {
			raceFactory.sendLeaderboard(player, startLine);
		}
	}

	/* This should ONLY be called by a Race that wants to remove itself! */
	public void __removeRace(Player player) {
		mActiveRaces.remove(player.getUniqueId());
	}

	public void startRace(Player player, String raceLabel) {
		if (mActiveRaces.containsKey(player.getUniqueId())) {
			mPlugin.getLogger().severe("Attempted to start second race '" + raceLabel +
			                           "' for player '" + player.getName() + "'");
			player.sendMessage(ChatColor.RED + "You are already in a race!");
			return;
		}

		RaceFactory raceFactory = mRaceFactories.get(raceLabel);
		if (raceFactory == null) {
			mPlugin.getLogger().severe("Attempted to start nonexistent race '" + raceLabel + "'");
			return;
		}

		mActiveRaces.put(player.getUniqueId(), raceFactory.createRace(player));
	}
}
