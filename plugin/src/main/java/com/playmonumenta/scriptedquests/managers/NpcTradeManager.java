package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.trades.NpcTrader;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class NpcTradeManager {
	private final HashMap<String, NpcTrader> mTraders = new HashMap<String, NpcTrader>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		String tradersLocation = plugin.getDataFolder() + File.separator + "traders";
		ArrayList<File> listOfFiles;

		// Attempt to load all JSON files in subdirectories
		try {
			File directory = new File(tradersLocation);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			listOfFiles = FileUtils.getFilesInDirectory(tradersLocation, ".json");
		} catch (IOException e) {
			plugin.getLogger().severe("Caught exception trying to reload traders: " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload traders: " + e);
			}
			return;
		}

		Collections.sort(listOfFiles);
		for (File file : listOfFiles) {
			try {
				// Load this file into a NpcTrader object
				NpcTrader trader = new NpcTrader(file.getPath());

				if (mTraders.containsKey(trader.getNpcName())) {
					throw new Exception("Trader '" + trader.getNpcName() + "' already exists!");
				}

				mTraders.put(trader.getNpcName(), trader);
			} catch (Exception e) {
				plugin.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load traders file '" + file.getPath() + "'");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}

		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Loaded " + Integer.toString(mTraders.size()) + " traders");

			if (mTraders.size() <= 20) {
				String outMsg = "";
				for (String npc : mTraders.keySet()) {
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

	public NpcTradeManager(Plugin plugin) {
		reload(plugin, null);
	}

	public void setNpcTrades(Plugin plugin, Villager villager, Player player) {
		if (villager.getCustomName() != null) {
			NpcTrader trader = mTraders.get(QuestNpc.squashNpcName(villager.getCustomName()));
			if (trader != null) {
				trader.setNpcTrades(plugin, villager, player);
			}
		}
	}
}

