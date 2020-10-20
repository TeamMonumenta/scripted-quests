package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import me.Novalescent.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Random;

public class ActionDropLoot implements ActionBase {

	private String mLootPath;
	private Random mRandom;

	public ActionDropLoot(JsonElement element) throws Exception {
		mLootPath = element.getAsString();
		if (mLootPath == null) {
			throw new Exception("Command value is not a string!");
		}
		mRandom = new Random();
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		try {
			Collection<ItemStack> items = InventoryUtils.getLootTableContents(player, mLootPath, mRandom);
			Location loc = npcEntity != null ? npcEntity.getLocation() : player.getLocation();

			int moves = 0;
			while (loc.getBlock().getType().isSolid()) {
				loc.add(0, 0.3, 0);
				moves++;

				if (moves >= 12) {
					break;
				}
			}
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
	}

}
