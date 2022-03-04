package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;

public class PrerequisiteTestForBlock implements PrerequisiteBase {
	private final Location mLoc;
	private final Material mType;

	public PrerequisiteTestForBlock(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("test_for_block value is not an object!");
		}

		Integer x = null;
		Integer y = null;
		Integer z = null;
		String typeStr = null;

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			if (key.equals("x")) {
				x = value.getAsInt();
			} else if (key.equals("y")) {
				y = value.getAsInt();
			} else if (key.equals("z")) {
				z = value.getAsInt();
			} else if (key.equals("type")) {
				typeStr = value.getAsString();
				if (typeStr == null) {
					throw new Exception("test_for_block type entry is not a string!");
				}
			} else {
				throw new Exception("Unknown location key: '" + key + "'");
			}
		}

		if (x == null || y == null || z == null || typeStr == null) {
			throw new Exception("test_for_block prerequisite requires x y z and type!");
		}

		try {
			mType = Material.valueOf(typeStr);
		} catch (IllegalArgumentException e) {
			throw new Exception("Unknown Material '" + typeStr +
								"' - it should be one of the values in this list: " +
								"https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
		}

		mLoc = new Location(null, x, y, z);
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		mLoc.setWorld(context.getPlayer().getWorld());
		return mLoc.getBlock().getType().equals(mType);
	}
}
