package com.playmonumenta.scriptedquests.zones.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.utils.MaterialUtils;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.Nullable;

public class ZoneRemoteClickEvent extends ZoneEvent {

	private final @Nullable Set<Material> mMaterials;
	private final int mMaxDistance;
	private final boolean mIgnoreTransparentBlocks;
	private final @Nullable Action mClickType;

	private ZoneRemoteClickEvent(@Nullable Set<Material> materials, @Nullable Action clickType, int maxDistance, boolean ignoreTransparentBlocks, String command) {
		super(command);
		mMaterials = materials;
		mMaxDistance = maxDistance;
		mIgnoreTransparentBlocks = ignoreTransparentBlocks;
		mClickType = clickType;
	}

	public static ZoneRemoteClickEvent fromJson(JsonElement jsonElement) {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Set<Material> materials = null;
		JsonArray blocks = jsonObject.getAsJsonArray("blocks");
		if (blocks != null) {
			materials = new HashSet<>();
			for (JsonElement block : blocks) {
				materials.add(Material.getMaterial(block.getAsString()));
			}
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
		int maxDistance = jsonObject.getAsJsonPrimitive("max_distance").getAsInt();
		boolean ignoreTransparentBlocks = jsonObject.getAsJsonPrimitive("ignore_transparent_blocks").getAsBoolean();
		return new ZoneRemoteClickEvent(materials, clickType, maxDistance, ignoreTransparentBlocks, command);
	}

	public @Nullable Block getBlock(PlayerInteractEvent event) {
		if (mClickType == null
			    || event.getAction() == mClickType
			    || (event.getAction() == Action.RIGHT_CLICK_AIR && mClickType == Action.RIGHT_CLICK_BLOCK)
			    || (event.getAction() == Action.LEFT_CLICK_AIR && mClickType == Action.LEFT_CLICK_BLOCK)) {
			Player player = event.getPlayer();
			BlockIterator iter = new BlockIterator(player.getWorld(), player.getEyeLocation().toVector(), player.getEyeLocation().getDirection(), 0, mMaxDistance);
			while (iter.hasNext()) {
				Block block = iter.next();
				Material type = block.getType();
				if (mMaterials != null && mMaterials.contains(type)) {
					return block;
				}
				if (MaterialUtils.isOccluding(type) || (!type.isAir() && !mIgnoreTransparentBlocks)) {
					return mMaterials == null ? block : null;
				}
			}
		}
		return null;
	}

}
