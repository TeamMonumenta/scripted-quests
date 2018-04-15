package pe.scriptedquests.quests;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.scriptedquests.utils.InventoryUtils;

class PrerequisiteItemInHand implements PrerequisiteBase {
	private String mName = "";
	private String mLore = "";
	private Material mType = Material.AIR;
	private int mCount = 1;

	PrerequisiteItemInHand(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("item_in_hand value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			if (!key.equals("lore") && !key.equals("name") && !key.equals("count") && !key.equals("type")) {
				throw new Exception("Unknown item_in_hand key: " + key);
			}

			if (key.equals("lore")) {
				mLore = value.getAsString();
				if (mLore == null) {
					throw new Exception("item_in_hand lore entry is not a string!");
				}
			} else if (key.equals("name")) {
				mName = value.getAsString();
				if (mName == null) {
					throw new Exception("item_in_hand name entry is not a string!");
				}
			} else if (key.equals("count")) {
				mCount = value.getAsInt();
			} else if (key.equals("type")) {
				String typeStr = value.getAsString();
				if (typeStr == null) {
					throw new Exception("item_in_hand type entry is not a string!");
				}
				try {
					mType = Material.valueOf(typeStr);
				} catch (IllegalArgumentException e) {
					throw new Exception("Unknown Material '" + typeStr +
					                    "' - it should be one of the values in this list: " +
					                    "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
				}
			}
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		int matchCount = 0;

		ItemStack item = player.getInventory().getItemInMainHand();
		if (item != null) {
			if (InventoryUtils.testForItemWithName(item, mName) &&
				InventoryUtils.testForItemWithLore(item, mLore) &&
				(mType.equals(Material.AIR) || mType.equals(item.getType()))) {
				matchCount += item.getAmount();
			}

			if (mCount <= 0 && matchCount > 0) {
				// Found an item where none should be - fail
				return false;
			} else if (mCount > 0 && matchCount >= mCount) {
				// Found at least the correct number of items
				return true;
			}
		}

		// didn't find item when didn't expect to
		if (mCount <= 0 && matchCount <= 0) {
			return true;
		}

		return false;
	}
}
