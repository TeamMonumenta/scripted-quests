package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.playmonumenta.scriptedquests.quests.QuestContext;

public interface ActionBase {
	void doAction(QuestContext context);

	default JsonElement serializeForClientAPI(QuestContext context) {
		return JsonNull.INSTANCE;
	}
}
