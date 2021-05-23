package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import me.Novalescent.Core;
import me.Novalescent.items.types.RPGItem;
import me.Novalescent.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ActionDropLoot implements ActionBase {

	private String mLootPath;
	private Map<String, Integer> mLootMap = new HashMap<>();
	private Random mRandom;

	public ActionDropLoot(JsonElement element) throws Exception {

		mRandom = new Random();
		if (element.isJsonPrimitive()) {
			mLootPath = element.getAsString();
			if (mLootPath == null) {
				throw new Exception("loottable value is not a string!");
			}
		} else {
			JsonArray jsonArray = element.getAsJsonArray();
			for (JsonElement ele : jsonArray) {
				JsonObject jsonObject = ele.getAsJsonObject();
				mLootMap.put(jsonObject.get("id").getAsString(), jsonObject.get("amount").getAsInt());
			}
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		Location loc = npcEntity != null ? npcEntity.getLocation() : player.getLocation();

		int moves = 0;
		while (loc.getBlock().getType().isSolid()) {
			loc.add(0, 0.3, 0);
			moves++;

			if (moves >= 12) {
				break;
			}
		}
		if (mLootPath != null) {
			try {
				Collection<ItemStack> items = InventoryUtils.getLootTableContents(player, mLootPath, mRandom);
				for (ItemStack itemStack : items) {
					Item item = npcEntity.getWorld().dropItemNaturally(loc,
						Utils.convertItemToRPG(itemStack));
					Utils.attuneDrop(item, player, false);
					double x = mRandom.nextBoolean() ? -Math.random() : Math.random();
					double y = Math.random() * 0.5;
					double z = mRandom.nextBoolean() ? -Math.random() : Math.random();
					item.setVelocity(new Vector(x * 0.25, y, z * 0.25).multiply(0.65));
				}
			} catch (Exception e) {
				player.sendMessage(ChatColor.RED + "BUG! Server failed to drop you loot from the table '" + mLootPath + "'");
				player.sendMessage(ChatColor.RED + "Please hover over the following message, take a screenshot, and report this to a moderator");
				MessagingUtils.sendStackTrace(player, e);
			}
		} else {
			for (String id : mLootMap.keySet()) {
				RPGItem rpgItem = Core.getInstance().mItemManager.getItem(id);
				if (rpgItem != null) {
					ItemStack itemStack = rpgItem.getItemStack();
					itemStack.setAmount(mLootMap.get(id));

					Item item = npcEntity.getWorld().dropItemNaturally(loc, itemStack);
					Utils.attuneDrop(item, player, false);
					double x = mRandom.nextBoolean() ? -Math.random() : Math.random();
					double y = Math.random() * 0.5;
					double z = mRandom.nextBoolean() ? -Math.random() : Math.random();
					item.setVelocity(new Vector(x * 0.25, y, z * 0.25).multiply(0.65));
				}
			}
		}
	}

}
