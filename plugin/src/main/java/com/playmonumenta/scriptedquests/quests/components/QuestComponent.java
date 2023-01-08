package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class QuestComponent {
	private @Nullable QuestPrerequisites mPrerequisites = null;
	private QuestActions mActions;

	public QuestComponent(String npcName, String displayName,
	                      EntityType entityType, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("quest_components value is not an object!");
		}

		// Read the delay_actions_by_ticks field first, if specified
		JsonElement delayElement = object.get("delay_actions_by_ticks");
		int delayTicks = 0;
		if (delayElement != null) {
			delayTicks = delayElement.getAsInt();
		}

		QuestActions actions = null;

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("prerequisites") && !key.equals("actions")
			&& !key.equals("delay_actions_by_ticks")) {
				throw new Exception("Unknown quest_components key: " + key);
			}

			// All quest_components entries are single JSON things that should be passed
			// to their respective handlers
			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("quest_components value for key '" + key + "' is not parseable!");
			}

			if (key.equals("prerequisites")) {
				mPrerequisites = new QuestPrerequisites(value);
			} else if (key.equals("actions")) {
				actions = new QuestActions(npcName, displayName, entityType, delayTicks, value);
			}
		}

		if (actions == null) {
			throw new Exception("quest_components value without an action!");
		}

		mActions = actions;
	}

	public boolean doActionsIfPrereqsMet(QuestContext context) {
		if (mPrerequisites == null || mPrerequisites.prerequisiteMet(context)) {
			mActions.doActions(context.withPrerequisites(mPrerequisites));
			return true;
		}
		return false;
	}

	public Optional<JsonElement> serializeForClientAPI(QuestContext context) {
		if (mPrerequisites == null || mPrerequisites.prerequisiteMet(context)) {
			JsonElement sub = mActions.serializeForClientAPI(context);
			if (!sub.equals(JsonNull.INSTANCE)) {
				return Optional.of(sub);
			}
		}
		return Optional.empty();
	}
}
