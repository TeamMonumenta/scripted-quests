package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.handbook.HandbookEntry;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ActionGiveHandbookEntries implements ActionBase {

	private List<String> mHandbookEntries = new ArrayList<>();

	public ActionGiveHandbookEntries(JsonElement element) throws Exception {
		JsonArray entryArray = element.getAsJsonArray();
		if (entryArray == null) {
			throw new Exception("give_handbook_entries is not a type array");
		}

		for (JsonElement e : entryArray) {
			String entry = e.getAsString();
			if (entry == null) {
				throw new Exception("give_handbook_entries entry is not a string!");
			}

			mHandbookEntries.add(entry);
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {

		List<HandbookEntry> entries = new ArrayList<>();
		for (String str : mHandbookEntries) {
			HandbookEntry entry = Core.getInstance().mHandbookManager.getEntry(str);

			if (entry != null) {
				entries.add(entry);
			} else {
				player.sendMessage(ChatColor.RED + "Tried to give you Handbook entry " + str + ", but it was not found. Please report this!");

			}
		}

		Core.getInstance().mHandbookManager.learnEntries(player, entries);
	}
}
