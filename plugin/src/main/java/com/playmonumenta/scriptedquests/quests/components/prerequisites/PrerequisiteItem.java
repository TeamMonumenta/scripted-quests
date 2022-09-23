package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PrerequisiteItem {
	private final @Nullable Material mType;
	private final @Nullable String mName;
	private final @Nullable String mExactName;
	private final @Nullable String mLore;
	private final @Nullable String mExactLore;
	private final int mCount;

	public PrerequisiteItem(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();

		mName = JsonUtils.getString(object, "name", null);
		mType = JsonUtils.getMaterial(object, "type", null);
		mExactName = JsonUtils.getString(object, "exact_name", null);
		mLore = JsonUtils.getString(object, "lore", null);
		mExactLore = JsonUtils.getString(object, "exact_lore", null);
		mCount = JsonUtils.getInt(object, "count", 1);
	}

	/* Note this is not the standard check - can't be used as a generic PrerequisiteBase */
	public boolean prerequisiteMet(ItemStack[] items) {
		return check(items, false);
	}

	public boolean check(ItemStack[] items, boolean remove) {
		int matchCount = 0;

		if (items != null) {
			//  Loop through the given items
			for (ItemStack item : items) {
				if (item == null) {
					continue;
				}

				if ((mType == null || mType == item.getType())
					    && InventoryUtils.testForItemWithName(item, mName, false)
					    && InventoryUtils.testForItemWithName(item, mExactName, true)
					    && InventoryUtils.testForItemWithLore(item, mLore, false)
					    && InventoryUtils.testForItemWithLore(item, mExactLore, true)) {

					matchCount += item.getAmount();

					if (remove && mCount > 0) {
						int remaining = mCount - matchCount;
						item.setAmount(remaining < 0 ? -remaining : 0);
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
		}

		// didn't find enough items, return success only if we didn't want to find any
		return mCount <= 0;
	}
}
