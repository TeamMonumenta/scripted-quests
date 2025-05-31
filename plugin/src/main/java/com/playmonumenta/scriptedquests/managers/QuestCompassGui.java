package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.managers.QuestCompassManager.ValidCompassEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestCompassGui extends CustomInventory {
	private final Player mPlayer;
	private final QuestCompassManager mManager;

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
		int rows = mInventory.getSize() / 9;

		ItemStack info = new ItemStack(Material.COMPASS);
		ItemMeta infoMeta = info.getItemMeta();
		infoMeta.displayName(Component.text("Quest Compass", NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		infoMeta.lore(List.of(Component.text("Left click on a quest to track it.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("More instructions later", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		info.setItemMeta(infoMeta);
		mInventory.setItem(4, info);

		ItemStack guides = new ItemStack(Material.SCUTE);
		ItemMeta guidesMeta = guides.getItemMeta();
		guidesMeta.displayName(Component.text("Quest Guides", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		guidesMeta.lore(List.of(Component.text("Click to see available quests", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("across Monumenta by region and", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("town.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		guides.setItemMeta(guidesMeta);
		mInventory.setItem(3, guides);

		ItemStack hidden = new ItemStack(Material.CAULDRON);
		ItemMeta hiddenMeta = guides.getItemMeta();
		hiddenMeta.displayName(Component.text("Hidden Quests", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		hiddenMeta.lore(List.of(Component.text("Instructions", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		hidden.setItemMeta(hiddenMeta);
		mInventory.setItem(5, hidden);

		ItemStack death = new ItemStack(Material.SKULL_POTTERY_SHERD);
		ItemMeta deathMeta = death.getItemMeta();
		deathMeta.displayName(Component.text("Death Waypoints", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		deathMeta.lore(List.of(Component.text("No recent death to show here.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		death.setItemMeta(deathMeta);
		mInventory.setItem(32 + 9 * (rows - 4), death);
		mInventory.setItem(33 + 9 * (rows - 4), death);
		mInventory.setItem(34 + 9 * (rows - 4), death);

		ItemStack custom = new ItemStack(Material.LODESTONE);
		ItemMeta customMeta = custom.getItemMeta();
		customMeta.displayName(Component.text("Custom Waypoint", NamedTextColor.DARK_GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		customMeta.lore(List.of(Component.text("No custom waypoint set. Waypoints can be set through", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("Quest Guides, /waypoint, or by other special means.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		custom.setItemMeta(customMeta);
		mInventory.setItem(29 + 9 * (rows - 4), custom);

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

			String title = StringUtils.remove(quest.mTitle, "&a&l");

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
				slot = 31 + quest.mMarkersIndex[0] + 9 * (rows - 4);
			} else if (quest.mType == QuestCompassManager.CompassEntryType.Waypoint) {
				slot = 29 + 9 * (rows - 4);
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
				String qLore = StringUtils.remove(q.mLocation.getMessage(), "&a");
				while (splitIndex < qLore.length() && !(j != i && splitIndex > 0)) {
					lore.add(Component.text(StringUtils.substring(qLore, splitIndex, qLore.indexOf(" ", splitIndex + 45) < 0 ? 1000 : qLore.indexOf(" ", splitIndex + 45)) + (j != i && qLore.indexOf(" ", splitIndex + 45) > 0 ? " {...}" : ""), j == i ? NamedTextColor.WHITE : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					if (qLore.indexOf(" ", splitIndex + 45) < 0) {
						splitIndex += 1000;
					} else {
						splitIndex = 1 + qLore.indexOf(" ", splitIndex + 45);
					}
				}
				lore.add(Component.text((int) q.mLocation.getLocation().getX() + ", " + (int) q.mLocation.getLocation().getY() + ", " + (int) q.mLocation.getLocation().getZ() + " (" + (int) mPlayer.getLocation().distance(q.mLocation.getLocation()) + "m away)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
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
		if (!(event.getWhoClicked() instanceof Player player)
			|| event.getClickedInventory() != mInventory
			|| event.getInventory().getItem(event.getSlot()) == null) {
			return;
		}
		if (event.getSlot() == 3) {
			player.performCommand("sqgui show regionqg @s");
			return;
		} else if (event.getSlot() == 4 || event.getSlot() == 5) {
			// Not implemented
			return;
		}
		// This is throwing a Lot of NullPointerExceptions - find a way to make it Not do that
		int index = Objects.requireNonNull(event.getInventory().getItem(event.getSlot())).getItemMeta().getCustomModelData();
		if (mManager.getCurrentMarkerTitles(player).get(index).mType == QuestCompassManager.CompassEntryType.Waypoint && event.isShiftClick()) {
			mManager.removeCommandWaypoint(player);
			player.playSound(player.getLocation(), "minecraft:entity.armadillo.scute_drop", SoundCategory.PLAYERS, 1f, 1f);
			setupInventory();
			return;
		}
		mManager.mCurrentIndex.put(player, mManager.showCurrentQuest(player, index));
		player.playSound(player.getLocation(), Sound.UI_LOOM_TAKE_RESULT, SoundCategory.PLAYERS, 1f, 1f);
		close();
	}

	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		// Something later
	}
}

