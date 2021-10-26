package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class DialogRandomText implements DialogBase {
	private String mDisplayName;
	private ArrayList<String> mText = new ArrayList<String>();
	private Random mRandom = new Random();

	public DialogRandomText(String displayName, JsonElement element) throws Exception {
		mDisplayName = displayName;

		if (element.isJsonPrimitive()) {
			mText.add(element.getAsString());
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mText.add(iter.next().getAsString());
			}
		} else {
			throw new Exception("random_text value is neither an array nor a string!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		int idx = mRandom.nextInt(mText.size());
		MessagingUtils.sendNPCMessage(player, mDisplayName, mText.get(idx));
	}

	@Override
	public JsonElement serializeForClientAPI(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		int idx = mRandom.nextInt(mText.size());
		return JsonObjectBuilder.get()
			.add("type", "random_text")
			.add("commands", mText.get(idx))
			.add("npc_name", mDisplayName)
			.build();

	}
}

