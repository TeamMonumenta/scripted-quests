package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.tr7zw.nbtapi.NBTItem;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public class NbtTags {
	//
	boolean mHasTags;
	Map<String, String> mTags = new HashMap<>();

	//
	public NbtTags(JsonElement element) throws Exception {
		try {
			JsonArray array = element.getAsJsonArray();
			array.forEach((JsonElement tagElement) -> {
				JsonObject tag = tagElement.getAsJsonObject();
				String path = tag.get("path").getAsString();
				String value = tag.get("value").getAsString();
				mTags.put(path, value);
			});
			mHasTags = !mTags.isEmpty();
		} catch (Exception e) {
			throw new Exception("error parsing nbt tags: " + e.getMessage());
		}
	}

	public ItemStack applyTags(ItemStack displayItem) {
		NBTItem item = new NBTItem(displayItem);
		mTags.forEach((path, value) -> {
			int pos = path.lastIndexOf('.');
			if (pos == -1) {
				// not a nested path:
				item.setString(path, value);
			} else {
				// nested path, grab the last node to use as a compound:
				String actualPath = path.substring(0, pos);
				String partOfCompound = path.substring(pos + 1);
				item.resolveOrCreateCompound(actualPath).setString(partOfCompound, value);
			}
	});
		return item.getItem();
	}

	public boolean hasTags() {
		return mHasTags;
	}
}