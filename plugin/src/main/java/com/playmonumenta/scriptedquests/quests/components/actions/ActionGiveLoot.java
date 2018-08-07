package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonElement;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

class ActionGiveLoot implements ActionBase {
	private String mLootPath;

	ActionGiveLoot(JsonElement element) throws Exception {
		mLootPath = element.getAsString();
		if (mLootPath == null) {
			throw new Exception("Command value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs) {
		try {
			InventoryUtils.giveLootTableContents(player, mLootPath);
		} catch (Exception e) {
			player.sendMessage(ChatColor.RED + "BUG! Server failed to give you loot from the table '" + mLootPath + "'");
			player.sendMessage(ChatColor.RED + "Please hover over the following message, take a screenshot, and report this to a moderator");
			MessagingUtils.sendStackTrace(player, e);
		}
	}
}
