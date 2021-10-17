package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.*;
import java.util.stream.Collectors;

import com.google.api.client.json.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public class DialogClickableText implements DialogBase {
	private ArrayList<DialogClickableTextEntry> mEntries = new ArrayList<DialogClickableTextEntry>();

	public DialogClickableText(String npcName, String displayName,
	                    EntityType entityType, JsonElement element) throws Exception {
		/*
		 * Integer used to determine which of the available clickable entries was
		 * clicked when a player clicks a chat message
		 * Choosing a random 32-bit starting number is good enough to prevent collisions
		 * Doesn't actually matter from a security perspective if collisions do happen,
		 *  it'd just mean you could click dialog from a different NPC way up in chat
		 *  and it'd potentially trigger a current conversation
		 */
		int entryIdx = new Random().nextInt();

		if (element.isJsonObject()) {
			mEntries.add(new DialogClickableTextEntry(npcName, displayName, entityType, element, entryIdx));
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mEntries.add(new DialogClickableTextEntry(npcName, displayName, entityType, iter.next(), entryIdx));

				entryIdx++;
			}
		} else {
			throw new Exception("clickable_text value is neither an object nor an array!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		for (DialogClickableTextEntry entry : mEntries) {
			entry.sendDialog(plugin, player, npcEntity, prereqs);
		}
	}

	@Override
	public JsonElement serialize(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		return JsonObjectBuilder.get()
			.add("type", "clickable_text")
			.add("commands", mEntries.stream().map(v -> v.serialize(plugin, player, npcEntity, prereqs))
				.collect(Collectors.toList()))
			.build();
	}
}

