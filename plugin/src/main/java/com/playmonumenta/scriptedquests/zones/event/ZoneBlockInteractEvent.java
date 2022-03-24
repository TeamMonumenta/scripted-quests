package com.playmonumenta.scriptedquests.zones.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.Nullable;

public class ZoneBlockInteractEvent extends ZoneEvent {

	private final Set<Material> mMaterials;
	private final @Nullable Action mClickType;

	private ZoneBlockInteractEvent(Set<Material> materials, @Nullable Action clickType, String command) {
		super(command);
		mMaterials = materials;
		mClickType = clickType;
	}

	public static ZoneBlockInteractEvent fromJson(JsonElement jsonElement) {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Set<Material> materials = new HashSet<>();
		for (JsonElement block : jsonObject.getAsJsonArray("blocks")) {
			materials.add(Material.getMaterial(block.getAsString()));
		}
		Action clickType = null;
		JsonPrimitive type = jsonObject.getAsJsonPrimitive("type");
		if (type != null) {
			clickType = switch (type.getAsString().toUpperCase(Locale.ROOT)) {
				case "RIGHT_CLICK" -> Action.RIGHT_CLICK_BLOCK;
				case "LEFT_CLICK" -> Action.LEFT_CLICK_BLOCK;
				default -> null;
			};
		}
		String command = jsonObject.getAsJsonPrimitive("command").getAsString();
		return new ZoneBlockInteractEvent(materials, clickType, command);
	}

	public Set<Material> getMaterials() {
		return mMaterials;
	}

	public boolean appliesTo(Action action, Material type) {
		return (mClickType == null || action == mClickType) && mMaterials.contains(type);
	}

}
