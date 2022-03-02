package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public class PrerequisiteFacing implements PrerequisiteBase {
	private final Vector mLoc;
	private final double mMinAlignment;

	public PrerequisiteFacing(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("location value is not an object!");
		}

		Double x = null;
		Double y = null;
		Double z = null;
		Double minAlignment = null;

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			if (key.equals("x")) {
				x = value.getAsDouble();
			} else if (key.equals("y")) {
				y = value.getAsDouble();
			} else if (key.equals("z")) {
				z = value.getAsDouble();
			} else if (key.equals("min_alignment")) {
				minAlignment = value.getAsDouble();
			} else {
				throw new Exception("Unknown facing key: '" + key + "'");
			}
		}

		if (x == null || y == null || z == null || minAlignment == null) {
			throw new Exception("Location prereq must have x y z and min_alignment");
		}

		mLoc = new Vector(x, y, z);
		mMinAlignment = minAlignment;
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		final Location entityLoc;
		Entity entity = context.getEntityUsedForPrerequisites();
		if (entity instanceof Mob) {
			entityLoc = ((Mob) entity).getEyeLocation();
		} else {
			entityLoc = entity.getLocation();
		}

		Vector toTarget = mLoc.clone().subtract(entityLoc.toVector()).normalize();
		Vector facing = entityLoc.getDirection().normalize();
		return facing.dot(toTarget) >= mMinAlignment;
	}
}
