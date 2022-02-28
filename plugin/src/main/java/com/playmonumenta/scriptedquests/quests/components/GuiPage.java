package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GuiPage {

	private final int mRows;
	private final String mTitle;
	private final @Nullable ItemStack mFillerItem;
	private final List<GuiItem> mItems = new ArrayList<>();
	private final @Nullable JsonElement mCloseActionsJson;
	private final @Nullable QuestActions mCloseActions;

	public GuiPage(JsonObject object) throws Exception {
		mRows = JsonUtils.getInt(object, "rows");
		mTitle = JsonUtils.getString(object, "title");
		String fillerItem = JsonUtils.getString(object, "filler_item", null);
		mFillerItem = fillerItem != null && !fillerItem.isEmpty() ? NBTItem.convertNBTtoItem(new NBTContainer(fillerItem)) : null;
		for (JsonElement item : JsonUtils.getJsonArray(object, "items")) {
			GuiItem guiItem = new GuiItem(item.getAsJsonObject());
			int index = guiItem.getRow() * 9 + guiItem.getCol();
			if (index < 0 || index >= mRows * 9) {
				throw new Exception("Invalid coordinates (row=" + guiItem.getRow() + ", col=" + guiItem.getCol() + ") for GUI item");
			}
			mItems.add(guiItem);
		}
		mCloseActionsJson = object.get("close_actions");
		if (mCloseActionsJson != null) {
			mCloseActions = new QuestActions(null, null, null, 1, mCloseActionsJson);
		} else {
			mCloseActions = null;
		}
	}

	private GuiPage(int rows, String title, @Nullable ItemStack fillerItem, @Nullable JsonElement closeActionsJson, @Nullable QuestActions closeActions) {
		mRows = rows;
		mTitle = title;
		mFillerItem = fillerItem;
		mCloseActionsJson = closeActionsJson;
		mCloseActions = closeActions;
	}

	public JsonObject toJson() {
		return new JsonObjectBuilder()
			.add("rows", mRows)
			.add("title", mTitle)
			.add("filler_item", NBTItem.convertItemtoNBT(mFillerItem).toString())
			.add("items", JsonUtils.toJsonArray(mItems, GuiItem::toJson))
			.add("close_actions", mCloseActionsJson)
			.build();
	}

	public void setupInventory(CustomInventory customInventory, Player player, boolean edit) throws WrapperCommandSyntaxException {
		Inventory inventory = customInventory.getInventory();
		inventory.clear();
		for (GuiItem item : mItems) {
			int index = item.getRow() * 9 + item.getCol();
			ItemStack existingItem = inventory.getItem(index);
			ItemStack displayItem;
			if (existingItem != null) {
				if (edit) {
					displayItem = item.combineDisplayItem(player, existingItem);
				} else {
					// When multiple items are visible in the same slot, show only the first one.
					// This matches the click behaviour where only the first visible item's actions are executed.
					continue;
				}
			} else {
				displayItem = item.getDisplayItem(player, edit);
			}
			if (displayItem != null) {
				inventory.setItem(index, displayItem);
			}
		}
		if (!edit && mFillerItem != null) {
			for (int i = 0; i < inventory.getSize(); i++) {
				if (inventory.getItem(i) == null) {
					inventory.setItem(i, mFillerItem);
				}
			}
		}
	}

	/**
	 * Creates a new {@link GuiPage} that is a copy of this one with items replaced by the ones in the given inventory.
	 *
	 * @param inventory An inventory
	 * @return A new {@link GuiPage}
	 * @throws Exception If anything bad happens, e.g. prerequisites cannot be parsed
	 */
	public GuiPage createUpdated(Inventory inventory) throws Exception {
		GuiPage clone = new GuiPage(mRows, mTitle, mFillerItem, mCloseActionsJson, mCloseActions);
		@Nullable ItemStack[] contents = inventory.getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack itemStack = contents[i];
			if (itemStack != null) {
				clone.mItems.addAll(GuiItem.parseItems(i, itemStack));
			}
		}
		return clone;
	}

	public @Nullable GuiItem getItem(int index, Player player) {
		for (GuiItem item : mItems) {
			if (item.getCol() + item.getRow() * 9 == index
				    && (item.getPrerequisites() == null || item.getPrerequisites().prerequisiteMet(player, null))) {
				return item;
			}
		}
		return null;
	}

	public int getRows() {
		return mRows;
	}

	public String getTitle() {
		return mTitle;
	}

	public QuestActions getCloseActions() {
		return mCloseActions;
	}
}
