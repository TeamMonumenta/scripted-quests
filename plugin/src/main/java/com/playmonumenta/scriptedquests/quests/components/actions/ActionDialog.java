package com.playmonumenta.scriptedquests.quests;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.playmonumenta.scriptedquests.Plugin;

class ActionDialog implements ActionBase {
	private ArrayList<DialogBase> mDialogs = new ArrayList<DialogBase>();

	ActionDialog(String npcName, String displayName,
	             EntityType entityType, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (key.equals("text")) {
				mDialogs.add(new DialogText(displayName, ent.getValue()));
			} else if (key.equals("raw_text")) {
				mDialogs.add(new DialogRawText(ent.getValue()));
			} else if (key.equals("clickable_text")) {
				mDialogs.add(new DialogClickableText(npcName, displayName, entityType, ent.getValue()));
			} else if (key.equals("random_text")) {
				mDialogs.add(new DialogRandomText(displayName, ent.getValue()));
			} else {
				throw new Exception("Unknown dialog key: '" + key + "'");
			}
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs) {
		for (DialogBase dialog : mDialogs) {
			dialog.sendDialog(plugin, player, prereqs);
		}
	}
}
