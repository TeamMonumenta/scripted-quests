package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.trades.NpcTrade;
import com.playmonumenta.scriptedquests.trades.NpcTrader;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitRunnable;

public class NpcTradeManager implements Listener {
	private final HashMap<String, NpcTrader> mTraders = new HashMap<>();

	private static class PlayerTradeContext {
		private final Map<Integer, NpcTrade> mSlotProperties;
		private final Villager mVillager;
		private final Merchant mMerchant;

		private PlayerTradeContext(Map<Integer, NpcTrade> slotProperties, Villager villager, Merchant merchant) {
			mSlotProperties = slotProperties;
			mVillager = villager;
			mMerchant = merchant;
		}

		public Map<Integer, NpcTrade> getSlotProperties() {
			return mSlotProperties;
		}

		public Villager getVillager() {
			return mVillager;
		}

		public Merchant getMerchant() {
			return mMerchant;
		}
	}

	/*
	 * This tracks open trader windows for all players.
	 * A player can not interact with a Merchant-type inventory unless they are on this map
	 * The value map for each player tracks what special Trader specifications are for each slot, if any
	 */
	public final HashMap<UUID, PlayerTradeContext> mOpenTrades = new HashMap<>();

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

		Map<Integer, NpcTrade> slotProperties = new HashMap<>();

		// Iterate over the villager recipes and filter out what shouldn't be there
		// We need to tag the outer loop so inner loops can continue it
		recipes: for (int i = 0; i < villager.getRecipeCount(); i++) {
			MerchantRecipe recipe = villager.getRecipe(i);

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

			// Remove unmatched prereq trades
			if (trader != null) {
				NpcTrade trade = trader.getTrade(i);
				if (trade != null) {
					if (!trade.prerequisiteMet(new QuestContext(plugin, player, villager))) {
						if (lockedSlots.length() != 0) {
							lockedSlots.append(", ");
						}
						lockedSlots.append(i);
						modified = true;
						continue;
					} else {
						slotProperties.put(trades.size(), trade);
					}
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
			List<TradeWindowOpenEvent.Trade> eventTrades = new ArrayList<>();
			for (int i = 0; i < trades.size(); i++) {
				NpcTrade npcTrade = slotProperties.get(i);
				eventTrades.add(new TradeWindowOpenEvent.Trade(trades.get(i), npcTrade != null ? npcTrade.getActions() : null));
			}
			TradeWindowOpenEvent tradeEvent = new TradeWindowOpenEvent(player, eventTrades);
			Bukkit.getPluginManager().callEvent(tradeEvent);
			if (!tradeEvent.isCancelled() && !tradeEvent.getTrades().isEmpty()) {
				new BukkitRunnable() {
					@Override
					public void run() {
						Merchant merchant = Bukkit.createMerchant(villager.customName());
						final List<TradeWindowOpenEvent.Trade> newEventTrades = tradeEvent.getTrades();
						Map<Integer, NpcTrade> newSlotProperties = new HashMap<>();
						for (int i = 0; i < newEventTrades.size(); i++) {
							NpcTrade npcTrade = new NpcTrade(i, new QuestPrerequisites(), newEventTrades.get(i).getActions());
							newSlotProperties.put(i, npcTrade);
						}
						merchant.setRecipes(newEventTrades.stream().map(TradeWindowOpenEvent.Trade::getRecipe).collect(Collectors.toList()));
						mOpenTrades.put(player.getUniqueId(), new PlayerTradeContext(newSlotProperties, villager, merchant));
						player.openMerchant(merchant, true);
					}
				}.runTaskLater(plugin, 1);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (!event.getInventory().getType().equals(InventoryType.MERCHANT)) {
			return;
		}

		mOpenTrades.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.isCancelled() || event.getResult().equals(Event.Result.DENY) || !event.getInventory().getType().equals(InventoryType.MERCHANT) || !(event.getWhoClicked() instanceof Player)) {
			/* Already cancelled, or not a merchant inventory, or not a click by a player */
			return;
		}

		Player player = (Player)event.getWhoClicked();
		MerchantInventory merchInv = (MerchantInventory)event.getInventory();
		PlayerTradeContext context = mOpenTrades.get(player.getUniqueId());
		int hotbarButton = event.getHotbarButton();

		if (context == null || !merchInv.getMerchant().equals(context.getMerchant())) {
			player.sendMessage(ChatColor.RED + "DENIED: You should not have been able to view this interface. If this is a bug, please report it, and try trading with the villager again.");
			event.setCancelled(true);
			event.setResult(Event.Result.DENY);
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> player.closeInventory());
			return;
		}

		if (event.getSlot() != 2) {
			/*
			 * The player is interacting with a valid merchant inventory but not yet clicking on the resulting item slot
			 * Ignore the click
			 */
			return;
		}

		ItemStack clickedItem = event.getInventory().getItem(event.getSlot());
		if (hotbarButton != -1) {
			ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
			if ((hotbarItem == null || hotbarItem.getType().isAir()) && clickedItem != null && !clickedItem.getType().isAir()) {
				onSuccessfulTrade(event);
			}
		} else {
			// If they use the swap hands key on the trade item and their offhand is not empty, do not trigger a successful trade
			if (event.getClick().equals(ClickType.SWAP_OFFHAND) && player.getInventory().getItemInOffHand() != null && !player.getInventory().getItemInOffHand().getType().isAir()) {
				return;
			}
			if ((event.getCursor() == null || event.getCursor().getType().isAir()) && clickedItem != null && !clickedItem.getType().isAir()) {
				onSuccessfulTrade(event);
			}
		}
	}

	/* This is a successful trade - clicking with an empty cursor on a valid result item */
	private void onSuccessfulTrade(InventoryClickEvent event) {
		Player player = (Player)event.getWhoClicked();
		MerchantInventory merchInv = (MerchantInventory)event.getInventory();
		PlayerTradeContext context = mOpenTrades.get(player.getUniqueId());

		/*
		 * Have to manually compute which slot this was because merchInv.getSelectedIndex returns the wrong index
		 * if the player leaves the trade on the first slot but puts in materials for one of the other trades
		 */

		MerchantRecipe recipe = merchInv.getSelectedRecipe();
		List<MerchantRecipe> recipes = merchInv.getMerchant().getRecipes();
		int selectedIndex = recipes.indexOf(recipe);

		if (selectedIndex < 0) {
			player.sendMessage(ChatColor.YELLOW + "BUG! Somehow the recipe you selected couldn't be found. Please report this, and include which villager and what you were trading for");
		} else {
			NpcTrade trade = context.getSlotProperties().get(selectedIndex);
			if (trade != null) {
				trade.doActions(new QuestContext(Plugin.getInstance(), player, context.getVillager()));
			}
		}
	}
}

