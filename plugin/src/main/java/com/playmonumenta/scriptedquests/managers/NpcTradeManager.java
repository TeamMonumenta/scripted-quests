package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.trades.NpcTrade;
import com.playmonumenta.scriptedquests.trades.NpcTrader;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

public class NpcTradeManager {
	private final HashMap<String, NpcTrader> mTraders = new HashMap<>();

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

	/**
	 * Initiates a player trade with a villager. Instead of trading with the villager directly,
	 * we generate a fake merchant to do the trading. We override this trade event for multiple reasons:
	 * <li>This allows multiple players to trade with the same villager at the same time</li>
	 * <li>This way, the villager does not gain any trade experience</li>
	 * <li>We can filter out randomly-generated vanilla trades, and hide trades based on quest prerequisites</li>
	 * @param plugin Used for metadata
	 * @param villager The villager to trade with
	 * @param player The player trading
	 * @param event The interact event that initiated the trade
	 */
	public void trade(Plugin plugin, Villager villager, Player player, PlayerInteractEntityEvent event) {
		ArrayList<MerchantRecipe> trades = new ArrayList<>();
		NpcTrader trader = mTraders.get(QuestNpc.squashNpcName(villager.getName()));
		StringBuilder lockedSlots = new StringBuilder();
		StringBuilder vanillaSlots = new StringBuilder();
		boolean modified = false;

		// We don't want any vanilla trades to occur, regardless of if trades were changed or not.
		// As a side-effect, right-clicking a villager will not activate interactables
		// This is fine for now, but if we ever want interactables to work on villagers, we need to change this
		event.setCancelled(true);

		// Iterate over the villager recipes and filter out what shouldn't be there
		// We need to tag the outer loop so inner loops can continue it
		recipes: for (int i = 0; i < villager.getRecipeCount(); i++) {
			MerchantRecipe recipe = villager.getRecipe(i);
			// Remove unmatched prereq trades
			if (trader != null) {
				NpcTrade trade = trader.getTrade(i);
				if (trade != null && !trade.prerequisiteMet(player, villager)) {
					if (lockedSlots.length() != 0) {
						lockedSlots.append(", ");
					}
					lockedSlots.append(i);
					modified = true;
					continue;
				}
			}
			// Remove vanilla trades (those with a regular emerald in any slot)
			List<ItemStack> items = recipe.getIngredients();
			items.add(recipe.getResult());
			for (ItemStack item : items) {
				if (item != null
				    && item.getType() == Material.EMERALD
				    && !item.hasItemMeta()) {
					// Found emerald with no item data
					if (vanillaSlots.length() != 0) {
						vanillaSlots.append(", ");
					}
					vanillaSlots.append(i);
					modified = true;
					continue recipes;
				}
			}
			// This trade was not filtered by any of the above checks. Add to the fake merchant
			trades.add(recipe);
		}

		if (modified && player.getGameMode() == GameMode.CREATIVE && player.isOp()) {
			player.sendMessage(ChatColor.GOLD + "Some trader slots were not shown to you:");
			if (lockedSlots.length() > 0) {
				player.sendMessage(ChatColor.GOLD + "These slots were locked by quest scores: " + lockedSlots);
			}
			if (vanillaSlots.length() > 0) {
				player.sendMessage(ChatColor.GOLD + "These slots contained a vanilla emerald: " + vanillaSlots);
			}
			player.sendMessage(ChatColor.GOLD + "This message only appears to operators in creative mode");
		}

		/*
		 * If this villager still has trades, create a temporary fake merchant to interact with the player
		 * This allows multiple players to trade with the same NPC at the same time, and also gives score-limited trades
		 */
		if (trades.size() > 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Merchant merchant = Bukkit.createMerchant(villager.getName());
					merchant.setRecipes(trades);
					player.openMerchant(merchant, true);
				}
			}.runTaskLater(plugin, 1);
		}
	}
}

