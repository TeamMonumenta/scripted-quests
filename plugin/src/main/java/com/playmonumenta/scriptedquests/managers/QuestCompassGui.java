package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.managers.QuestCompassManager.ValidCompassEntry;
import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestCompassGui extends CustomInventory {
	private final Player mPlayer;
	private final QuestCompassManager mManager;
	private final int mRows = mInventory.getSize() / 9;
	private final int mGuidesSlot = 6;
	private final int mInfoSlot = 4;
	private final List<Integer> mDeathSlots = List.of(32 + 9 * (mRows - 4), 33 + 9 * (mRows - 4), 34 + 9 * (mRows - 4));
	private final int mCustomSlot = 30 + 9 * (mRows - 4);
	private final int mDeselectSlot = 28 + 9 * (mRows - 4);

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
		setupInventory();
	}

	private void setupInventory() {
		mInventory.clear();

		ItemStack info = new ItemStack(Material.COMPASS);
		ItemMeta infoMeta = info.getItemMeta();
		infoMeta.displayName(Component.text("Quest Compass", NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		infoMeta.lore(List.of(Component.text("Left click on a quest to track it.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("More instructions later", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		info.setItemMeta(infoMeta);
		mInventory.setItem(mInfoSlot, info);

		ItemStack guides = new ItemStack(Material.SCUTE);
		ItemMeta guidesMeta = guides.getItemMeta();
		guidesMeta.displayName(Component.text("Quest Guides", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		guidesMeta.lore(List.of(Component.text("Click to see available quests", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("across Monumenta by region and", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("town.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		guides.setItemMeta(guidesMeta);
		mInventory.setItem(mGuidesSlot, guides);

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
			Component.text("Quest Guides, /waypoint, or other special means.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		custom.setItemMeta(customMeta);
		mInventory.setItem(mCustomSlot, custom);

		ItemStack deselect = new ItemStack(Material.TNT_MINECART);
		ItemMeta deselectMeta = deselect.getItemMeta();
		deselectMeta.displayName(Component.text("Deselect Active Quest", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		deselectMeta.lore(List.of(Component.text("Click to deselect your currently tracked quest.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		deselect.setItemMeta(deselectMeta);
		mInventory.setItem(mDeselectSlot, deselect);

		List<ValidCompassEntry> quests = mManager.getCurrentMarkerTitles(mPlayer);

		for (int i = 0; i < quests.size(); i++) {
			if (i > 20) {
				return;
			}

			ValidCompassEntry quest = quests.get(i);

			ItemStack displayItem;

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

			int slot = i + 10;
			Material material;
			if (mManager.mCurrentIndex.getOrDefault(mPlayer, 0) == i) {
				switch (quest.mType) {
					default -> material = Material.ENCHANTED_BOOK;
					case Death -> material = Material.ARMOR_STAND;
					case Waypoint -> material = Material.KNOWLEDGE_BOOK;
				}
				itemName = itemName.append(Component.text(" [Currently Tracking]", selectedColor).decoration(TextDecoration.BOLD, false).decoration(TextDecoration.ITALIC, false));
			} else {
				switch (quest.mType) {
					default -> material = Material.BOOK;
					case Death -> material = Material.ARMOR_STAND;
					case Waypoint -> material = Material.KNOWLEDGE_BOOK;
				}
			}

			slot += (int) (2 * Math.floor((double) (slot - 8) / 9));
			if (quest.mType == QuestCompassManager.CompassEntryType.Death) {
				slot = 31 + quest.mMarkersIndex[0] + 9 * (mRows - 4);
			} else if (quest.mType == QuestCompassManager.CompassEntryType.Waypoint) {
				slot = 30 + 9 * (mRows - 4);
			}

			displayItem = new ItemStack(material);
			ItemMeta meta = displayItem.getItemMeta();
			meta.displayName(itemName);

			// Use this as a quest index tag for inventoryClick to easily identify quest index
			meta.setCustomModelData(i);

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

				boolean differentWorld = !com.playmonumenta.scriptedquests.Plugin.getInstance().mZoneManager.getWorldRegexMatcher().matches(mPlayer.getWorld().getName(), q.mLocation.getWorldRegex());
				Location qLoc = q.mLocation.getLocation();
				lore.add(Component.text((int) qLoc.getX() + ", " + (int) qLoc.getY() + ", " + (int) qLoc.getZ() + " ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
					.append(differentWorld ? Component.text("[Different World!]", NamedTextColor.DARK_RED) : Component.text(("("+ (int) mPlayer.getLocation().distance(new Location(mPlayer.getWorld(), qLoc.getX(), qLoc.getY(), qLoc.getZ())) + "m away)"))));

				if (q.mType == QuestCompassManager.CompassEntryType.Waypoint) {
					lore.add(Component.text("(Shift Click to remove this waypoint.)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				}
				if (j != i - quest.mMarkersIndex[0] + quest.mMarkersIndex[1]) {
					lore.add(Component.text(""));
				}
			}

			meta.lore(lore);
			displayItem.setItemMeta(meta);

			mInventory.setItem(slot, displayItem);
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		InventoryUtils.refreshOffhand(event);
		int slot = event.getSlot();
		ItemStack item = event.getInventory().getItem(slot);
		if (!(event.getWhoClicked() instanceof Player player)
			|| event.getClickedInventory() != mInventory
			|| item == null) {
			return;
		}
		if (slot == mGuidesSlot) {
			player.performCommand("sqgui show regionqg @s");
			return;
		} else if (slot == mInfoSlot) {
			return;
		} else if (slot == mDeselectSlot) {
			mManager.mCurrentIndex.put(player, mManager.showCurrentQuest(player, -1));
			player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 1f, 0.8f);
			close();
			return;
		} else if (slot == mCustomSlot && event.isShiftClick() && item.getType() == Material.KNOWLEDGE_BOOK) {
			mManager.removeCommandWaypoint(player);
			player.playSound(player.getLocation(), "minecraft:entity.armadillo.scute_drop", SoundCategory.PLAYERS, 1f, 1f);
			setupInventory();
			return;
		} else if (mDeathSlots.contains(slot) && item.getType() == Material.SKULL_POTTERY_SHERD) {
			return;
		} else if (slot == mCustomSlot && item.getType() == Material.HOPPER) {
			return;
		}

		int index = item.getItemMeta().getCustomModelData();
		mManager.mCurrentIndex.put(player, mManager.showCurrentQuest(player, index));
		player.playSound(player.getLocation(), Sound.UI_LOOM_TAKE_RESULT, SoundCategory.PLAYERS, 1f, 1f);
		if (slot == mCustomSlot) {
			player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 0.8f);
		}
		close();
	}
}

