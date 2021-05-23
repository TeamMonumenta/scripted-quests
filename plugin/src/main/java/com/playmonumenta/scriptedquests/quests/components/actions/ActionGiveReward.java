package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import me.Novalescent.Core;
import me.Novalescent.items.ItemTier;
import me.Novalescent.items.types.RPGItem;
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
import org.bukkit.entity.EntityType;
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

		private String bracketText(String str) {
			return ChatColor.DARK_GRAY + "[" + str + ChatColor.DARK_GRAY + "]";
		}

		public void openMenu(Plugin plugin, Entity npcEntity, Player player) {

			int xp = (int) (mAction.mXP + (PlayerData.getXPForLevel(mAction.mXPLevel) * mAction.mXPPercent));
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

				// Base rewards
				Collection<ItemStack> base = new ArrayList<>();
				if (mAction.mLootBasePath != null) {
					base = InventoryUtils.getLootTableContents(player, mAction.mLootBasePath, mAction.mRandom);
				} else {
					for (String str : mAction.mLootBaseList) {
						RPGItem rpgItem = Core.getInstance().mItemManager.getItem(str);
						if (rpgItem != null) {
							base.add(rpgItem.getItemStack());
						}
					}
				}

				populateItemBox(page, base, 10, false, null);

				// Pick rewards
				Collection<ItemStack> picks = new ArrayList<>();

				if (mAction.mLootPickPath != null) {
					picks = InventoryUtils.getLootTableContents(player, mAction.mLootPickPath, mAction.mRandom);
				} else {
					for (String str : mAction.mLootPickList) {
						RPGItem rpgItem = Core.getInstance().mItemManager.getItem(str);
						if (rpgItem != null) {
							picks.add(rpgItem.getItemStack());
						}
					}
				}

				populateItemBox(page, picks, 10, true,
					(ItemStack item) -> {
					mPicked = item;
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 2);
					openMenu(plugin, npcEntity, player);
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
					ItemStack claim = new ItemStack(Material.LIME_CONCRETE);
					ItemMeta claimMeta = claim.getItemMeta();
					claimMeta.setDisplayName(ChatColor.GREEN + "Claim Rewards");

					if (mPicked != null) {
						List<String> lore = new ArrayList<>();
						lore.add(ChatColor.WHITE + "Selected Item:");

						RPGItem rpgItem = Utils.getRPGItem(mPicked);
						if (rpgItem != null) {
							ItemTier tier = rpgItem.mTier;
							lore.add(tier.getSubColor() + " - x" + mPicked.getAmount() + " " + tier.getColor() + "[" + tier.getSubColor()
								+ rpgItem.mDisplayName + tier.getSubColor() + "]");
						} else {
							ItemMeta pickedMeta = mPicked.getItemMeta();
							lore.add(ChatColor.WHITE + " - x" + mPicked.getAmount() + " " + pickedMeta.getDisplayName());
						}
						claimMeta.setLore(lore);
					}

					claim.setItemMeta(claimMeta);

					MenuItem claimItem = new MenuItem(claim);
					Collection<ItemStack> finalBase = base;
					claimItem.setAction(new CodeSnip() {

						@Override
						public void run() {
							player.closeInventory();
							PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
							player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.15f);
							// Actions to run whenever the player accepts
							for (QuestComponent component : mAction.mComponents) {
								component.doActionsIfPrereqsMet(plugin, player, npcEntity);
							}

							if (xp > 0) {
								data.giveXP(xp);
								player.sendMessage(bracketText(Utils.getColor("#73deff") + "+"
									+ xp + " " + Utils.getColor("#93ff73") + "Experience Points"));
							}

							if (mAction.mCoin > 0) {
								data.mCoins += mAction.mCoin;
								data.updateCoinPurse();
								player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1, 2);
								player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1.5f);

								int[] coins = Utils.getCoinCounts(mAction.mCoin);
								String coinCounts = "";

								List<String> toJoin = new ArrayList<>();

								if (coins[2] > 0) {
									toJoin.add(Utils.getColor("#c5dde6") + "+" + coins[2] + " Platinum Coins");
								}

								if (coins[1] > 0) {
									toJoin.add(Utils.getColor("#ffd633") + "+" + coins[1] + " Gold Coins");
								}

								if (coins[0] > 0) {
									toJoin.add(Utils.getColor("#ffffff") + "+" + coins[0] + " Silver Coins");
								}

								coinCounts += String.join(", ", toJoin);
								player.sendMessage(bracketText(coinCounts));
							}

							if (finalBase.size() > 0) {
								InventoryUtils.giveItems(player, finalBase, false);

								for (ItemStack item : finalBase) {

									ItemStack converted = Utils.convertItemToRPG(item);
									ItemMeta meta = converted.getItemMeta();
									player.sendMessage(bracketText(ChatColor.AQUA + "+" + converted.getAmount() +
										" " + meta.getDisplayName()));
								}
							}

							if (mPicked != null) {
								Collection<ItemStack> picked = new HashSet<>();
								picked.add(mPicked);
								InventoryUtils.giveItems(player, picked, false);

								ItemMeta meta = mPicked.getItemMeta();
								player.sendMessage(bracketText(ChatColor.AQUA + "+" + mPicked.getAmount()
									+ " " + meta.getDisplayName()));
							}

						}

					});
					page.setMenuElement(claimItem, 50);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			// Coin
			int coins = mAction.mCoin;
			ItemStack whiteFiller = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
			ItemMeta whiteFillerMeta = whiteFiller.getItemMeta();
			whiteFillerMeta.setDisplayName(ChatColor.GRAY + "Empty Slot");
			whiteFiller.setItemMeta(whiteFillerMeta);
			if (coins > 0) {
				int[] coinCounts = Utils.getCoinCounts(coins);
				int platinumCoins = coinCounts[2];
				int goldCoins = coinCounts[1];
				int silverCoins = coinCounts[0];
				ItemStack coinIcon = new ItemStack(Material.SUNFLOWER);
				ItemMeta coinMeta = coinIcon.getItemMeta();
				coinMeta.setDisplayName(Utils.getColor("#8bff85") + ChatColor.UNDERLINE + "Coin Reward");

				List<String> lore = new ArrayList<>();
				String costString = "";
				if (platinumCoins > 0) {
					costString += Utils.getColor("#c5dde6") + platinumCoins + "p, ";
				}

				if (goldCoins > 0) {
					costString += Utils.getColor("#ffd633") + goldCoins + "g, ";
				}

				costString += Utils.getColor("#ffffff") + silverCoins + "s";

				lore.add(ChatColor.WHITE + "Coins: " + costString);
				coinMeta.setLore(lore);
				coinIcon.setItemMeta(coinMeta);

				MenuItem coinItem = new MenuItem(coinIcon);
				page.setMenuElement(coinItem, 31);
			} else {
				MenuItem coinItem = new MenuItem(whiteFiller);
				page.setMenuElement(coinItem, 31);
			}

			if (xp > 0) {
				ItemStack xpIcon = new ItemStack(Material.EXPERIENCE_BOTTLE);
				ItemMeta xpMeta = xpIcon.getItemMeta();
				xpMeta.setDisplayName(Utils.getColor("#4accff") + xp + " Experience Points");
				xpIcon.setItemMeta(xpMeta);

				MenuItem xpItem = new MenuItem(xpIcon);
				page.setMenuElement(xpItem, 22);
			} else {
				MenuItem coinItem = new MenuItem(whiteFiller);
				page.setMenuElement(coinItem, 22);
			}


			page.displayShop();
		}

		private void populateItemBox(MenuPage page, Collection<ItemStack> items, int startingSlot, boolean mirror, OnPick onPick) {

			int slot = startingSlot;
			int moved = 0;
			int row = 1;
			ItemStack filler = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
			ItemMeta fillerMeta = filler.getItemMeta();
			fillerMeta.setDisplayName(ChatColor.GRAY + "Empty Slot");
			filler.setItemMeta(fillerMeta);
			for (int i = 0; i < 8; i++) {

				MenuItem menuItem;
				if (items.size() <= i) {
					menuItem = new MenuItem(filler);
				} else {
					ItemStack item = Utils.convertItemToRPG(((ItemStack) items.toArray()[i]).clone());
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
								onPick.onPick(Utils.convertItemToRPG((ItemStack) items.toArray()[finalI1]));
							}
						});
					}

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
					slot = startingSlot + (row * 9);
					row++;
				}
			}

		}

	}

	private String mLootBasePath;
	private String mLootPickPath;
	private List<String> mLootBaseList = new ArrayList<>();
	private List<String> mLootPickList = new ArrayList<>();
	private Integer mXP;
	private Integer mCoin;

	// XP Percent
	private Integer mXPLevel;
	private double mXPPercent;

	private Random mRandom;
	private List<QuestComponent> mComponents = new ArrayList<>();
	public ActionGiveReward(String npcName, String displayName, EntityType entityType, JsonElement element) throws Exception {
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
				&& !key.equals("coins")
				&& !key.equals("xp_percent")
				&& !key.equals("quest_components")) {
				throw new Exception("Unknown give_reward key: " + key);
			}

			// All quest_components entries are single JSON things that should be passed
			// to their respective handlers
			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("give_reward value for key '" + key + "' is not parseable!");
			}

			if (key.equals("base_reward")) {
				if (value.isJsonPrimitive()) {
					mLootBasePath = value.getAsString();
					if (mLootBasePath == null) {
						throw new Exception("give_reward base_reward entry is not a string!");
					}
				} else {
					JsonArray array = value.getAsJsonArray();
					for (JsonElement itemEle : array) {
						mLootBaseList.add(itemEle.getAsString());
					}
				}

			} else if (key.equals("pick_reward")) {
				if (value.isJsonPrimitive()) {
					mLootPickPath = value.getAsString();
					if (mLootPickPath == null) {
						throw new Exception("give_reward pick_reward entry is not a string!");
					}
				} else {
					JsonArray array = value.getAsJsonArray();
					for (JsonElement itemEle : array) {
						mLootPickList.add(itemEle.getAsString());
					}
				}
			} else if (key.equals("xp")) {
				mXP = value.getAsInt();
				if (mXP == null) {
					throw new Exception("give_reward xp entry is not a integer!");
				}
			} else if (key.equals("xp_percent")) {
				if (!value.isJsonObject()) {
					throw new Exception("give_reward xp entry is not a integer!");
				}
				JsonObject xpObject = value.getAsJsonObject();
				mXPLevel = xpObject.get("level").getAsInt();
				mXPPercent = xpObject.get("xp_percent").getAsDouble();

			} else if (key.equals("coins")) {
				mCoin = value.getAsInt();
				if (mCoin == null) {
					throw new Exception("give_reward coins entry is not a integer!");
				}
			} else if (key.equals("quest_components")) {
				JsonArray components = value.getAsJsonArray();
				for (JsonElement ele : components) {
					QuestComponent component = new QuestComponent(npcName, displayName, entityType, ele);
					mComponents.add(component);
				}
			}
		}

		mRandom = new Random();

	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		RewardMenu menu = new RewardMenu(this);
		menu.openMenu(plugin, npcEntity, player);
	}

}
