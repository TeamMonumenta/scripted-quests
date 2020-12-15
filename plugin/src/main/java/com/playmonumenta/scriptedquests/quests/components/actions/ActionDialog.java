package com.playmonumenta.scriptedquests.quests.components.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.playmonumenta.scriptedquests.quests.components.actions.dialog.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public class ActionDialog implements ActionBase {
	private ArrayList<DialogBase> mDialogs = new ArrayList<DialogBase>();

	public ActionDialog(String npcName, String displayName,
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
			} else if (key.equals("raw_random_text")) {
				mDialogs.add(new DialogRawRandomText(ent.getValue()));
			} else {
				throw new Exception("Unknown dialog key: '" + key + "'");
			}
		}
	}

	public List<DialogBase> getDialog() {
		return mDialogs;
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		for (DialogBase dialog : mDialogs) {
			dialog.sendDialog(plugin, player, npcEntity, prereqs);
		}
	}
}
