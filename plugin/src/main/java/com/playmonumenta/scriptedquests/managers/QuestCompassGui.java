package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.managers.QuestCompassManager.ValidCompassEntry;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestCompassGui extends CustomInventory {
	private final Player mPlayer;
	private final Plugin mPlugin = Plugin.getInstance();
	private final QuestCompassManager mManager;
	private final int mRows = mInventory.getSize() / 9;
	private final List<Integer> mDeathSlots = List.of(32 + 9 * (mRows - 4), 33 + 9 * (mRows - 4), 34 + 9 * (mRows - 4));
	private final int mCustomSlot = 30 + 9 * (mRows - 4);
	private final int mDeselectSlot = 28 + 9 * (mRows - 4);
	private final int mNextSlot = 44;
	private final int mPrevSlot = 36;
	private int mPage = 0;
	private final Map<ItemStack, ConfigurationSection> mItemToActions = new HashMap<>();
	private final String mNBTTag = "quest_index";

	public QuestCompassGui(Player player, QuestCompassManager manager) {
		super(player, Math.min(36 + 9 * (manager.getCurrentMarkerTitles(player).stream().filter(
				q -> q.mType != QuestCompassManager.CompassEntryType.Death && q.mType != QuestCompassManager.CompassEntryType.Waypoint).toList().size() / 7), 54),
			"Quest Compass");
		mPlayer = player;
		mManager = manager;
	}

	@Override
	public void openInventory(Player player, org.bukkit.plugin.Plugin owner) {
		super.openInventory(player, owner);
		player.playSound(player.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, SoundCategory.PLAYERS, 1f, 1f);
		setupInventory(mPage);
	}

	private void setupInventory(int page) {
		mInventory.clear();
		mPage = page;

		for (String key : mPlugin.mQuestCompassGUIItems.getKeys(false)) {
			ConfigurationSection itemConfig = mPlugin.mQuestCompassGUIItems.getConfigurationSection(key);
			if (itemConfig != null) {
				String name = itemConfig.getString("name");
				String materialName = itemConfig.getString("material");
				String nameColorHex = itemConfig.getString("name_color");
				String loreColorHex = itemConfig.getString("lore_color");

				name = name != null ? name : "Name Unset";
				Material material = materialName != null ? Material.getMaterial(materialName) : Material.BARRIER;
				if (material == null) {
					material = Material.BARRIER;
				}
				TextColor nameColor = nameColorHex != null ? TextColor.fromHexString(nameColorHex) : NamedTextColor.LIGHT_PURPLE;
				TextColor loreColor = loreColorHex != null ? TextColor.fromHexString(loreColorHex) : NamedTextColor.DARK_PURPLE;

				List<Component> lores = new ArrayList<>();
				ConfigurationSection loresConfig = itemConfig.getConfigurationSection("lore");
				if (loresConfig != null) {
					for (String line : loresConfig.getKeys(false)) {
						String text = loresConfig.getString(line);
						if (text == null) {
							text = "Lore unset";
						}
						lores.add(Component.text(text, loreColor).decoration(TextDecoration.ITALIC, false));
					}
				}

				int slot = itemConfig.getInt("slot");
				if (itemConfig.getBoolean("slot_scale_with_rows", false)) {
					slot += 9 * (mRows - 4);
				}

				ItemStack item = new ItemStack(material);
				ItemMeta itemMeta = item.getItemMeta();
				itemMeta.displayName(Component.text(name, nameColor).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
				itemMeta.lore(lores);
				item.setItemMeta(itemMeta);
				mInventory.setItem(slot, item);

				if (itemConfig.isConfigurationSection("actions")) {
					mItemToActions.put(item, itemConfig.getConfigurationSection("actions"));
				}
			}
		}

		ItemStack death = new ItemStack(Material.SKULL_POTTERY_SHERD);
		ItemMeta deathMeta = death.getItemMeta();
		deathMeta.displayName(Component.text("Death Waypoints", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		deathMeta.lore(List.of(Component.text("No recent death to show here.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		death.setItemMeta(deathMeta);
		mInventory.setItem(mDeathSlots.get(0), death);
		mInventory.setItem(mDeathSlots.get(1), death);
		mInventory.setItem(mDeathSlots.get(2), death);

		ItemStack custom = new ItemStack(Material.HOPPER);
		ItemMeta customMeta = custom.getItemMeta();
		customMeta.displayName(Component.text("Custom Waypoint", NamedTextColor.DARK_GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		customMeta.lore(List.of(Component.text("No custom waypoint set. Waypoints can be set by", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("the /waypoint command or other special means.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		custom.setItemMeta(customMeta);
		mInventory.setItem(mCustomSlot, custom);

		ItemStack deselect = new ItemStack(Material.TNT_MINECART);
		ItemMeta deselectMeta = deselect.getItemMeta();
		deselectMeta.displayName(Component.text("Stop Tracking", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		deselectMeta.lore(List.of(Component.text("Click to stop tracking the selected quest.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		deselect.setItemMeta(deselectMeta);
		mInventory.setItem(mDeselectSlot, deselect);

		List<ValidCompassEntry> quests = mManager.getCurrentMarkerTitles(mPlayer);

		if (quests.stream().filter(q -> q.mType == QuestCompassManager.CompassEntryType.Quest).toList().size() > 21 * (1 + mPage)) {
			ItemStack next = new ItemStack(Material.ARROW);
			ItemMeta nextMeta = next.getItemMeta();
			nextMeta.displayName(Component.text("Next Page", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			next.setItemMeta(nextMeta);
			mInventory.setItem(mNextSlot, next);
		}
		if (mPage > 0) {
			ItemStack prev = new ItemStack(Material.ARROW);
			ItemMeta prevMeta = prev.getItemMeta();
			prevMeta.displayName(Component.text("Previous Page", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
			prev.setItemMeta(prevMeta);
			mInventory.setItem(mPrevSlot, prev);
		}

		for (int i = 21 * mPage; i < quests.size(); i++) {
			ValidCompassEntry quest = quests.get(i);
			if (i >= 21 * (1 + mPage) && quest.mType == QuestCompassManager.CompassEntryType.Quest) {
				// Death and waypoint quests are sorted last - don't skip these, so they stay on every page
				continue;
			}

			NamedTextColor titleColor;
			NamedTextColor selectedColor = NamedTextColor.DARK_AQUA;
			if (quest.mType == QuestCompassManager.CompassEntryType.Death) {
				titleColor = NamedTextColor.RED;
			} else if (quest.mType == QuestCompassManager.CompassEntryType.Waypoint) {
				titleColor = NamedTextColor.GREEN;
			} else {
				titleColor = NamedTextColor.AQUA;
			}

			String title = quest.mTitle.replaceAll("&.", "");

			Component itemName = Component.text(title, titleColor).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false);

			int slot = i + 10 - 21 * mPage;
			Material material;
			if (mManager.mCurrentIndex.getOrDefault(mPlayer, 0) == i) {
				switch (quest.mType) {
					default -> material = Material.ENCHANTED_BOOK;
					case Death -> material = Material.TOTEM_OF_UNDYING;
					case Waypoint -> material = Material.BAMBOO_RAFT;
				}
				itemName = itemName.append(Component.text(" (Currently Tracking)", selectedColor).decoration(TextDecoration.BOLD, false).decoration(TextDecoration.ITALIC, false));
			} else {
				switch (quest.mType) {
					default -> material = Material.BOOK;
					case Death -> material = Material.ARMOR_STAND;
					case Waypoint -> material = Material.KNOWLEDGE_BOOK;
				}
			}

			slot += (int) (2 * Math.floor((double) (slot - 10) / 7));
			if (quest.mType == QuestCompassManager.CompassEntryType.Death) {
				slot = 31 + quest.mMarkersIndex[0] + 9 * (mRows - 4);
			} else if (quest.mType == QuestCompassManager.CompassEntryType.Waypoint) {
				slot = 30 + 9 * (mRows - 4);
			}

			ItemStack displayItem = new ItemStack(material);
			ItemMeta meta = displayItem.getItemMeta();
			meta.displayName(itemName);

			// Compile all steps and highlight the one that matches this quest index (j == i)
			List<Component> lore = new ArrayList<>();
			for (int j = i + 1 - quest.mMarkersIndex[0]; j < i + 1 - quest.mMarkersIndex[0] + quest.mMarkersIndex[1]; j++) {
				int splitIndex = 0;
				ValidCompassEntry q = quests.get(j);
				String qLore = q.mLocation.getMessage().replaceAll("&.", "");
				while (splitIndex < qLore.length() && !(j != i && splitIndex > 0)) {
					lore.add(Component.text(StringUtils.substring(qLore, splitIndex, qLore.indexOf(" ", splitIndex + 45) < 0 ? 1000 : qLore.indexOf(" ", splitIndex + 45)) + (j != i && qLore.indexOf(" ", splitIndex + 45) > 0 ? " {...}" : ""), j == i ? NamedTextColor.WHITE : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					if (qLore.indexOf(" ", splitIndex + 45) < 0) {
						splitIndex += 1000;
					} else {
						splitIndex = 1 + qLore.indexOf(" ", splitIndex + 45);
					}
				}
				boolean differentWorld = !mPlayer.getWorld().getName().matches(q.mLocation.getWorldRegex());
				Location qLoc = q.mLocation.getLocation();
				lore.add(Component.text((int) qLoc.getX() + ", " + (int) qLoc.getY() + ", " + (int) qLoc.getZ() + " ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
					.append(differentWorld ? Component.text("(Different World!)", NamedTextColor.RED) : Component.text(("("+ (int) mPlayer.getLocation().distance(new Location(mPlayer.getWorld(), qLoc.getX(), qLoc.getY(), qLoc.getZ())) + "m away)"))));
				if (q.mType == QuestCompassManager.CompassEntryType.Waypoint) {
					lore.add(Component.text("(Shift Click to remove this waypoint.)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				}
				if (j != i - quest.mMarkersIndex[0] + quest.mMarkersIndex[1]) {
					lore.add(Component.text(""));
				}
			}

			meta.lore(lore);
			displayItem.setItemMeta(meta);
			// Use NBT as a quest index tag for inventoryClick to easily identify quest index
			NBTItem nbtItem = new NBTItem(displayItem);
			nbtItem.setInteger(mNBTTag, i);
			displayItem = nbtItem.getItem();

			mInventory.setItem(slot, displayItem);
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		InventoryUtils.refreshOffhand(event);
		int slot = event.getSlot();
		if (slot < 0) {
			return;
		}
		ItemStack item = event.getInventory().getItem(slot);
		if (!(event.getWhoClicked() instanceof Player player)
			|| event.getClickedInventory() != mInventory
			|| item == null) {
			return;
		}
		NBTItem nbtItem = new NBTItem(item);
		if (mItemToActions.containsKey(item)) {
			String command = mItemToActions.get(item).getString("command");
			if (command != null) {
				NmsUtils.getVersionAdapter().runConsoleCommandSilently(command.replace("@S", mPlayer.getName()));
			}
			if (mItemToActions.get(item).getBoolean("close_gui", false)) {
				close();
			}
			return;
		} else if (slot == mDeselectSlot) {
			mManager.mCurrentIndex.put(player, mManager.showCurrentQuest(player, -1));
			player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 1f, 0.8f);
			close();
			return;
		} else if (slot == mCustomSlot && event.isShiftClick() && nbtItem.hasTag(mNBTTag)) {
			mManager.removeCommandWaypoint(player);
			player.playSound(player.getLocation(), "minecraft:entity.armadillo.scute_drop", SoundCategory.PLAYERS, 1f, 1f);
			setupInventory(mPage);
			return;
		} else if (slot == mNextSlot) {
			setupInventory(mPage + 1);
			player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
			return;
		} else if (slot == mPrevSlot) {
			setupInventory(mPage - 1);
			player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
			return;
		} else if (!nbtItem.hasTag(mNBTTag)) {
			// Item has no function and is not a quest
			return;
		}

		int index = nbtItem.getInteger(mNBTTag);
		mManager.mCurrentIndex.put(player, mManager.showCurrentQuest(player, index));
		player.playSound(player.getLocation(), Sound.UI_LOOM_TAKE_RESULT, SoundCategory.PLAYERS, 1f, 1f);
		if (slot == mCustomSlot) {
			player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 0.8f);
		}
		close();
	}
}

