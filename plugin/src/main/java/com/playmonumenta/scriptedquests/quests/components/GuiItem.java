package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiItem {

	private static final String ROW_KEY = "row";
	private static final String COL_KEY = "col";
	private static final String ITEM_KEY = "item";
	private static final String KEEP_GUI_OPEN_KEY = "keep_gui_open";
	private static final String PREREQUISITES_KEY = "prerequisites";
	private static final String NBT_TAGS_KEY = "nbt_tags";
	private static final String LEFT_CLICK_ACTIONS_KEY = "left_click_actions";
	private static final String RIGHT_CLICK_ACTIONS_KEY = "right_click_actions";

	// these are only used for the items in the edit inventory
	private static final String SQGUI_KEY = "sqgui";
	private static final String MORE_ITEMS_KEY = "more_items";

	private final int mRow;
	private final int mCol;
	private final ItemStack mDisplayItem;

	private boolean mKeepGuiOpen;
	private @Nullable JsonElement mPrerequisitesJson;
	private @Nullable QuestPrerequisites mPrerequisites;
	private @Nullable JsonElement mNbtTagsJson;
	private @Nullable NbtTags mNbtTags;
	private @Nullable JsonElement mLeftClickActionsJson;
	private @Nullable QuestActions mLeftClickActions;
	private @Nullable JsonElement mRightClickActionsJson;
	private @Nullable QuestActions mRightClickActions;

	public GuiItem(JsonObject object) throws Exception {
		mRow = JsonUtils.getInt(object, ROW_KEY);
		mCol = JsonUtils.getInt(object, COL_KEY);
		mDisplayItem = NBTItem.convertNBTtoItem(new NBTContainer(JsonUtils.getString(object, ITEM_KEY)));

		mKeepGuiOpen = JsonUtils.getBoolean(object, KEEP_GUI_OPEN_KEY, false);

		mPrerequisitesJson = object.get(PREREQUISITES_KEY);
		if (mPrerequisitesJson != null) {
			mPrerequisites = new QuestPrerequisites(mPrerequisitesJson.getAsJsonObject());
		}

		mNbtTagsJson = object.get(NBT_TAGS_KEY);
		if (mNbtTagsJson != null) {
			NbtTags tags = new NbtTags(mNbtTagsJson);
			mNbtTags = tags.hasTags() ? tags : null;
		}

		mLeftClickActionsJson = object.get(LEFT_CLICK_ACTIONS_KEY);
		if (mLeftClickActionsJson != null) {
			mLeftClickActions = new QuestActions(null, null, null, 0, mLeftClickActionsJson);
		}

		mRightClickActionsJson = object.get(RIGHT_CLICK_ACTIONS_KEY);
		if (mRightClickActionsJson != null) {
			mRightClickActions = new QuestActions(null, null, null, 0, mRightClickActionsJson);
		}

	}

	public JsonObject toJson() {
		return new JsonObjectBuilder()
			.add(ROW_KEY, mRow)
			.add(COL_KEY, mCol)
			.add(ITEM_KEY, NBTItem.convertItemtoNBT(mDisplayItem).toString())
			.add(KEEP_GUI_OPEN_KEY, mKeepGuiOpen)
			.add(PREREQUISITES_KEY, mPrerequisitesJson)
			.add(NBT_TAGS_KEY, mNbtTagsJson)
			.add(LEFT_CLICK_ACTIONS_KEY, mLeftClickActionsJson)
			.add(RIGHT_CLICK_ACTIONS_KEY, mRightClickActionsJson)
			.build();
	}

	private GuiItem(int index, ItemStack itemStack) throws Exception {
		mRow = index / 9;
		mCol = index % 9;

		NBTItem nbtItem = new NBTItem(itemStack);
		NBTCompound sqguiCompound = nbtItem.getCompound(SQGUI_KEY);
		if (sqguiCompound != null) {
			Boolean keepGuiOpen = sqguiCompound.getBoolean(KEEP_GUI_OPEN_KEY);
			if (keepGuiOpen != null) {
				mKeepGuiOpen = keepGuiOpen;
			}
			Gson gson = new Gson();
			String prerequisites = sqguiCompound.getString(PREREQUISITES_KEY);
			if (prerequisites != null) {
				mPrerequisitesJson = gson.fromJson(prerequisites, JsonElement.class);
				if (mPrerequisitesJson.isJsonNull()) {
					mPrerequisitesJson = null;
				} else {
					mPrerequisites = new QuestPrerequisites(mPrerequisitesJson);
				}
			}
			String nbt_tags = sqguiCompound.getString(NBT_TAGS_KEY);
			if (nbt_tags != null) {
				mNbtTagsJson = gson.fromJson(nbt_tags, JsonElement.class);
				if (mNbtTagsJson.isJsonNull()) {
					mNbtTagsJson = null;
				} else {
					NbtTags tags = new NbtTags(mNbtTagsJson);
					mNbtTags = tags.hasTags() ? mNbtTags : null;
				}
			}
			String leftClickActions = sqguiCompound.getString(LEFT_CLICK_ACTIONS_KEY);
			if (leftClickActions != null) {
				mLeftClickActionsJson = gson.fromJson(leftClickActions, JsonElement.class);
				if (mLeftClickActionsJson.isJsonNull()) {
					mLeftClickActionsJson = null;
				} else {
					mLeftClickActions = new QuestActions(null, null, null, 0, mLeftClickActionsJson);
				}
			}
			String rightClickActions = sqguiCompound.getString(RIGHT_CLICK_ACTIONS_KEY);
			if (rightClickActions != null) {
				mRightClickActionsJson = gson.fromJson(rightClickActions, JsonElement.class);
				if (mRightClickActionsJson.isJsonNull()) {
					mRightClickActionsJson = null;
				} else {
					mRightClickActions = new QuestActions(null, null, null, 0, mRightClickActionsJson);
				}
			}
			nbtItem.removeKey(SQGUI_KEY);
			itemStack = nbtItem.getItem();
			List<Component> lore = itemStack.lore();
			if (lore != null) {
				itemStack.lore(lore.stream().filter(s -> !MessagingUtils.plainText(s).startsWith("[SQGUI]")).collect(Collectors.toList()));
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

	public ItemStack getDisplayItem(QuestContext context, boolean edit) {
		if (edit) {
			ItemStack item = mDisplayItem.clone();
			List<Component> lore = item.lore();
			if (lore == null) {
				lore = new ArrayList<>();
			}
			if (mPrerequisites != null) {
				lore.add(Component.text("[SQGUI] Has prerequisites"));
			}
			if (mNbtTags != null) {
				lore.add(Component.text("[SQGUI] Has tags"));
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
			NBTCompound sqguiCompound = nbtItem.addCompound(SQGUI_KEY);
			Gson gson = new Gson();
			sqguiCompound.setBoolean(KEEP_GUI_OPEN_KEY, mKeepGuiOpen);
			sqguiCompound.setString(PREREQUISITES_KEY, gson.toJson(mPrerequisitesJson));
			sqguiCompound.setString(NBT_TAGS_KEY, gson.toJson(mNbtTagsJson));
			sqguiCompound.setString(LEFT_CLICK_ACTIONS_KEY, gson.toJson(mLeftClickActionsJson));
			sqguiCompound.setString(RIGHT_CLICK_ACTIONS_KEY, gson.toJson(mRightClickActionsJson));
			return nbtItem.getItem();
		} else {
			if (mPrerequisites != null && !mPrerequisites.prerequisiteMet(context)) {
				return null;
			}
			ItemStack displayItem = mDisplayItem.clone();
			ItemMeta meta = displayItem.getItemMeta();
			meta.displayName(NmsUtils.getVersionAdapter().resolveComponents(meta.displayName(), context.getPlayer()));
			List<Component> lore = meta.lore();
			if (lore != null) {
				meta.lore(lore.stream().map(line -> NmsUtils.getVersionAdapter().resolveComponents(line, context.getPlayer())).collect(Collectors.toList()));
			}
			displayItem.setItemMeta(meta);
			if (mNbtTags != null && mNbtTags.hasTags()) {
				displayItem = mNbtTags.applyTags(displayItem);
			}
			context.getPlayer().getInventory().addItem(displayItem);
			return displayItem;
		}
	}

	public ItemStack combineDisplayItem(QuestContext context, ItemStack item) {
		List<Component> lore = item.lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}
		lore.add(Component.text("[SQGUI] Has more items in this slot"));
		item.lore(lore);
		NBTItem nbtItem = new NBTItem(item);
		NBTCompound sqguiCompound = nbtItem.addCompound(SQGUI_KEY);
		NBTCompoundList moreItemsList = sqguiCompound.getCompoundList(MORE_ITEMS_KEY);
		moreItemsList.addCompound(NBTItem.convertItemtoNBT(getDisplayItem(context, true)));
		return nbtItem.getItem();
	}

	public static List<GuiItem> parseItems(int index, ItemStack itemStack) throws Exception {
		List<GuiItem> result = new ArrayList<>();
		NBTItem nbtItem = new NBTItem(itemStack);
		NBTCompound sqguiCompound = nbtItem.getCompound(SQGUI_KEY);
		if (sqguiCompound != null) {
			NBTCompoundList moreItems = sqguiCompound.getCompoundList(MORE_ITEMS_KEY);
			for (ReadWriteNBT item : moreItems) {
				result.add(new GuiItem(index, NBTItem.convertNBTtoItem(new NBTContainer(item.toString()))));
			}
		}
		result.add(0, new GuiItem(index, itemStack));
		return result;
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
