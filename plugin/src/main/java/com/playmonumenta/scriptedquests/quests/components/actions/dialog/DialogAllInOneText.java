package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public class DialogAllInOneText implements DialogBase {
	private ArrayList<DialogAllInOneEntry> mEntries = new ArrayList<DialogAllInOneEntry>();

	public DialogAllInOneText(String npcName, JsonElement element) throws Exception {
		if (element.isJsonObject()) {
			mEntries.add(new DialogAllInOneEntry(npcName, element));
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mEntries.add(new DialogAllInOneEntry(npcName, iter.next()));
			}
		} else {
			throw new Exception("all_text value is neither an object nor an array!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		for (DialogAllInOneEntry ent : mEntries) {
			ent.sendDialog(plugin, player, npcEntity, prereqs);
		}
	}
}
