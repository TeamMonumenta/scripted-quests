package com.playmonumenta.scriptedquests.races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestActions;

import org.bukkit.entity.Player;

public class RaceTime {
	private final String mLabel;
	private final int mTime;
	private final String mColor;
	private final QuestActions mActions;

	public RaceTime(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("times value is not an object!");
		}

		// label
		JsonElement labelElement = object.get("label");
		if (labelElement != null) {
			mLabel = labelElement.getAsString();
		} else {
			throw new Exception("times entry missing label value!");
		}

		// time
		JsonElement timeElement = object.get("time");
		double timeInSeconds;
		if (timeElement != null) {
			timeInSeconds = timeElement.getAsDouble();
		} else {
			throw new Exception("times entry missing time value!");
		}
		mTime = (int)(timeInSeconds * 1000);


		// color
		JsonElement colorElement = object.get("color");
		if (colorElement != null) {
			mColor = colorElement.getAsString();
		} else {
			throw new Exception("times entry missing color value!");
		}

		// actions
		JsonElement actionsElement = object.get("actions");
		if (actionsElement != null) {
			// Actions should not use NPC dialog or rerun_components since they make no sense here
			mActions = new QuestActions("REPORT_THIS_BUG", "REPORT_THIS_BUG", null, 0, actionsElement);
		} else {
			mActions = null;
		}
	}

	public void doActions(Plugin plugin, Player player) {
		if (mActions != null) {
			mActions.doActions(plugin, player, null);
		}
	}

	public String getLabel() {
		return mLabel;
	}

	public int getTime() {
		return mTime;
	}

	public String getColor() {
		return mColor;
	}
}
