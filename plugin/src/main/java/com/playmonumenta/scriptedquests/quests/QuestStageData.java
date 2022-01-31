package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class QuestStageData {

	private final Map<String, Integer> mObjectives = new LinkedHashMap<>();

	public QuestStageData(JsonObject object) {
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			mObjectives.put(entry.getKey(), entry.getValue().getAsInt());
		}
	}

	public QuestStageData() {

	}

	public void setObjective(String objectiveId, int max, int change, Integer defaultIfNotPresent) {
		if (!addNewObjective(objectiveId, defaultIfNotPresent)) {
			return;
		}

		int value = Math.min(Math.max(change, 0), max);
		mObjectives.put(objectiveId, value);
	}

	public void changeObjective(String objectiveId, int max, int change, Integer defaultIfNotPresent) {
		if (!addNewObjective(objectiveId, defaultIfNotPresent)) {
			return;
		}

		int value = Math.min(Math.max(mObjectives.get(objectiveId) + change, 0), max);
		mObjectives.put(objectiveId, value);
	}

	public Integer getObjective(String objectiveId) {
		return mObjectives.get(objectiveId);
	}

	public Set<String> getObjectiveSet() {
		return mObjectives.keySet();
	}

	public boolean isStageCompleted(QuestStage stage) {
		for (QuestObjective objective : stage.getObjectives()) {
			Integer value = mObjectives.get(objective.getId());
			if (value == null) {
				return false;
			} else if (value != objective.getObjectiveMax()) {
				return false;
			}
		}
		return true;
	}

	private boolean addNewObjective(String objectiveId, Integer defaultIfNotPresent) {
		if (defaultIfNotPresent != null) {
			mObjectives.put(objectiveId, defaultIfNotPresent);
			return true;
		}
		return false;
	}


	public JsonObject getAsJsonObject() {
		JsonObject object = new JsonObject();

		for (String id : mObjectives.keySet()) {
			object.addProperty(id, mObjectives.get(id));
		}

		return object;
	}

}
