package com.playmonumenta.scriptedquests.quests.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.utils.JsonUtils;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

public class GuiItem {

	private final int mRow;
	private final int mCol;
	private final ItemStack mDisplayItem;

	private boolean mKeepGuiOpen;
	private @Nullable JsonElement mPrerequisitesJson;
	private @Nullable QuestPrerequisites mPrerequisites;
	private @Nullable JsonElement mLeftClickActionsJson;
	private @Nullable QuestActions mLeftClickActions;
	private @Nullable JsonElement mRightClickActionsJson;
	private @Nullable QuestActions mRightClickActions;

	public GuiItem(JsonObject object) throws Exception {
		mRow = JsonUtils.getInt(object, "row");
		mCol = JsonUtils.getInt(object, "col");
		mDisplayItem = NBTItem.convertNBTtoItem(new NBTContainer(JsonUtils.getString(object, "item")));

		mKeepGuiOpen = JsonUtils.getBoolean(object, "keep_gui_open", false);

		mPrerequisitesJson = object.get("prerequisites");
		if (mPrerequisitesJson != null) {
			mPrerequisites = new QuestPrerequisites(mPrerequisitesJson.getAsJsonObject());
		}

		mLeftClickActionsJson = object.get("left_click_actions");
		if (mLeftClickActionsJson != null) {
			mLeftClickActions = new QuestActions(null, null, null, 0, mLeftClickActionsJson);
		}

		mRightClickActionsJson = object.get("right_click_actions");
		if (mRightClickActionsJson != null) {
			mRightClickActions = new QuestActions(null, null, null, 0, mRightClickActionsJson);
		}

	}

	public JsonObject toJson() {
		return new JsonObjectBuilder()
			.add("row", mRow)
			.add("col", mCol)
			.add("item", NBTItem.convertItemtoNBT(mDisplayItem).toString())
			.add("prerequisites", mPrerequisitesJson)
			.add("keep_gui_open", mKeepGuiOpen)
			.add("left_click_actions", mLeftClickActionsJson)
			.add("right_click_actions", mRightClickActionsJson)
			.build();
	}

	public GuiItem(int index, ItemStack itemStack) throws Exception {
		mRow = index / 9;
		mCol = index % 9;

		NBTItem nbtItem = new NBTItem(itemStack);
		NBTCompound sqguiCompound = nbtItem.getCompound("sqgui");
		if (sqguiCompound != null) {
			JsonParser jsonParser = new JsonParser();
			Boolean keepGuiOpen = sqguiCompound.getBoolean("keep_gui_open");
			if (keepGuiOpen != null) {
				mKeepGuiOpen = keepGuiOpen;
			}
			String prerequisites = sqguiCompound.getString("prerequisites");
			if (prerequisites != null) {
				mPrerequisitesJson = jsonParser.parse(prerequisites);
				if (mPrerequisitesJson.isJsonNull()) {
					mPrerequisitesJson = null;
				} else {
					mPrerequisites = new QuestPrerequisites(mPrerequisitesJson);
				}
			}
			String leftClickActions = sqguiCompound.getString("left_click_actions");
			if (leftClickActions != null) {
				mLeftClickActionsJson = jsonParser.parse(leftClickActions);
				if (mLeftClickActionsJson.isJsonNull()) {
					mLeftClickActionsJson = null;
				} else {
					mLeftClickActions = new QuestActions(null, null, null, 0, mLeftClickActionsJson);
				}
			}
			String rightClickActions = sqguiCompound.getString("right_click_actions");
			if (rightClickActions != null) {
				mRightClickActionsJson = jsonParser.parse(rightClickActions);
				if (mRightClickActionsJson.isJsonNull()) {
					mRightClickActionsJson = null;
				} else {
					mRightClickActions = new QuestActions(null, null, null, 0, mRightClickActionsJson);
				}
			}
			nbtItem.removeKey("sqgui");
			itemStack = nbtItem.getItem();
			List<Component> lore = itemStack.lore();
			if (lore != null) {
				itemStack.lore(lore.stream().filter(s -> !PlainComponentSerializer.plain().serialize(s).startsWith("[SQGUI]")).collect(Collectors.toList()));
			}
		}

		mDisplayItem = itemStack;
	}

	public int getRow() {
		return mRow;
	}

	public int getCol() {
		return mCol;
	}

	public ItemStack getDisplayItem(Player player, boolean edit) {
		if (edit) {
			ItemStack item = mDisplayItem.clone();
			List<Component> lore = item.lore();
			if (lore == null) {
				lore = new ArrayList<>();
			}
			if (mPrerequisites != null) {
				lore.add(Component.text("[SQGUI] Has prerequisites"));
			}
			if (mKeepGuiOpen) {
				lore.add(Component.text("[SQGUI] keeps GUI open"));
			}
			if (mLeftClickActions != null) {
				lore.add(Component.text("[SQGUI] Has left click action(s)"));
			}
			if (mRightClickActions != null) {
				lore.add(Component.text("[SQGUI] Has right click action(s)"));
			}
			item.lore(lore);
			NBTItem nbtItem = new NBTItem(item);
			NBTCompound sqguiCompound = nbtItem.addCompound("sqgui");
			Gson gson = new Gson();
			sqguiCompound.setBoolean("keep_gui_open", mKeepGuiOpen);
			sqguiCompound.setString("prerequisites", gson.toJson(mPrerequisitesJson));
			sqguiCompound.setString("left_click_actions", gson.toJson(mLeftClickActionsJson));
			sqguiCompound.setString("right_click_actions", gson.toJson(mRightClickActionsJson));
			item = nbtItem.getItem();
			return item;
		} else {
			if (mPrerequisites != null && !mPrerequisites.prerequisiteMet(player, null)) {
				return null;
			}
			return mDisplayItem;
		}
	}

	public boolean getKeepGuiOpen() {
		return mKeepGuiOpen;
	}

	public @Nullable QuestPrerequisites getPrerequisites() {
		return mPrerequisites;
	}

	public @Nullable QuestActions getLeftClickActions() {
		return mLeftClickActions;
	}

	public @Nullable QuestActions getRightClickActions() {
		return mRightClickActions;
	}

}
