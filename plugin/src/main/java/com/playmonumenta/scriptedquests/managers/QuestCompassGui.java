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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestCompassGui extends CustomInventory {
	private final Player mPlayer;
	private final QuestCompassManager mManager;

	public QuestCompassGui(Player player, QuestCompassManager manager) {
		super(player, 36, "Quest Compass");
		mPlayer = player;
		mManager = manager;
	}

	@Override
	public void openInventory(Player player, org.bukkit.plugin.Plugin owner) {
		super.openInventory(player, owner);
		setupInventory();
	}

	private void setupInventory() {
		mInventory.clear();
		mPlayer.playSound(mPlayer.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, SoundCategory.PLAYERS, 1f, 1f);

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
		hiddenMeta.displayName(Component.text("Hidden Quests", NamedTextColor.DARK_GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		hiddenMeta.lore(List.of(Component.text("Instructions", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		hidden.setItemMeta(hiddenMeta);
		mInventory.setItem(5, hidden);

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

			displayItem = new ItemStack(material);
			ItemMeta meta = displayItem.getItemMeta();
			meta.displayName(itemName);

			// Add quest index tag
			meta.setCustomModelData(i);

			List<Component> lore2 = new ArrayList<>();


			for (int j = i + 1 - quest.mMarkersIndex[0]; j < quest.mMarkersIndex[1]; j++) {
				int splitIndex = 0;
				ValidCompassEntry q = quests.get(j);
				String lore = StringUtils.remove(q.mLocation.getMessage(), "&a");
				while (splitIndex < lore.length() && !(j != i && splitIndex > 0)) {
					lore2.add(Component.text(StringUtils.substring(lore, splitIndex, lore.indexOf(" ", splitIndex + 45) < 0 ? 1000 : lore.indexOf(" ", splitIndex + 45)) + (j != i && lore.indexOf(" ", splitIndex + 45) > 0 ? " {...}" : ""), j == i ? NamedTextColor.WHITE : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
					if (lore.indexOf(" ", splitIndex + 45) < 0) {
						splitIndex += 1000;
					} else {
						splitIndex = 1 + lore.indexOf(" ", splitIndex + 45);
					}
				}
				lore2.add(Component.text((int) q.mLocation.getLocation().getX() + ", " + (int) q.mLocation.getLocation().getY() + ", " + (int) q.mLocation.getLocation().getZ() + " (" + (int) mPlayer.getLocation().distance(q.mLocation.getLocation()) + "m away)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
				if (j + 1 != q.mMarkersIndex[1]) {
					lore2.add(Component.text(""));
				}
			}

			// Find out if this quest has multiple markers. If it does, removing the last 6 characters of mTitle will match the QuestCompass title
//			for (QuestCompass q : mManager.mQuests) {
//				if (q.getQuestName().equals(StringUtils.substringBefore(title, title.length() - 6))) {
//					for (CompassLocation cLoc : q.getMarkers(mPlayer)) {
//						int splitIndex = 0;
//						while (splitIndex < lore.length()) {
//							lore2.add(Component.text(StringUtils.substring(lore, splitIndex, lore.indexOf(" ", splitIndex + 45) < 0 ? 1000 : lore.indexOf(" ", splitIndex + 45)), cLoc.getLocation().equals(quest.mLocation.getLocation()) ? NamedTextColor.WHITE : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
//							if (lore.indexOf(" ", splitIndex + 45) < 0) {
//								splitIndex += 1000;
//							} else {
//								splitIndex = 1 + lore.indexOf(" ", splitIndex + 45);
//							}
//						}
//						lore2.add(Component.text((int) cLoc.getLocation().getX() + ", " + (int) cLoc.getLocation().getY() + ", " + (int) cLoc.getLocation().getZ() + " (" + (int) mPlayer.getLocation().distance(cLoc.getLocation()) + "m away)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
//						lore2.add(Component.text(""));
//					}
//				} else if (q.getQuestName().equals(title)) {
//					int splitIndex = 0;
//					while (splitIndex < lore.length()) {
//						lore2.add(Component.text(StringUtils.substring(lore, splitIndex, lore.indexOf(" ", splitIndex + 45) < 0 ? 1000 : lore.indexOf(" ", splitIndex + 45)), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
//						if (lore.indexOf(" ", splitIndex + 45) < 0) {
//							splitIndex += 1000;
//						} else {
//							splitIndex = 1 + lore.indexOf(" ", splitIndex + 45);
//						}
//					}
//					lore2.add(Component.text((int) quest.mLocation.getLocation().getX() + ", " + (int) quest.mLocation.getLocation().getY() + ", " + (int) quest.mLocation.getLocation().getZ() + " (" + (int) mPlayer.getLocation().distance(quest.mLocation.getLocation()) + "m away)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
//					lore2.add(Component.text(""));
//				}
//			}

			meta.lore(lore2);
			displayItem.setItemMeta(meta);

			mInventory.setItem(slot, displayItem);
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		InventoryUtils.refreshOffhand(event);
		if ((event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)
			|| !(event.getWhoClicked() instanceof Player player)
			|| event.getClickedInventory() != mInventory
			|| event.getInventory().getItem(event.getSlot()) == null) {
			return;
		}
		if (event.getSlot() == 3) {
			mPlayer.performCommand("sqgui show regionqg @s");
			return;
		}

		mPlayer.playSound(mPlayer.getLocation(), Sound.UI_LOOM_TAKE_RESULT, SoundCategory.PLAYERS, 1f, 1f);

		int index = Objects.requireNonNull(event.getInventory().getItem(event.getSlot())).getItemMeta().getCustomModelData();
		mManager.mCurrentIndex.put(mPlayer, mManager.showCurrentQuest(player, index));
		close();
	}

	@Override
	protected void inventoryClose(InventoryCloseEvent event) {
		// Something later
	}
}

