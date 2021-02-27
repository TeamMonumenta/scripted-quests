package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.abilities.core.Ability;
import me.Novalescent.player.PlayerData;
import me.Novalescent.player.handbook.HandbookEntry;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionGiveHandbookEntry implements ActionBase {

	private String mHandbookEntryId;

	public ActionGiveHandbookEntry(JsonElement element) throws Exception {
		mHandbookEntryId = element.getAsString();
		if (mHandbookEntryId == null) {
			throw new Exception("Ability value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		HandbookEntry entry = Core.getInstance().mHandbookManager.getEntry(mHandbookEntryId);

		if (entry != null) {
			entry.learn(Core.getInstance(), player);
		} else {
			player.sendMessage(ChatColor.RED + "Tried to give you Handbook entry " + mHandbookEntryId + ", but it was not found. Please report this!");
		}
	}
}
