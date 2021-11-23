package com.playmonumenta.scriptedquests.quests.points;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class QuestPoint implements Cloneable {

	private Plugin mPlugin;
	private JsonObject mJson;
	private Location mLocation;
	private double mRadius;
	private QuestPrerequisites mVisibilityPrerequisites;
	private List<QuestComponent> mComponents = new ArrayList<>();
	public QuestPoint(Plugin plugin, JsonObject object) throws Exception {
		mPlugin = plugin;
		mJson = object;
		mLocation = Utils.getLocation(object.get("location").getAsString(), false);

		mRadius = object.get("radius").getAsDouble();

		if (object.has("visibilityPrerequisites")) {
			mVisibilityPrerequisites = new QuestPrerequisites(object.get("visibilityPrerequisites"));
		}

		if (object.has("quest_components")) {
			JsonArray onFail = object.get("quest_components").getAsJsonArray();
			for (JsonElement element : onFail) {
				QuestComponent component = new QuestComponent("", "", EntityType.ARMOR_STAND, element);
				mComponents.add(component);
			}
		}
	}

	public Location getLocation() {
		return mLocation;
	}

	public boolean canSee(Player player) {
		return mVisibilityPrerequisites != null
			&& mVisibilityPrerequisites.getPrerequisites().size() > 0
			&& mVisibilityPrerequisites.prerequisiteMet(player, player);
	}

	public double getRadius() {
		return mRadius;
	}

	public void checkAndActivate(Player player) {
		if (player.getLocation().distance(mLocation) <= mRadius) {
			for (QuestComponent component : mComponents) {
				component.doActionsIfPrereqsMet(mPlugin, player, null);
			}
		}
	}

	@Override
	public QuestPoint clone() {
		try {
			return new QuestPoint(mPlugin, mJson);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
