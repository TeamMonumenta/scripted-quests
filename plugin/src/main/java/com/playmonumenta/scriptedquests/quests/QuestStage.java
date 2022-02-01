package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class QuestStage {

	private final List<QuestObjective> mObjectives = new ArrayList<>();
	public QuestStage(JsonObject object) {

		JsonArray objectives = object.getAsJsonArray("quest_objectives");
		for (JsonElement element : objectives) {
			mObjectives.add(new QuestObjective(element.getAsJsonObject(), false));
		}

	}

	public QuestStage(JsonArray array) {

		for (JsonElement element : array) {
			mObjectives.add(new QuestObjective(element.getAsJsonObject(), true));
		}

	}

	public QuestObjective getObjective(String objectiveId) {
		for (QuestObjective objective : mObjectives) {
			if (objective.getId().equals(objectiveId)) {
				return objective;
			}
		}
		return null;
	}

	public List<QuestObjective> getObjectives() {
		return mObjectives;
	}

	public QuestStageData getNewStageData(String questId) {
		JsonObject object = new JsonObject();

		for (QuestObjective objective : mObjectives) {
			object.addProperty(objective.getId(), objective.getObjectiveDefault());
		}

		return new QuestStageData(object);
	}

	public void messageObjectives(Player player) {
		for (QuestObjective objectives : mObjectives) {
			if (objectives.isVisible()) {
				player.sendMessage("  " + ChatColor.of("#FEF2C1") + objectives.getDescription());
			}
		}
	}

}
