package com.playmonumenta.scriptedquests.races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RaceWaypoint {
	private final Vector mPosition;
	private final @Nullable QuestActions mActions;

	public RaceWaypoint(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("waypoints value is not an object!");
		}

		// x
		JsonElement xElement = object.get("x");
		double x = 0;
		if (xElement != null) {
			x = xElement.getAsDouble();
		} else {
			throw new Exception("waypoints entry missing x value!");
		}

		// y
		JsonElement yElement = object.get("y");
		double y = 0;
		if (yElement != null) {
			y = yElement.getAsDouble();
		} else {
			throw new Exception("waypoints entry missing y value!");
		}

		// z
		JsonElement zElement = object.get("z");
		double z = 0;
		if (zElement != null) {
			z = zElement.getAsDouble();
		} else {
			throw new Exception("waypoints entry missing z value!");
		}

		mPosition = new Vector(x, y, z);

		// actions
		JsonElement actionsElement = object.get("actions");
		if (actionsElement != null) {
			// Actions should not use NPC dialog or rerun_components since they make no sense here
			mActions = new QuestActions("REPORT_THIS_BUG", "REPORT_THIS_BUG", null, 0, actionsElement);
		} else {
			mActions = null;
		}
	}

	public Vector getPosition() {
		return mPosition.clone();
	}

	public void doActions(QuestContext context) {
		if (mActions != null) {
			mActions.doActions(context);
		}
	}
}
