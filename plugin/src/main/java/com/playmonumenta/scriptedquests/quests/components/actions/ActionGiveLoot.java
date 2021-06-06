package com.playmonumenta.scriptedquests.quests.components.actions;

import java.util.Collection;
import java.util.Random;

import me.Novalescent.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ActionGiveLoot implements ActionBase {
	private String mLootPath;
	private Random mRandom;

	public ActionGiveLoot(JsonElement element) throws Exception {
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
			InventoryUtils.giveItems(player, items, false);

			for (ItemStack item : items) {

				ItemStack converted = Utils.convertItemToRPG(item);
				ItemMeta meta = converted.getItemMeta();
				player.sendMessage(bracketText(net.md_5.bungee.api.ChatColor.AQUA + "+" + converted.getAmount() +
					" " + meta.getDisplayName()));
			}
		} catch (Exception e) {
			player.sendMessage(ChatColor.RED + "BUG! Server failed to give you loot from the table '" + mLootPath + "'");
			player.sendMessage(ChatColor.RED + "Please hover over the following message, take a screenshot, and report this to a moderator");
			MessagingUtils.sendStackTrace(player, e);
		}
	}

	private String bracketText(String str) {
		return net.md_5.bungee.api.ChatColor.DARK_GRAY + "[" + str + net.md_5.bungee.api.ChatColor.DARK_GRAY + "]";
	}
}
