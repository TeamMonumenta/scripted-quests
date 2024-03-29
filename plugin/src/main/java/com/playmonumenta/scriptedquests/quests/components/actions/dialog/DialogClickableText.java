package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.entity.EntityType;

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
	public void sendDialog(QuestContext context) {
		for (DialogClickableTextEntry entry : mEntries) {
			entry.sendDialog(context);
		}
	}

	@Override
	public JsonElement serializeForClientAPI(QuestContext context) {
		return JsonObjectBuilder.get()
			.add("type", "clickable_text")
			.add("commands", mEntries.stream().map(v -> v.serializeForClientAPI(context))
				.collect(Collectors.toList()))
			.build();
	}
}

