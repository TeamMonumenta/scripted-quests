package com.playmonumenta.scriptedquests.utils;

import java.util.Map;

import org.bukkit.util.BoundingBox;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtils {
	public static Vector getAsVector(JsonElement element, String label) throws Exception {
		if (element == null) {
			if (label == null) {
				throw new Exception("Failed to parse Vector");
			} else {
				throw new Exception("Failed to parse Vector '" + label + "'");
			}
		}
		return getAsVector(element.getAsJsonObject(), label);
	}

	public static Vector getAsVector(JsonObject object, String label) throws Exception {
		Double x = 0.0;
		Double y = 0.0;
		Double z = 0.0;
		for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
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
				if (label == null) {
					throw new Exception("Unknown Vector key: '" + key + "'");
				} else {
					throw new Exception("Unknown Vector key: '" + key + "' in '" + label + "'");
				}
			}
		}
		return new Vector(x, y, z);
	}

	public static JsonObject toJsonObject(Vector vector) {
		if (vector == null) {
			return null;
		}

		JsonObject result = new JsonObject();
		result.addProperty("x", vector.getX());
		result.addProperty("y", vector.getY());
		result.addProperty("z", vector.getZ());
		return result;
	}

	public static Location getAsLocation(JsonElement element, String label) throws Exception {
		if (element == null) {
			if (label == null) {
				throw new Exception("Failed to parse Location");
			} else {
				throw new Exception("Failed to parse Location '" + label + "'");
			}
		}
		return getAsLocation(element.getAsJsonObject(), label);
	}

	public static Location getAsLocation(JsonObject object, String label) throws Exception {
		Double x = 0.0;
		Double y = 0.0;
		Double z = 0.0;
		Float yaw = 0.0f;
		Float pitch = 0.0f;
		for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
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
				yaw = value.getAsFloat();
				break;
			case "pitch":
				pitch = value.getAsFloat();
				break;
			default:
				if (label == null) {
					throw new Exception("Unknown Location key: '" + key + "'");
				} else {
					throw new Exception("Unknown Location key: '" + key + "' in '" + label + "'");
				}
			}
		}
		return new Location(null, x, y, z, yaw, pitch);
	}

	public static JsonObject toJsonObject(Location loc) {
		if (loc == null) {
			return null;
		}

		JsonObject result = new JsonObject();
		result.addProperty("x", loc.getX());
		result.addProperty("y", loc.getY());
		result.addProperty("z", loc.getZ());
		result.addProperty("yaw", loc.getYaw());
		result.addProperty("pitch", loc.getPitch());
		return result;
	}

	public static BoundingBox getAsBoundingBox(JsonElement element, String label) throws Exception {
		if (element == null) {
			if (label == null) {
				throw new Exception("Failed to parse BoundingBox");
			} else {
				throw new Exception("Failed to parse BoundingBox '" + label + "'");
			}
		}
		return getAsBoundingBox(element.getAsJsonObject(), label);
	}

	public static BoundingBox getAsBoundingBox(JsonObject object, String label) throws Exception {
		Double x1 = 0.0;
		Double y1 = 0.0;
		Double z1 = 0.0;
		Double x2 = 0.0;
		Double y2 = 0.0;
		Double z2 = 0.0;
		for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();
			switch (key) {
			case "x1":
				x1 = value.getAsDouble();
				break;
			case "y1":
				y1 = value.getAsDouble();
				break;
			case "z1":
				z1 = value.getAsDouble();
				break;
			case "x2":
				x2 = value.getAsDouble();
				break;
			case "y2":
				y2 = value.getAsDouble();
				break;
			case "z2":
				z2 = value.getAsDouble();
				break;
			default:
				if (label == null) {
					throw new Exception("Unknown BoundingBox key: '" + key + "'");
				} else {
					throw new Exception("Unknown BoundingBox key: '" + key + "' in '" + label + "'");
				}
			}
		}
		return new BoundingBox(x1, y1, z1, x2, y2, z2);
	}

	public static JsonObject toJsonObject(BoundingBox bb) throws Exception {
		if (bb == null) {
			return null;
		}

		JsonObject result = new JsonObject();
		result.addProperty("x1", bb.getMinX());
		result.addProperty("y1", bb.getMinY());
		result.addProperty("z1", bb.getMinZ());
		result.addProperty("x2", bb.getMaxX());
		result.addProperty("y2", bb.getMaxY());
		result.addProperty("z2", bb.getMaxZ());
		return result;
	}
}
