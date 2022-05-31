package com.playmonumenta.scriptedquests.zones.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;

public class ZoneBlockBreakEvent extends ZoneEvent {

	private final Set<Material> mMaterials;

	private ZoneBlockBreakEvent(Set<Material> materials, String command) {
		super(command);
		this.mMaterials = materials;
	}

	public static ZoneBlockBreakEvent fromJson(JsonElement jsonElement) throws Exception {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Set<Material> materials = new HashSet<>();
		for (JsonElement block : JsonUtils.getJsonArray(jsonObject, "blocks")) {
			materials.add(Material.getMaterial(block.getAsString()));
		}
		String command = JsonUtils.getString(jsonObject, "command");
		return new ZoneBlockBreakEvent(materials, command);
	}

	public Set<Material> getMaterials() {
		return mMaterials;
	}

	public boolean appliesTo(Material type) {
		return mMaterials.contains(type);
	}

}
