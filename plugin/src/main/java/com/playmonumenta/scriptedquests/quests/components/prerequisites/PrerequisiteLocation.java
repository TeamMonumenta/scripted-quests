package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.point.AreaBounds;
import com.playmonumenta.scriptedquests.point.Point;

public class PrerequisiteLocation implements PrerequisiteBase {
	private AreaBounds mAreaBounds;

	public PrerequisiteLocation(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("location value is not an object!");
		}

		Integer x1 = null;
		Integer x2 = null;
		Integer y1 = null;
		Integer y2 = null;
		Integer z1 = null;
		Integer z2 = null;

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			if (key.equals("x1")) {
				x1 = value.getAsInt();
			} else if (key.equals("x2")) {
				x2 = value.getAsInt();
			} else if (key.equals("y1")) {
				y1 = value.getAsInt();
			} else if (key.equals("y2")) {
				y2 = value.getAsInt();
			} else if (key.equals("z1")) {
				z1 = value.getAsInt();
			} else if (key.equals("z2")) {
				z2 = value.getAsInt();
			} else {
				throw new Exception("Unknown location key: '" + key + "'");
			}
		}

		if (x1 == null || x2 == null || y1 == null || y2 == null || z1 == null || z2 == null) {
			throw new Exception("Location prereq must have x1 x2 y1 y2 z1 and z2");
		}

		mAreaBounds = new AreaBounds("", new Point(x1, y1, z1), new Point(x2, y2, z2));
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		return mAreaBounds.within(entity.getLocation());
	}
}
