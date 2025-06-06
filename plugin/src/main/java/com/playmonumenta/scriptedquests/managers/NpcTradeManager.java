package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.trades.NpcTrade;
import com.playmonumenta.scriptedquests.trades.NpcTradeOverride;
import com.playmonumenta.scriptedquests.trades.NpcTrader;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class NpcTradeManager implements Listener {
	private final HashMap<String, List<NpcTrader>> mTraders = new HashMap<>();

	private static class PlayerTradeContext {
		private final Map<Integer, NpcTrade> mSlotProperties;
		private final @Nullable Villager mVillager;
		private final Merchant mMerchant;

		private PlayerTradeContext(Map<Integer, NpcTrade> slotProperties, @Nullable Villager villager, Merchant merchant) {
			mSlotProperties = slotProperties;
			mVillager = villager;
			mMerchant = merchant;
		}

		public Map<Integer, NpcTrade> getSlotProperties() {
			return mSlotProperties;
		}

		public @Nullable Villager getVillager() {
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
	private final HashMap<UUID, PlayerTradeContext> mOpenTrades = new HashMap<>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mTraders.clear();
		QuestUtils.loadScriptedQuests(plugin, "traders", sender, (object, file) -> {
			// Load this file into a NpcTrader object
			NpcTrader trader = new NpcTrader(object, file);

			trader.getNpcNames().forEach(npcName -> mTraders.computeIfAbsent(npcName, key -> new ArrayList<>()).add(trader));

			return "" + trader.getNpcNames();
		});
	}

	public @Nullable NpcTrader reloadSingleTrader(NpcTrader trader, CommandSender sender) throws WrapperCommandSyntaxException {
		try {
			AtomicReference<NpcTrader> newTraderRef = new AtomicReference<>();
			QuestUtils.loadScriptedQuestsFile(trader.getFile(), (object, file) -> {
				NpcTrader newTrader = new NpcTrader(object, file);
				mTraders.values().forEach(ts -> ts.removeIf(t -> trader.getFile().getAbsoluteFile().equals(t.getFile().getAbsoluteFile())));
				newTrader.getNpcNames().forEach(npcName -> mTraders.computeIfAbsent(npcName, key -> new ArrayList<>()).add(newTrader));
				newTraderRef.set(newTrader);
				return null;
			});
			return newTraderRef.get();
		} catch (Exception e) {
			MessagingUtils.sendStackTrace(sender, e);
			throw CommandAPI.failWithString("Failed to reload trader: error in quest file '" + trader.getFile().getPath() + "'");
		}
	}

	public Collection<NpcTrader> getTraders() {
		return mTraders.values().stream().flatMap(Collection::stream).toList();
	}

	public Collection<String> getTraderNames() {
		return mTraders.keySet();
	}

	public @Nullable List<NpcTrader> getTrades(String npc) {
		return mTraders.get(QuestNpc.squashNpcName(npc));
	}

	/**
	 * Initiates a player trade with a villager. Instead of trading with the villager directly,
	 * we generate a fake merchant to do the trading. We override this trade event for multiple reasons:
	 * <ul>
	 * <li>This allows multiple players to trade with the same villager at the same time</li>
	 * <li>This way, the villager does not gain any trade experience</li>
	 * <li>We can filter out randomly-generated vanilla trades, and hide trades based on quest prerequisites</li>
	 * </ul>
	 *
	 * @param plugin   Used for metadata
	 * @param villager The villager to trade with
	 * @param player   The player trading
	 */
	public void trade(Plugin plugin, Villager villager, Player player) {
		List<NpcTrader> traderFiles = mTraders.getOrDefault(QuestNpc.squashNpcName(villager.getName()), Collections.emptyList());
		trade(plugin, traderFiles, villager, villager.customName(), player);
	}

	public void trade(Plugin plugin, Collection<NpcTrader> traderFiles, @Nullable Villager villager, Component title, Player player) {
		QuestContext context = new QuestContext(plugin, player, villager, false, null, player.getInventory().getItemInMainHand());
		traderFiles = traderFiles.stream().filter(trader -> trader.areFilePrerequisitesMet(context)).toList();

		ArrayList<MerchantRecipe> trades = new ArrayList<>();
		StringBuilder lockedSlots = new StringBuilder();
		StringBuilder vanillaSlots = new StringBuilder();
		boolean modified = false;

		Map<Integer, NpcTrade> slotProperties = new HashMap<>();

		if (villager != null) {
			// Iterate over the villager recipes and filter out what shouldn't be there
			// We need to tag the outer loop so inner loops can continue it
			recipes:
			for (int i = 0; i < villager.getRecipeCount(); i++) {
				MerchantRecipe recipe = villager.getRecipe(i);

				List<ItemStack> items = recipe.getIngredients();
				items.add(recipe.getResult());
				// Remove vanilla trades (those with a regular emerald in any slot)
				for (ItemStack item : items) {
					if (item != null
						&& item.getType() == Material.EMERALD
						&& !item.hasItemMeta()) {
						// Found emerald with no item data
						if (!vanillaSlots.isEmpty()) {
							vanillaSlots.append(", ");
						}
						vanillaSlots.append(i);
						modified = true;
						continue recipes;
					}
				}

				// Remove unmatched prereq trades
				for (NpcTrader trader : traderFiles) {
					NpcTrade trade = trader.getTrade(i);
					if (trade != null) {
						if (!trade.prerequisiteMet(context)) {
							if (!lockedSlots.isEmpty()) {
								lockedSlots.append(", ");
							}
							lockedSlots.append(i);
							modified = true;
							continue recipes;
						} else {
							List<ItemStack> overrideTradeItems = trade.getResolvedOverrideTradeItems(player);
							if (overrideTradeItems != null) {
								if (overrideTradeItems.stream().anyMatch(item -> item.getType() == Material.BARRIER)) {
									continue;
								}
								MerchantRecipe newRecipe = new MerchantRecipe(overrideTradeItems.get(2),
									recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(),
									recipe.getVillagerExperience(), recipe.getPriceMultiplier(), recipe.getDemand(),
									recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
								newRecipe.setIngredients(overrideTradeItems.subList(0, 2));
								recipe = newRecipe;
							}

							NpcTrade previousTrade = slotProperties.put(trades.size(), trade);
							if (previousTrade != null
								&& (trade.getActions() != null || overrideTradeItems != null || trade.getCount() > 0)
								&& (previousTrade.getActions() != null || previousTrade.getOverrideTradeItems() != null || previousTrade.getCount() > 0)) {
								MMLog.warning("Duplicate active non-prerequisite-only trade for villager '" + villager.getName() + "' at index " + trade.getIndex());
							}
						}
					}
				}

				// This trade was not filtered by any of the above checks. Add to the fake merchant
				trades.add(recipe);
			}
		}

		// check added trades
		TreeMap<Integer, NpcTrade> addedTrades = new TreeMap<>();
		for (NpcTrader trader : traderFiles) {
			for (NpcTrade trade : trader.getTrades()) {
				if ((villager == null || trade.getIndex() >= villager.getRecipeCount()) && trade.getOverrideTradeItems() != null) {
					NpcTrade previousTrade = addedTrades.put(trade.getIndex(), trade);
					if (previousTrade != null) {
						MMLog.warning("Duplicate added trade for villager '" + (villager == null ? "<unknown>" :
							villager.getName()) + "' at index " + trade.getIndex());
					}
				}
			}
		}
		// add new trades after real ones
		for (NpcTrade trade : addedTrades.values()) {
			if (!trade.prerequisiteMet(context)) {
				continue;
			}
			List<ItemStack> overrideTradeItems = Objects.requireNonNull(trade.getResolvedOverrideTradeItems(player));
            // must have overrides to get here
			if (overrideTradeItems.stream().anyMatch(item -> item != null && item.getType() == Material.BARRIER)) { //
                // safeguard against incomplete trades
				continue;
			}
			MerchantRecipe recipe = new MerchantRecipe(overrideTradeItems.get(2), 0, Integer.MAX_VALUE, false, 0,
				0f, 0, 0, true);
			recipe.setIngredients(overrideTradeItems.subList(0, 2));
			slotProperties.put(trades.size(), trade);
			trades.add(recipe);
		}

		// Set custom count if applicable
		Map<Integer, ItemStack> originalResults = new HashMap<>();
		for (int i = 0; i < trades.size(); i++) {
			MerchantRecipe recipe = trades.get(i);
			NpcTrade trade = slotProperties.get(i);
			if (trade != null) {
				int count = trade.getCount();
				if (count > 0) {
					ItemStack originalResult = recipe.getResult();
					originalResults.put(i, originalResult);

					ItemStack result = new ItemStack(originalResult);
					int maxStackSize = result.getMaxStackSize();

					String countString;
					if (maxStackSize > 1 && count % maxStackSize == 0) {
						countString = count / maxStackSize + " Stacks of ";
					} else {
						countString = count + " ";
					}

					result.setAmount(1);
					ItemMeta meta = result.getItemMeta();
					if (meta.hasDisplayName()) {
						meta.displayName(Component.text(countString, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).append(Objects.requireNonNull(meta.displayName())));
						result.setItemMeta(meta);
					}
					// make a new recipe with the replaced item
					MerchantRecipe newRecipe = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(),
						recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier(),
						recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
					newRecipe.setIngredients(recipe.getIngredients());
					trades.set(i, newRecipe);
				}
			}
		}

		if (modified && player.getGameMode() == GameMode.CREATIVE && player.isOp()) {
			player.sendMessage(Component.text("Some trader slots were not shown to you:", NamedTextColor.GOLD));
			if (!lockedSlots.isEmpty()) {
				player.sendMessage(Component.text("These slots were locked by quest scores: " + lockedSlots,
					NamedTextColor.GOLD));
			}
			if (!vanillaSlots.isEmpty()) {
				player.sendMessage(Component.text("These slots contained a vanilla emerald: " + vanillaSlots,
					NamedTextColor.GOLD));
			}
			player.sendMessage(Component.text("This message only appears to operators in creative mode",
				NamedTextColor.GOLD));
		}

		/*
		 * If this villager still has trades, create a temporary fake merchant to interact with the player
		 * This allows multiple players to trade with the same NPC at the same time, and also gives score-limited
		 * trades
		 */
		if (!trades.isEmpty()) {
			List<TradeWindowOpenEvent.Trade> eventTrades = new ArrayList<>();
			for (int i = 0; i < trades.size(); i++) {
				NpcTrade npcTrade = slotProperties.get(i);
				TradeWindowOpenEvent.Trade trade = new TradeWindowOpenEvent.Trade(trades.get(i), npcTrade);
				trade.setOriginalResult(originalResults.get(i));
				eventTrades.add(trade);
			}
			TradeWindowOpenEvent tradeEvent = new TradeWindowOpenEvent(player, villager, title, eventTrades);
			Bukkit.getPluginManager().callEvent(tradeEvent);
			if (!tradeEvent.isCancelled() && !tradeEvent.getTrades().isEmpty()) {
				new BukkitRunnable() {
					@Override
					public void run() {
						Merchant merchant = Bukkit.createMerchant(title);
						final List<TradeWindowOpenEvent.Trade> newEventTrades = tradeEvent.getTrades();
						Map<Integer, NpcTrade> newSlotProperties = new HashMap<>();
						for (int i = 0; i < newEventTrades.size(); i++) {
							NpcTrade npcTrade = new NpcTrade(i, new QuestPrerequisites(), newEventTrades.get(i));
							newSlotProperties.put(i, npcTrade);
						}
						merchant.setRecipes(newEventTrades.stream().map(TradeWindowOpenEvent.Trade::getRecipe).collect(Collectors.toList()));
						mOpenTrades.put(player.getUniqueId(), new PlayerTradeContext(newSlotProperties, villager,
							merchant));
						player.openMerchant(merchant, true);
					}
				}.runTaskLater(plugin, 1);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (!event.getInventory().getType().equals(InventoryType.MERCHANT)) {
			return;
		}

		mOpenTrades.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerPurchaseEvent(PlayerPurchaseEvent event) {
		// Runs once for every successful trade, ie can run multiple times within a single inventory click
		Player player = event.getPlayer();

		Inventory inv = player.getOpenInventory().getTopInventory();
		PlayerTradeContext context = mOpenTrades.get(player.getUniqueId());

		if (context == null || !(inv instanceof MerchantInventory merchInv) || !merchInv.getMerchant().equals(context.getMerchant())) {
			player.sendMessage(Component.text("DENIED: You should not have been able to view this interface. If this " +
				"is a bug, please report it, and try trading with the villager again.", NamedTextColor.RED));
			event.setCancelled(true);
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> player.closeInventory());
			return;
		}

		onSuccessfulTrade(event, merchInv);
	}

	/* This is a successful trade - clicking with an empty cursor on a valid result item */
	private void onSuccessfulTrade(PlayerPurchaseEvent event, MerchantInventory merchInv) {
		Player player = event.getPlayer();
		PlayerTradeContext context = mOpenTrades.get(player.getUniqueId());

		if (context == null) {
			player.sendMessage(Component.text("BUG! Trade context not found. Please report this, including which villager you were trading with and what you tried to buy.", NamedTextColor.RED));
			return;
		}

		/*
		 * Have to manually compute which slot this was because merchInv.getSelectedIndex returns the wrong index
		 * if the player leaves the trade on the first slot but puts in materials for one of the other trades
		 */

		MerchantRecipe recipe = event.getTrade();
		List<MerchantRecipe> recipes = merchInv.getMerchant().getRecipes();
		int selectedIndex = recipes.indexOf(recipe);

		if (selectedIndex < 0) {
			player.sendMessage(Component.text("BUG! Somehow the recipe you selected couldn't be found. Please report " +
				"this, and include which villager and what you were trading for", NamedTextColor.YELLOW));
		}

		NpcTrade trade = context.getSlotProperties().get(selectedIndex);
		if (trade == null) {
			player.sendMessage(Component.text("BUG! Somehow the trade you selected couldn't be found. Please report " +
				"this, and include which villager and what you were trading for", NamedTextColor.YELLOW));
			return;
		}

		trade.doActions(new QuestContext(Plugin.getInstance(), player, context.getVillager()));

		int count = trade.getCount();
		if (count > 0) {
			ItemStack result = trade.getOriginalResult();
			if (result != null) {
				int maxStackSize = result.getMaxStackSize();
				List<ItemStack> items = new ArrayList<>();
				while (count > 0) {
					int amount = count;
					if (amount > maxStackSize) {
						amount = maxStackSize;
					}
					ItemStack resultCopy = new ItemStack(result);
					resultCopy.setAmount(amount);
					items.add(resultCopy);

					count -= amount;
				}

				InventoryUtils.giveItems(player, items, false);
				event.setCancelled(true);
				ingredient:
				for (ItemStack recipeItem : recipe.getIngredients()) {
					int amountToDecrement = recipeItem.getAmount();
					for (ItemStack merchItem : merchInv) {
						if (merchItem != null && merchItem.isSimilar(recipeItem)) {
							int merchAmount = merchItem.getAmount();
							if (merchAmount >= amountToDecrement) {
								merchItem.setAmount(merchAmount - amountToDecrement);
								continue ingredient;
							} else {
								merchItem.setAmount(0);
								amountToDecrement -= merchAmount;
							}
						}
					}
				}
			}
		}
	}

	public void editTrader(NpcTrader trader, Player player) throws WrapperCommandSyntaxException {
		trader = reloadSingleTrader(trader, player);
		if (trader == null) {
			throw CommandAPI.failWithString("Failed to reload trader - trader is null after reload");
		}
		new TraderEditCustomInventory(player, trader).openInventory(player, Plugin.getInstance());
	}

	private static class TraderEditCustomInventory extends CustomInventory {

		private static final ItemStack UNDEFINED_ITEM = new ItemStack(Material.BARRIER);

		static {
			UNDEFINED_ITEM.editMeta(meta -> meta.displayName(Component.text("MISSING ITEM", NamedTextColor.DARK_RED)));
		}

		private final Player mPlayer;
		private final NpcTrader mTrader;
		private int mPage = 0;

		public TraderEditCustomInventory(Player player, NpcTrader trader) {
			super(player, 6 * 9,
				"Trades for " + trader.getOriginalNpcNames().get(0) + (trader.getOriginalNpcNames().size() > 1 ? " " +
					"(+" + (trader.getOriginalNpcNames().size() - 1) + " more)" : ""));
			this.mPlayer = player;
			mTrader = trader;
			setup();
		}

		/*
		 * Layout: one trade per column
		 * items per column:
		 * - info item
		 * - ingredient 1
		 * - ingredient 2
		 * - result
		 *
		 * last row has previous/next page icons at start and end (if applicable) and a help item in the center
		 */
		private void setup() {
			getInventory().clear();
			int totalTrades = mTrader.getTrades().size();
			List<NpcTrade> trades = mTrader.getTrades().stream().skip(mPage * 9L).limit(9).toList();
			for (int i = 0; i < trades.size(); i++) {
				NpcTrade trade = trades.get(i);
				ItemStack infoItem = new ItemStack(Material.JIGSAW);
				infoItem.editMeta(meta -> meta.displayName(Component.text("Trade #" + trade.getIndex())));
				getInventory().setItem(i, infoItem);

				List<NpcTradeOverride> overrideTradeItems = trade.getOverrideTradeItems();
				if (overrideTradeItems != null) {
					ItemStack ingredient1 = getDisplayItem(overrideTradeItems.get(0), mPlayer.getLocation());
					getInventory().setItem(9 + i, ingredient1);
					ItemStack ingredient2 = getDisplayItem(overrideTradeItems.get(1), mPlayer.getLocation());
					getInventory().setItem(2 * 9 + i, ingredient2);
					ItemStack result = getDisplayItem(overrideTradeItems.get(2), mPlayer.getLocation());
					getInventory().setItem(3 * 9 + i, result);
				}
			}

			ItemStack infoItem = new ItemStack(Material.DARK_OAK_SIGN);
			infoItem.editMeta(meta -> {
				meta.displayName(Component.text("Help"));
				meta.lore(List.of(
					Component.text("- Add new trades by editing the file, then open this GUI again"),
					Component.text("  (the file is reloaded automatically)"),
					Component.text("- The oder of items in trades is ingredients 1 and 2, then result")
				));
			});
			getInventory().setItem(5 * 9 + 4, infoItem);
			if (mPage > 0) {
				ItemStack item = new ItemStack(Material.ARROW);
				item.editMeta(meta -> meta.displayName(Component.text("Previous page")));
				getInventory().setItem(5 * 9, item);
			}
			if (mPage < totalTrades / 9) {
				ItemStack item = new ItemStack(Material.ARROW);
				item.editMeta(meta -> meta.displayName(Component.text("Next page")));
				getInventory().setItem(5 * 9 + 8, item);
			}
		}

		private ItemStack getDisplayItem(NpcTradeOverride override, Location someLocation) {
			ItemStack item = override.resolve(someLocation);
			if (override instanceof NpcTradeOverride.LootTableOverride lootTableOverride) {
				item.editMeta(meta -> {
					List<Component> oldLore = meta.lore();
					ArrayList<Component> lore = new ArrayList<>(oldLore == null ? List.of() : oldLore);
					lore.add(0, Component.text("Loot table: " + lootTableOverride.mLootTable.key().asString(),
						NamedTextColor.GOLD).decorate(TextDecoration.UNDERLINED));
					meta.lore(lore);
				});
			}
			return item;
		}

		@Override
		protected void inventoryClick(InventoryClickEvent event) {
			if (event.getClickedInventory() == getInventory()) {
				event.setCancelled(true);
				if (9 <= event.getSlot() && event.getSlot() < 4 * 9 && event.getClick() == ClickType.LEFT) {
					// clicked an ingredient/recipe row
					List<NpcTrade> trades = mTrader.getTrades().stream().toList();
					int index = mPage * 9 + event.getSlot() % 9;
					if (index < trades.size()) {
						NpcTrade trade = trades.get(index);
						int overrideSlot = event.getSlot() / 9 - 1;
						List<NpcTradeOverride> overrideTradeItems = trade.getOverrideTradeItems();
						List<NpcTradeOverride> empty = Arrays.asList(
							new NpcTradeOverride.ItemOverride(UNDEFINED_ITEM),
							new NpcTradeOverride.ItemOverride(new ItemStack(Material.AIR)),
							new NpcTradeOverride.ItemOverride(UNDEFINED_ITEM));
						if (overrideTradeItems == null) {
							overrideTradeItems = new ArrayList<>(empty);
							trade.setOverrideTradeItems(overrideTradeItems);
						}
						NpcTradeOverride oldOverride = overrideTradeItems.get(overrideSlot);
						if (oldOverride instanceof NpcTradeOverride.ItemOverride || oldOverride == null) {
							ItemStack oldItem = oldOverride != null ?
								((NpcTradeOverride.ItemOverride) oldOverride).mItem : null;
							overrideTradeItems.set(overrideSlot,
								new NpcTradeOverride.ItemOverride(event.getCursor() == null || event.getCursor().getType() == Material.AIR ? (overrideSlot == 1 ? new ItemStack(Material.AIR) : UNDEFINED_ITEM) : event.getCursor()));
							if (overrideTradeItems.equals(empty)) {
								trade.setOverrideTradeItems(null);
							}
							event.getView().setCursor(oldItem == null || oldItem.getType() == Material.AIR || oldItem.getType() == Material.BARRIER ? null : oldItem);
							setup();
						}
					}
				} else {
					if (event.getSlot() == 5 * 9 && mPage > 0) {
						mPage--;
						setup();
					} else if (event.getSlot() == 5 * 9 + 8 && mPage < mTrader.getTrades().size() / 9) {
						mPage++;
						setup();
					}
				}
			}
		}

		@Override
		protected void inventoryClose(InventoryCloseEvent event) {
			try {
				QuestUtils.save(Plugin.getInstance(), event.getPlayer(), mTrader.toJson(), mTrader.getFile());
				event.getPlayer().sendMessage(Component.text("Trade file updated.", NamedTextColor.GOLD));
			} catch (Exception e) {
				event.getPlayer().sendMessage(Component.text("Failed to update trade file.", NamedTextColor.RED));
				MessagingUtils.sendStackTrace(event.getPlayer(), e);
			}
		}

	}

}
