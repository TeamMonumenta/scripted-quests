package com.playmonumenta.scriptedquests.zones.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class ZoneBlockBreakEvent implements ZoneEvent {

	private final Set<Material> mMaterials;
	private final String mCommand;

	private ZoneBlockBreakEvent(Set<Material> materials, String command) {
		this.mMaterials = materials;
		this.mCommand = command;
	}

	public static ZoneBlockBreakEvent fromJson(JsonElement jsonElement) {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Set<Material> materials = new HashSet<>();
		for (JsonElement block : jsonObject.getAsJsonArray("blocks")) {
			materials.add(Material.getMaterial(block.getAsString()));
		}
		String command = jsonObject.getAsJsonPrimitive("command").getAsString();
		return new ZoneBlockBreakEvent(materials, command);
	}

	public Set<Material> getMaterials() {
		return mMaterials;
	}

	public boolean appliesTo(Material type) {
		return mMaterials.contains(type);
	}

	public void execute(Entity entity, Block block) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
			"execute as " + entity.getUniqueId() + " at @s positioned "
				+ block.getX() + " " + block.getY() + " " + block.getZ() + " run " + mCommand);
	}

	public void execute(Block breaker, Block broken) {
		NmsUtils.getVersionAdapter().executeCommandAsBlock(breaker,
			"execute positioned " + broken.getX() + " " + broken.getY() + " " + broken.getZ() + " run " + mCommand);
	}

}
