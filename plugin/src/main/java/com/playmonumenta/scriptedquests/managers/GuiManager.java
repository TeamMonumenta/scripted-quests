package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.Gui;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.GuiItem;
import com.playmonumenta.scriptedquests.quests.components.GuiPage;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class GuiManager {

	public static final String MAIN_PAGE = "main";

	private final Map<String, Gui> mGuis = new HashMap<>();
	private final Plugin mPlugin;

	public GuiManager(Plugin plugin) {
		this.mPlugin = plugin;
		reload(plugin, null);
	}

	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mGuis.clear();
		QuestUtils.loadScriptedQuests(plugin, "guis", sender, (object, file) -> {
			Gui gui = new Gui(object, file);
			mGuis.put(gui.getLabel(), gui);
			return gui.getLabel() + ":" + gui.getPages().length;
		});
	}

	public void reloadSingleGui(Gui gui, CommandSender sender) throws WrapperCommandSyntaxException {
		try {
			QuestUtils.loadScriptedQuestsFile(gui.getFile(), (object, file) -> {
				Gui newGui = new Gui(object, file);
				mGuis.remove(gui.getLabel());
				mGuis.put(newGui.getLabel(), newGui);
				return null;
			});
		} catch (Exception e) {
			MessagingUtils.sendStackTrace(sender, e);
			CommandAPI.fail("Failed to reload GUI: error in quest file '" + gui.getFile().getPath() + "'");
		}
	}

	public String[] getGuiNames() {
		return mGuis.keySet().toArray(new String[0]);
	}

	public @Nullable Gui getGui(String label) {
		return mGuis.get(label);
	}

	public void showGui(String label, Player player, String pageName) throws WrapperCommandSyntaxException {
		openGui(label, player, pageName, false);
	}

	public void editGui(String label, Player player, String pageName) throws WrapperCommandSyntaxException {
		Gui gui = mGuis.get(label);
		if (gui == null) {
			return;
		}
		reloadSingleGui(gui, player);
		openGui(label, player, pageName, true);
	}

	private void openGui(String label, Player player, String pageName, boolean edit) throws WrapperCommandSyntaxException {
		Gui gui = mGuis.get(label);
		if (gui == null) {
			return;
		}
		GuiPage page = gui.getPage(pageName);
		if (page == null) {
			return;
		}
		QuestContext originalContext = QuestContext.getCurrentContext();
		QuestContext context = new QuestContext(Plugin.getInstance(), player, originalContext != null ? originalContext.getNpcEntity() : null, false, null, originalContext != null ? originalContext.getUsedItem() : null);
		GuiPageCustomInventory customInv = new GuiPageCustomInventory(player, gui, page, pageName, edit, originalContext);
		customInv.setupInventory(page, context, edit);
		customInv.openInventory(player, mPlugin);
	}

	private static class GuiPageCustomInventory extends CustomInventory {

		private final Gui mGui;
		private final String mPageName;
		private final boolean mEditMode;
		private final @Nullable QuestContext mOriginalContext;
		private final GuiItem[] mGuiItems;

		public GuiPageCustomInventory(Player player, Gui gui, GuiPage page, String pageName, boolean editMode, @Nullable QuestContext originalContext) {
			super(player, page.getRows() * 9, page.getTitle());
			mGui = gui;
			mPageName = pageName;
			mEditMode = editMode;
			mOriginalContext = originalContext;
			mGuiItems = new GuiItem[page.getRows() * 9];
		}

		private void setupInventory(GuiPage page, QuestContext context, boolean edit) {
			mInventory.clear();
			Arrays.fill(mGuiItems, null);
			for (GuiItem item : page.getItems()) {
				int index = item.getRow() * 9 + item.getCol();
				ItemStack existingItem = mInventory.getItem(index);
				ItemStack displayItem;
				if (existingItem != null) {
					if (edit) {
						displayItem = item.combineDisplayItem(context, existingItem);
					} else {
						// When multiple items are visible in the same slot, show only the first one.
						continue;
					}
				} else {
					displayItem = item.getDisplayItem(context, edit);
				}
				if (displayItem != null) {
					mInventory.setItem(index, displayItem);
					mGuiItems[index] = item;
				}
			}
			if (!edit && page.getFillerItem() != null) {
				for (int i = 0; i < mInventory.getSize(); i++) {
					if (mInventory.getItem(i) == null) {
						mInventory.setItem(i, page.getFillerItem());
					}
				}
			}
		}

		@Override
		protected void inventoryClick(InventoryClickEvent event) {
			if (mEditMode) {
				return;
			}
			event.setCancelled(true);
			if ((event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)
				    || !(event.getWhoClicked() instanceof Player player)
				    || event.getClickedInventory() != mInventory) {
				return;
			}
			GuiItem guiItem = event.getSlot() < 0 || event.getSlot() >= mGuiItems.length ? null : mGuiItems[event.getSlot()];
			if (guiItem != null) {
				QuestContext context = new QuestContext(Plugin.getInstance(), player, mOriginalContext != null ? mOriginalContext.getNpcEntity() : null, false, guiItem.getPrerequisites(), mOriginalContext != null ? mOriginalContext.getUsedItem() : null);
				if (!context.prerequisitesMet()) {
					player.sendMessage(Component.text("You no longer meet the requirements for this option", NamedTextColor.RED));
					close();
					return;
				}
				if (event.getClick() == ClickType.LEFT && guiItem.getLeftClickActions() != null) {
					if (!guiItem.getKeepGuiOpen()) {
						close();
					}
					guiItem.getLeftClickActions().doActions(context);
				} else if (event.getClick() == ClickType.RIGHT && guiItem.getRightClickActions() != null) {
					if (!guiItem.getKeepGuiOpen()) {
						close();
					}
					guiItem.getRightClickActions().doActions(context);
				}
			}
		}

		@Override
		protected void inventoryDrag(InventoryDragEvent event) {
			if (!mEditMode) {
				event.setCancelled(true);
			}
		}

		@Override
		protected void inventoryClose(InventoryCloseEvent event) {
			GuiPage page = mGui.getPage(mPageName);
			if (page == null) { // this could happen if someone removed the page while the inventory was open
				return;
			}
			if (mEditMode) {
				// save the GUI
				try {
					GuiPage updated = page.createUpdated(getInventory());
					mGui.setPage(mPageName, updated);
					QuestUtils.save(Plugin.getInstance(), event.getPlayer(), mGui.toJson(), mGui.getFile());
					event.getPlayer().sendMessage(ChatColor.GOLD + "GUI updated.");
				} catch (Exception e) {
					event.getPlayer().sendMessage(ChatColor.RED + "Failed to update GUI.");
					MessagingUtils.sendStackTrace(event.getPlayer(), e);
				}
			} else {
				if (!(event.getPlayer() instanceof Player)) {
					return;
				}
				QuestActions closeActions = page.getCloseActions();
				if (closeActions != null) {
					closeActions.doActions(new QuestContext(Plugin.getInstance(), (Player) event.getPlayer(), null));
				}
			}
		}

	}

}
