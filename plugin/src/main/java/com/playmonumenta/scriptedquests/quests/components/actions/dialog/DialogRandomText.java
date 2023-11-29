package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Random;
import org.jetbrains.annotations.Nullable;

public class DialogRandomText implements DialogBase {
	private final @Nullable String mDisplayName;
	private final ArrayList<String> mText = new ArrayList<>();
	private final Random mRandom = new Random();

	public DialogRandomText(@Nullable String displayName, JsonElement element) throws Exception {
		mDisplayName = displayName;

		if (element.isJsonPrimitive()) {
			mText.add(element.getAsString());
		} else if (element.isJsonArray()) {
			for (JsonElement jsonElement : element.getAsJsonArray()) {
				mText.add(jsonElement.getAsString());
			}
		} else {
			throw new Exception("random_text value is neither an array nor a string!");
		}
	}

	@Override
	public void sendDialog(QuestContext context) {
		int idx = mRandom.nextInt(mText.size());
		if (mDisplayName != null && !mDisplayName.isEmpty()) {
			MessagingUtils.sendNPCMessage(context.getPlayer(), mDisplayName, mText.get(idx));
		} else {
			MessagingUtils.sendRawMessage(context.getPlayer(), mText.get(idx));
		}
	}

	@Override
	public JsonElement serializeForClientAPI(QuestContext context) {
		int idx = mRandom.nextInt(mText.size());
		return JsonObjectBuilder.get()
			.add("type", "random_text")
			.add("commands", MessagingUtils.serializeRawMessage(context.getPlayer(), mText.get(idx), true).content())
			.add("npc_name", mDisplayName)
			.build();

	}
}

