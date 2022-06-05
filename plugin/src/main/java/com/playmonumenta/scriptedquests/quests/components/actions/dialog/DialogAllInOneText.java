package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Collectors;

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
	public void sendDialog(QuestContext context) {
		for (DialogAllInOneEntry ent : mEntries) {
			ent.sendDialog(context);
		}
	}

	@Override
	public JsonElement serializeForClientAPI(QuestContext context) {
		return JsonObjectBuilder.get()
			.add("type", "all_in_one_text")
			.add("entries", mEntries.stream().map(v -> v.serializeForClientAPI(context)).collect(Collectors.toList()))
			.build();
	}
}
