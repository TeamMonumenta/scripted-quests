package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.playmonumenta.scriptedquests.utils.InventoryUtils;

import java.util.Map.Entry;
import java.util.Set;

import me.Novalescent.items.RPGItem;
import me.Novalescent.utils.Utils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class PrerequisiteItem {
	private String mName = "";
	private String mLore = "";
	private Material mType = Material.AIR;
	private int mCount = 1;
	private String mRPGItemName = "";

	public PrerequisiteItem(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("item value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			if (!key.equals("lore") && !key.equals("name") && !key.equals("count") && !key.equals("type") && !key.equals("rpg_itemname")) {
				throw new Exception("Unknown item key: " + key);
			}

			if (key.equals("lore")) {
				mLore = value.getAsString();
				if (mLore == null) {
					throw new Exception("item lore entry is not a string!");
				}
			} else if (key.equals("name")) {
				mName = value.getAsString();
				if (mName == null) {
					throw new Exception("item name entry is not a string!");
				}
			} else if (key.equals("count")) {
				mCount = value.getAsInt();
			} else if (key.equals("type")) {
				String typeStr = value.getAsString();
				if (typeStr == null) {
					throw new Exception("item type entry is not a string!");
				}
				try {
					mType = Material.valueOf(typeStr);
				} catch (IllegalArgumentException e) {
					throw new Exception("Unknown Material '" + typeStr +
					                    "' - it should be one of the values in this list: " +
					                    "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
				}
			} else if (key.equals("rpg_itemname")) {
				mRPGItemName = value.getAsString();
				if (mRPGItemName == null) {
					throw new Exception("rpg itemname entry is not a string!");
				}
			}
		}
	}

	/* Note this is not the standard check - can't be used as a generic PrerequisiteBase */
	public boolean prerequisiteMet(ItemStack[] items) {
		int matchCount = 0;

		if (items != null) {
			//  Loop through the given items
			for (ItemStack item : items) {
				if (item == null) {
					continue;
				}

				if (InventoryUtils.testForItemWithName(item, mName) &&
				    InventoryUtils.testForItemWithLore(item, mLore) &&
				    (mType.equals(Material.AIR) || mType.equals(item.getType()))) {

					if (mRPGItemName == null || mRPGItemName.isEmpty()) {
						matchCount += item.getAmount();
					} else {
						RPGItem rpgItem = Utils.getRPGItem(item);
						if (rpgItem != null && rpgItem.mId.equalsIgnoreCase(mRPGItemName)) {
							matchCount += item.getAmount();
						}
					}

				}

				if (mCount <= 0 && matchCount > 0) {
					// Found an item where none should be - fail
					return false;
				} else if (mCount > 0 && matchCount >= mCount) {
					// Found at least the correct number of items
					return true;
				}
			}
		}

		// didn't find item when didn't expect to
		if (mCount <= 0 && matchCount <= 0) {
			return true;
		}

		return false;
	}
}
