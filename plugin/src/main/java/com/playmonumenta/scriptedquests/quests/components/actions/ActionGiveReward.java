package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.utils.CodeSnip;
import me.Novalescent.utils.Utils;
import me.Novalescent.utils.menus.MenuItem;
import me.Novalescent.utils.menus.MenuPage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.swing.*;
import java.util.*;

public class ActionGiveReward implements ActionBase {

	interface OnPick {

		void onPick(ItemStack item);

	}

	private class RewardMenu {

		private ItemStack mPicked = null;
		private ActionGiveReward mAction;

		public RewardMenu(ActionGiveReward reward) {
			mAction = reward;
		}

		public void openMenu(Player player) {
			MenuPage page = new MenuPage(player, "Claim Rewards", 54);

			ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
			ItemMeta fillerMeta = filler.getItemMeta();
			fillerMeta.setDisplayName(org.bukkit.ChatColor.RED + "");
			filler.setItemMeta(fillerMeta);

			page.setMenuElementDiagram(new MenuItem(filler), new Boolean[][]{
				{true, true, true, true, true, true, true, true, true},
				{true, false, false, true, true, true, false, false, true},
				{true, false, false, true, false, true, false, false, true},
				{true, false, false, true, false, true, false, false, true},
				{true, false, false, true, true, true, false, false, true},
				{true, true, true, true, true, true, true, true, true},
			});

			try {
				Collection<ItemStack> base = InventoryUtils.getLootTableContents(player, mAction.mLootBasePath, mAction.mRandom);
				// Base rewards
				populateItemBox(page, base, 10, false, null);

				Collection<ItemStack> picks = InventoryUtils.getLootTableContents(player, mAction.mLootPickPath, mAction.mRandom);
				// Pick rewards
				populateItemBox(page, picks, 10, true,
					(ItemStack item) -> {
					mPicked = item;
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 2);
					openMenu(player);
				});

				// Exit
				ItemStack exit = new ItemStack(Material.RED_CONCRETE);
				ItemMeta exitMeta = exit.getItemMeta();
				exitMeta.setDisplayName(ChatColor.RED + "Exit");
				exit.setItemMeta(exitMeta);

				MenuItem exitItem = new MenuItem(exit);
				exitItem.setAction(new CodeSnip() {
					@Override
					public void run() {
						player.closeInventory();
					}
				});
				page.setMenuElement(exitItem, 48);

				if (picks.size() > 0 && mPicked == null) {
					// Cannot accept
					ItemStack claim = new ItemStack(Material.YELLOW_CONCRETE);
					ItemMeta claimMeta = claim.getItemMeta();
					claimMeta.setDisplayName(ChatColor.GOLD + "Select an item on the right");
					claim.setItemMeta(claimMeta);

					MenuItem claimItem = new MenuItem(claim);
					page.setMenuElement(claimItem, 50);
				} else {

					// Accept
					ItemStack claim = new ItemStack(Material.GREEN_CONCRETE);
					ItemMeta claimMeta = claim.getItemMeta();
					claimMeta.setDisplayName(ChatColor.GREEN + "Claim items");
					claim.setItemMeta(claimMeta);

					MenuItem claimItem = new MenuItem(claim);
					claimItem.setAction(new CodeSnip() {

						@Override
						public void run() {
							player.closeInventory();
							InventoryUtils.giveItems(player, base, false);

							if (mPicked != null) {
								Collection<ItemStack> picked = new HashSet<>();
								picked.add(mPicked);
								InventoryUtils.giveItems(player, picked, false);
							}
							PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());

							if (mAction.mCoin > 0) {
								data.mCoins += mAction.mCoin;
								data.updateCoinPurse();
								player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1, 2);
								player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1.5f);
							}

							data.giveXP(mAction.mXP);
						}

					});
					page.setMenuElement(claimItem, 50);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			page.displayShop();
		}

		private void populateItemBox(MenuPage page, Collection<ItemStack> items, int startingSlot, boolean mirror, OnPick onPick) {

			int slot = startingSlot;
			int moved = 0;
			ItemStack filler = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
			ItemMeta fillerMeta = filler.getItemMeta();
			fillerMeta.setDisplayName(ChatColor.GRAY + "Empty Slot");
			filler.setItemMeta(fillerMeta);
			for (int i = 0; i < 8; i++) {

				MenuItem menuItem;
				if (items.size() <= i) {
					menuItem = new MenuItem(filler);
				} else {
					ItemStack item = ((ItemStack) items.toArray()[i]).clone();
					menuItem = new MenuItem(item);
					if (onPick != null) {
						ItemMeta meta = item.getItemMeta();
						List<String> lore = new ArrayList<>();
						if (meta.hasLore()) {
							lore = meta.getLore();
						}

						lore.add("");

						if (mPicked != null && mPicked.isSimilar(item)) {
							item.addUnsafeEnchantment(Enchantment.PROTECTION_FALL, 1);
							lore.add(ChatColor.GREEN + "Selected item");
						} else {
							lore.add(ChatColor.YELLOW + "Click to pick this item");
						}

						int finalI1 = i;
						menuItem.setAction(new CodeSnip() {

							@Override
							public void run() {

								// Returns the actual item
								onPick.onPick((ItemStack) items.toArray()[finalI1]);
							}
						});
					}

					page.setMenuElement(menuItem, slot);
				}

				if (mirror) {
					page.setMenuElement(menuItem, Utils.getMirrorSlot(slot));
				} else {
					page.setMenuElement(menuItem, slot);
				}

				slot++;
				moved++;
				if (moved > 1) {
					moved = 0;
					slot = startingSlot + 9;
				}
			}

		}

	}

	private String mLootBasePath;
	private String mLootPickPath;
	private Integer mXP;
	private Integer mCoin;

	private Random mRandom;
	public ActionGiveReward(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();

		if (object == null) {
			throw new Exception("give_reward value is not an object!");
		}

		Set<Map.Entry<String, JsonElement>> entries = object.entrySet();
		for (Map.Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("base_reward")
				&& !key.equals("pick_reward")
				&& !key.equals("xp")
				&& !key.equals("coins")) {
				throw new Exception("Unknown give_reward key: " + key);
			}

			// All quest_components entries are single JSON things that should be passed
			// to their respective handlers
			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("give_reward value for key '" + key + "' is not parseable!");
			}

			if (key.equals("base_reward")) {
				mLootBasePath = value.getAsString();
				if (mLootBasePath == null) {
					throw new Exception("give_reward base_reward entry is not a string!");
				}
			} else if (key.equals("pick_reward")) {
				mLootPickPath = value.getAsString();
				if (mLootPickPath == null) {
					throw new Exception("give_reward pick_reward entry is not a string!");
				}
			} else if (key.equals("xp")) {
				mXP = value.getAsInt();
				if (mXP == null) {
					throw new Exception("give_reward xp entry is not a integer!");
				}
			} else if (key.equals("coins")) {
				mCoin = value.getAsInt();
				if (mCoin == null) {
					throw new Exception("give_reward coins entry is not a integer!");
				}
			}
		}

		mRandom = new Random();

	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		MenuPage page = new MenuPage(player, "Claim Rewards", 54);

		ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta fillerMeta = filler.getItemMeta();
		fillerMeta.setDisplayName(org.bukkit.ChatColor.RED + "");
		filler.setItemMeta(fillerMeta);

		page.setMenuElementDiagram(new MenuItem(filler), new Boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, false, true, true, true, false, false, true},
			{true, false, false, true, false, true, false, false, true},
			{true, false, false, true, false, true, false, false, true},
			{true, false, false, true, true, true, false, false, true},
			{true, true, true, true, true, true, true, true, true},
		});

		page.displayShop();
	}

	private void populateItemBox(MenuPage page, List<ItemStack> items, int startingSlot, boolean mirror, OnPick onPick) {

		int slot = startingSlot;
		int moved = 0;
		ItemStack filler = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
		ItemMeta fillerMeta = filler.getItemMeta();
		fillerMeta.setDisplayName(ChatColor.GRAY + "Empty Slot");
		filler.setItemMeta(fillerMeta);
		for (int i = 0; i < 8; i++) {

			if (items.size() <= i) {
				page.setMenuElement(new MenuItem(filler), slot);
			} else {
				ItemStack item = items.get(i);
				MenuItem menuItem = new MenuItem(item);
				if (onPick != null) {

				}
			}

			slot++;
			moved++;
			if (moved > 1) {
				moved = 0;
				slot = startingSlot + 9;
			}
		}

	}
}
