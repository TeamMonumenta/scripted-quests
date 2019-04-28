package com.playmonumenta.scriptedquests.managers;

import java.util.HashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.trades.NpcTrader;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

public class NpcTradeManager {
	private final HashMap<String, NpcTrader> mTraders = new HashMap<String, NpcTrader>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mTraders.clear();
		QuestUtils.loadScriptedQuests(plugin, "traders", sender, (object) -> {
			// Load this file into a NpcTrader object
			NpcTrader trader = new NpcTrader(object);

			if (mTraders.containsKey(trader.getNpcName())) {
				throw new Exception(trader.getNpcName() + "' already exists!");
			}

			mTraders.put(trader.getNpcName(), trader);

			return trader.getNpcName();
		});
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

