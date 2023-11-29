package com.playmonumenta.scriptedquests.zones.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.ParseResults;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import java.util.HashSet;
import java.util.Set;

public class ZoneEntityDeathEvent extends ZoneEvent {

	private final Set<String> mEntityNames;

	private ZoneEntityDeathEvent(Set<String> names, String command) {
		super(command);
		this.mEntityNames = names;
	}

	public static ZoneEntityDeathEvent fromJson(JsonElement jsonElement) throws Exception {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Set<String> names = new HashSet<>();
		for (JsonElement name : JsonUtils.getJsonArray(jsonObject, "entity_names")) {
			names.add(name.getAsString());
		}
		String command = JsonUtils.getString(jsonObject, "command");
		ParseResults<?> pr = NmsUtils.getVersionAdapter().parseCommand(command);
		if (pr != null && pr.getReader().canRead()) {
			throw new Exception("Invalid command: '" + command + "'");
		}
		return new ZoneEntityDeathEvent(names, command);
	}

	public Set<String> getEntityNames() {
		return mEntityNames;
	}

	public boolean appliesTo(String entityName) {
		// Optional: if no names are specified, it applies to every entity that dies in the zone.
		return mEntityNames.size() == 0 || mEntityNames.contains(entityName);
	}

}
