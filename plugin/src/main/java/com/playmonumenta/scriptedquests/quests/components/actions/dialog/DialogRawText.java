package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Collectors;

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
	public void sendDialog(QuestContext context) {
		for (String text : mText) {
			MessagingUtils.sendRawMessage(context.getPlayer(), text);
		}
	}

	@Override
	public JsonElement serializeForClientAPI(final QuestContext context) {
		return JsonObjectBuilder.get()
			.add("type", "raw_text")
			.add("text", mText.stream()
				.map((t) -> MessagingUtils.serializeRawMessage(context.getPlayer(), t, true).content())
				.map(JsonPrimitive::new)
				.collect(Collectors.toList())
			)
			.build();
	}
}
