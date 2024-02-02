package com.playmonumenta.scriptedquests.trades;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import de.tr7zw.nbtapi.NBTItem;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

public interface NpcTradeOverride {

	ItemStack resolve(Location someLocation);

	JsonElement toJson();

	class ItemOverride implements NpcTradeOverride {
		public final ItemStack mItem;

		public ItemOverride(ItemStack item) {
			mItem = item;
		}

		@Override
		public ItemStack resolve(Location someLocation) {
			return mItem.clone();
		}

		@Override
		public JsonElement toJson() {
			return new JsonPrimitive(NBTItem.convertItemtoNBT(mItem).toString());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ItemOverride that = (ItemOverride) o;
			return mItem.equals(that.mItem);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mItem);
		}
	}

	class LootTableOverride implements NpcTradeOverride {
		public final LootTable mLootTable;
		public final int mCount;

		public LootTableOverride(JsonObject json) throws Exception {
			mLootTable = Bukkit.getLootTable(Objects.requireNonNull(NamespacedKey.fromString(JsonUtils.getString(json, "loot_table"))));
			mCount = JsonUtils.getInt(json, "count");
		}

		public LootTableOverride(String lootTableKey, int count) {
			mLootTable = Bukkit.getLootTable(Objects.requireNonNull(NamespacedKey.fromString(lootTableKey)));
			mCount = count;
		}

		public LootTableOverride(LootTable lootTable, int count) {
			mLootTable = lootTable;
			mCount = count;
		}

		@Override
		public JsonElement toJson() {
			return new JsonObjectBuilder()
				       .add("loot_table", mLootTable.key().toString())
				       .add("count", mCount)
				       .build();
		}

		@Override
		public ItemStack resolve(Location someLocation) {
			Collection<ItemStack> loot = mLootTable.populateLoot(new Random(), new LootContext.Builder(someLocation).build());
			if (loot.isEmpty()) {
				return new ItemStack(Material.AIR);
			}
			ItemStack item = loot.iterator().next();
			if (mCount > 0) {
				item.setAmount(mCount);
			}
			return item;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			LootTableOverride that = (LootTableOverride) o;
			return mCount == that.mCount && mLootTable.equals(that.mLootTable);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mLootTable, mCount);
		}
	}

}
