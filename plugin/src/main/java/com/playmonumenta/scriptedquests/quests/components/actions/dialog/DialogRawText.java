package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class DialogRawText implements DialogBase {
	private ArrayList<String> mText = new ArrayList<String>();

	public DialogRawText(JsonElement element) throws Exception {
		if (element.isJsonPrimitive()) {
			mText.add(element.getAsString());
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mText.add(iter.next().getAsString());
			}
		} else {
			throw new Exception("raw_text value is neither an array nor a string!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		for (String text : mText) {
			MessagingUtils.sendRawMessage(player, text);
		}
	}

	@Override
	public JsonElement serializeForClientAPI(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		return JsonObjectBuilder.get()
			.add("type", "raw_text")
			.add("text", mText.stream().map(JsonPrimitive::new).collect(Collectors.toList()))
			.build();
	}
}
