package com.playmonumenta.scriptedquests.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import me.Novalescent.mobs.spells.scripted.actions.SpellActions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Model {

	public final String mId;
	public final World mWorld;
	public final Vector mPos1;
	public final Vector mPos2;
	public final BoundingBox mBox;

	public final Vector mCenter;

	private List<ModelInstance> mInstances = new ArrayList<>();
	private List<Vector> mLocations = new ArrayList<>();
	private List<ModelPart> mModelParts = new ArrayList<>();
	private List<QuestComponent> mComponents = new ArrayList<>();

	// Use Info
	public int mUseTime = 0;
	public boolean mUseDisable = false;
	public int mUseDisableTime = 0;
	public String mUseMessage = "Using...";
	public int mOnUseTickRate = 1;
	public boolean mQuestMarker = false;
	public SpellActions mOnStart;
	public SpellActions mOnTick;
	public SpellActions mOnEnd;

	public Model(Plugin plugin, JsonObject object) throws Exception {

		mId = object.get("name").getAsString();
		if (mId == null || mId.isEmpty()) {
			throw new Exception("name value is not valid!");
		}

		String worldname = object.get("world").getAsString();
		mWorld = Bukkit.getWorld(worldname);

		if (mWorld == null) {
			throw new Exception("world value is not a valid world!");
		}

		// Use Info
		JsonElement useElement = object.get("use_info");
		if (useElement == null || useElement.getAsJsonObject() == null) {
			throw new Exception("Failed to parse 'use_info'");
		}

		JsonObject useInfo = useElement.getAsJsonObject();
		for (Map.Entry<String, JsonElement> ent : useInfo.entrySet()) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {

				case "use_time":
					mUseTime = value.getAsInt();
					break;

				case "use_message":
					mUseMessage = value.getAsString();
					break;

				case "use_disable_time":
					mUseDisableTime = value.getAsInt();
					break;

				case "on_tick_rate":
					mOnUseTickRate = value.getAsInt();
					break;

				case "on_start":
					mOnStart = new SpellActions(null, null, value);
					break;

				case "on_tick":
					mOnTick = new SpellActions(null, null, value);
					break;

				case "on_end":
					mOnEnd = new SpellActions(null, null, value);
					break;

				default:
					throw new Exception("Unknown center key: '" + key + "'");
			}
		}

		// Center
		double x = 0;
		double y = 0;
		double z = 0;
		if (object.get("center") == null ||
		object.get("center").getAsJsonObject() == null) {
			throw new Exception("Failed to parse 'center'");
		}

		JsonObject centerJson = object.get("center").getAsJsonObject();
		for (Map.Entry<String, JsonElement> ent : centerJson.entrySet()) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {
				case "x":
					x = value.getAsDouble();
					break;

				case "y":
					y = value.getAsDouble();
					break;

				case "z":
					z = value.getAsDouble();
					break;

				default:
					throw new Exception("Unknown center key: '" + key + "'");
			}
		}
		mCenter = new Vector(x, y, z);

		// Locations
		if (object.get("locations") == null ||
			object.get("locations").getAsJsonArray() == null) {
			throw new Exception("Failed to parse 'locations'");
		}

		// Region
		Double[] corners = new Double[6];
		// Load the zone location
		if (object.get("region") == null ||
			object.get("region").getAsJsonObject() == null) {
			throw new Exception("Failed to parse 'region'");
		}
		JsonObject locationJson = object.get("region").getAsJsonObject();
		for (Map.Entry<String, JsonElement> ent : locationJson.entrySet()) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();
			switch (key) {
				case "x1":
					corners[0] = value.getAsDouble();
					break;
				case "y1":
					corners[1] = value.getAsDouble();
					break;
				case "z1":
					corners[2] = value.getAsDouble();
					break;
				case "x2":
					corners[3] = value.getAsDouble();
					break;
				case "y2":
					corners[4] = value.getAsDouble();
					break;
				case "z2":
					corners[5] = value.getAsDouble();
					break;
				default:
					throw new Exception("Unknown region key: '" + key + "'");
			}
		}
		for (Double cornerAxis : corners) {
			if (cornerAxis == null) {
				throw new Exception("region must have x1 x2 y1 y2 z1 and z2");
			}
		}
		mPos1 = new Vector(corners[0], corners[1], corners[2]);
		mPos2 = new Vector(corners[3], corners[4], corners[5]);

		mBox = BoundingBox.of(mPos1, mPos2);
		for (Entity e : mWorld.getNearbyEntities(mBox)) {
			if (e instanceof ArmorStand) {
				ArmorStand stand = (ArmorStand) e;
				mModelParts.add(new ModelPart(stand, mCenter));
			}
		}

		// Components
		JsonArray components = object.get("quest_components").getAsJsonArray();
		for (JsonElement element : components) {
			QuestComponent component = new QuestComponent("", "", EntityType.ARMOR_STAND, element);
			mComponents.add(component);
		}

		if (object.has("quest_marker")) {
			mQuestMarker = object.get("quest_marker").getAsBoolean();
		}

		// Spawn @ Locations
		JsonArray array = object.get("locations").getAsJsonArray();
		for (JsonElement element : array) {
			JsonObject locObject = element.getAsJsonObject();

			if (locObject == null) {
				throw new Exception("locations array entry is not a valid location");
			}

			x = 0;
			y = 0;
			z = 0;
			double yaw = -1;
			for (Map.Entry<String, JsonElement> ent : locObject.entrySet()) {
				String key = ent.getKey();
				JsonElement value = ent.getValue();

				switch (key) {
					case "x":
						x = value.getAsDouble();
						break;

					case "y":
						y = value.getAsDouble();
						break;

					case "z":
						z = value.getAsDouble();
						break;

					case "yaw":
						yaw = value.getAsDouble();
						break;

					default:
						throw new Exception("Unknown locations key: '" + key + "'");
				}
			}
			Vector vec = new Vector(x, y, z);
			Location loc = vec.toLocation(mWorld);
			ModelInstance instance = new ModelInstance(plugin, this, loc, yaw);
			// instance.toggle();

			ModelTreeNode node = new ModelTreeNode(instance);
			node.mQuestMarkers = mQuestMarker;
			plugin.mModelManager.mQuadTree.add(node);
			mInstances.add(instance);
		}
	}

	public List<ModelPart> getModelParts() {
		return mModelParts;
	}

	public List<ModelInstance> getInstances() {
		return mInstances;
	}

	public List<QuestComponent> getComponents() {
		return mComponents;
	}

	public double getHeight() {
		return mBox.getHeight();
	}

}
