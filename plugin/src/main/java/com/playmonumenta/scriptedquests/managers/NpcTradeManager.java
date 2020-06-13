package com.playmonumenta.scriptedquests.managers;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitRunnable;

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

	public void trade(Plugin plugin, Villager villager, Player player, PlayerInteractEntityEvent event) {
		List<MerchantRecipe> trades = null;
		if (villager.getCustomName() != null) {
			NpcTrader trader = mTraders.get(QuestNpc.squashNpcName(villager.getCustomName()));
			if (trader != null) {
				trades = trader.getPlayerTrades(plugin, villager, player);
			}
		}

		if (trades == null) {
			trades = villager.getRecipes();
		}

		/*
		 * If this villager still has trades, create a temporary fake merchant to interact with the player
		 * This allows multiple players to trade with the same NPC at the same time, and also gives score-limited trades
		 */
		if (trades.size() > 0) {
			List<MerchantRecipe> finalTrades = trades;
			new BukkitRunnable() {
				@Override
				public void run() {
					Merchant merchant = Bukkit.createMerchant(villager.getName());
					merchant.setRecipes(finalTrades);
					player.openMerchant(merchant, true);
				}
			}.runTaskLater(plugin, 1);
		}
		event.setCancelled(true);
	}
}

