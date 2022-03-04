package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.ArrayList;
import java.util.Iterator;

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
}
