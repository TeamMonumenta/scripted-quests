package com.playmonumenta.scriptedquests.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Map;

public class Model {

	public final String mId;
	public final World mWorld;
	public final Vector mPos1;
	public final Vector mPos2;
	public Model(JsonObject object) throws Exception {

		mId = object.get("name").getAsString();
		if (mId == null || mId.isEmpty()) {
			throw new Exception("name value is not valid!");
		}

		String worldname = object.get("world").getAsString();
		mWorld = Bukkit.getWorld(worldname);

		if (mWorld == null) {
			throw new Exception("world value is not a valid world!");
		}

		Double[] corners = new Double[6];
		// Load the zone location
		if (object.get("location") == null ||
			object.get("location").getAsJsonObject() == null) {
			throw new Exception("Failed to parse 'location'");
		}
		JsonObject locationJson = object.get("location").getAsJsonObject();
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
					throw new Exception("Unknown location key: '" + key + "'");
			}
		}
		for (Double cornerAxis : corners) {
			if (cornerAxis == null) {
				throw new Exception("Location prereq must have x1 x2 y1 y2 z1 and z2");
			}
		}
		mPos1 = new Vector(corners[0], corners[1], corners[2]);
		mPos2 = new Vector(corners[3], corners[4], corners[5]);
	}

}
