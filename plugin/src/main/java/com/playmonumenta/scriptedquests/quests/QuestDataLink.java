package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.components.QuestFieldLink;
import me.Novalescent.player.quests.QuestData;

import java.util.*;

public class  QuestDataLink {

	public final String mId;
	public final String mDisplayName;
	public final Integer mLevel;

	public Boolean mVisible = true;

	private Map<String, QuestFieldLink> mLinks = new HashMap<>();
	public QuestDataLink(JsonObject object) throws Exception {

		mId = object.get("quest_id").getAsString();
		if (mId == null) {
			throw new Exception("quest_id value is not a string!");
		}

		mDisplayName = object.get("quest_name").getAsString();
		if (mDisplayName == null) {
			throw new Exception("quest_name value is not a string!");
		}

		mLevel = object.get("level").getAsInt();
		if (mLevel == null) {
			throw new Exception("level value is not a string!");
		}

		if (object.has("visible")) {
			mVisible = object.get("visible").getAsBoolean();
		}

		JsonArray fields = object.get("quest_fields").getAsJsonArray();
		if (fields == null) {
			throw new Exception("quest_fields value is not an array!");
		}

		for (JsonElement element : fields) {
			QuestFieldLink fieldLink = new QuestFieldLink(mId, element);

			mLinks.put(fieldLink.mFieldId, fieldLink);
		}

	}

	public Collection<QuestFieldLink> getLinks() {
		return mLinks.values();
	}

	public QuestFieldLink getFieldLink(String fieldId) {
		return mLinks.get(fieldId);
	}

}
