package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.Gui;
import com.playmonumenta.scriptedquests.quests.components.GuiItem;
import com.playmonumenta.scriptedquests.quests.components.GuiPage;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GuiManager {

	public static final String MAIN_PAGE = "main";

	private final Map<String, Gui> mGuis = new HashMap<>();
	private final Plugin mPlugin;

	public GuiManager(Plugin plugin) {
		this.mPlugin = plugin;
		reload(plugin, null);
	}

	public void reload(Plugin plugin, CommandSender sender) {
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
		GuiPageCustomInventory customInv = new GuiPageCustomInventory(player, gui, page, pageName, edit);
		page.setupInventory(customInv, player, edit);
		customInv.openInventory(player, mPlugin);
	}

	private static class GuiPageCustomInventory extends CustomInventory {

		private final Gui mGui;
		private final String mPageName;
		private final boolean mEditMode;

		public GuiPageCustomInventory(Player player, Gui gui, GuiPage page, String pageName, boolean editMode) {
			super(player, page.getRows() * 9, page.getTitle());
			mGui = gui;
			mPageName = pageName;
			mEditMode = editMode;
		}

		@Override
		protected void inventoryClick(InventoryClickEvent event) {
			if (mEditMode) {
				return;
			}
			event.setCancelled(true);
			if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT
				    || !(event.getWhoClicked() instanceof Player)
				    || event.getClickedInventory() != mInventory) {
				return;
			}
			Player player = (Player) event.getWhoClicked();
			GuiPage page = mGui.getPage(mPageName);
			if (page == null) {
				return;
			}
			GuiItem guiItem = page.getItem(event.getSlot(), player);
			if (guiItem != null) {
				if (event.getClick() == ClickType.LEFT && guiItem.getLeftClickActions() != null) {
					if (!guiItem.getKeepGuiOpen()) {
						close();
					}
					guiItem.getLeftClickActions().doActions((Plugin) getPlugin(), player, null, guiItem.getPrerequisites());
				} else if (event.getClick() == ClickType.RIGHT && guiItem.getRightClickActions() != null) {
					if (!guiItem.getKeepGuiOpen()) {
						close();
					}
					guiItem.getRightClickActions().doActions((Plugin) getPlugin(), player, null, guiItem.getPrerequisites());
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
					QuestUtils.save(getPlugin(), event.getPlayer(), mGui.toJson(), mGui.getFile());
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
					closeActions.doActions((Plugin) getPlugin(), (Player) event.getPlayer(), null, null);
				}
			}
		}

	}

}
