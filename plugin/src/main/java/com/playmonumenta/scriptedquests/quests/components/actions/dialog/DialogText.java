package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class DialogText implements DialogBase {
	private final @Nullable String mDisplayName;
	private final ArrayList<String> mText = new ArrayList<>();

	public DialogText(@Nullable String displayName, JsonElement element) throws Exception {
		mDisplayName = displayName;

		if (element.isJsonPrimitive()) {
			mText.add(element.getAsString());
		} else if (element.isJsonArray()) {
			for (JsonElement jsonElement : element.getAsJsonArray()) {
				mText.add(jsonElement.getAsString());
			}
		} else {
			throw new Exception("text value is neither an array nor a string!");
		}
	}

	@Override
	public void sendDialog(QuestContext context) {
		for (String text : mText) {
			if (mDisplayName != null && !mDisplayName.isEmpty()) {
				MessagingUtils.sendNPCMessage(context.getPlayer(), mDisplayName, text);
			} else {
				MessagingUtils.sendRawMessage(context.getPlayer(), text);
			}
		}
	}

	@Override
	public JsonElement serializeForClientAPI(final QuestContext context) {
		return JsonObjectBuilder.get()
			.add("type", "text")
			.add("text", mText.stream()
				.map((t) -> MessagingUtils.serializeRawMessage(context.getPlayer(), t, true).content())
				.map(JsonPrimitive::new)
				.collect(Collectors.toList())
			)
			.add("npc_name", mDisplayName)
			.build();
	}
}
