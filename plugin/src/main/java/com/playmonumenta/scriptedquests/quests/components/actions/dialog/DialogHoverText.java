package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public class DialogHoverText implements DialogBase {
	private ArrayList<DialogHoverTextEntry> mEntries = new ArrayList<DialogHoverTextEntry>();

	public DialogHoverText(String npcName, String displayName, EntityType entityType, JsonElement element) throws Exception {
		if (element.isJsonObject()) {
			mEntries.add(new DialogHoverTextEntry(npcName, displayName, entityType, element));
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mEntries.add(new DialogHoverTextEntry(npcName, displayName, entityType, iter.next()));
			}
		} else {
			throw new Exception("all_text value is neither an object nor an array!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		for (DialogHoverTextEntry ent : mEntries) {
			ent.sendDialog(plugin, player, npcEntity, prereqs);
		}
	}
}
