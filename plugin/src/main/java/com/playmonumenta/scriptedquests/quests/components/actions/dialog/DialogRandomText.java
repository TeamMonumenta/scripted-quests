package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

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
	public void sendDialog(QuestContext context) {
		int idx = mRandom.nextInt(mText.size());
		MessagingUtils.sendNPCMessage(context.getPlayer(), mDisplayName, mText.get(idx));
	}

	@Override
	public JsonElement serializeForClientAPI(QuestContext context) {
		int idx = mRandom.nextInt(mText.size());
		return JsonObjectBuilder.get()
			.add("type", "random_text")
			.add("commands", mText.get(idx))
			.add("npc_name", mDisplayName)
			.build();

	}
}

