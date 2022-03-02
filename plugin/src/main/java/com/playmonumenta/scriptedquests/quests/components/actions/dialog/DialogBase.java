package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.playmonumenta.scriptedquests.quests.QuestContext;

public interface DialogBase {
	/**
	 * Function should send the dialog to the player
	 */
	void sendDialog(QuestContext context);

	default JsonElement serializeForClientAPI(QuestContext context) {
		return JsonNull.INSTANCE;
	}
}
