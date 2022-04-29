package com.playmonumenta.scriptedquests.zones.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import com.playmonumenta.scriptedquests.utils.MaterialUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.Nullable;

public class ZoneRemoteClickEvent extends ZoneEvent {

	private final @Nullable Set<Material> mMaterials;
	private final int mMaxDistance;
	private final boolean mIgnoreTransparentBlocks;
	private final @Nullable Action mClickType;
	private final int mMinTicksBetweenClicks;
	private final Map<UUID, Integer> mLastClicks = new HashMap<>();

	private ZoneRemoteClickEvent(@Nullable Set<Material> materials, @Nullable Action clickType, int maxDistance, boolean ignoreTransparentBlocks, int minTicksBetweenClicks, String command) {
		super(command);
		mMaterials = materials;
		mMaxDistance = maxDistance;
		mIgnoreTransparentBlocks = ignoreTransparentBlocks;
		mClickType = clickType;
		mMinTicksBetweenClicks = minTicksBetweenClicks;
	}

	public static ZoneRemoteClickEvent fromJson(JsonElement jsonElement) throws Exception {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Set<Material> materials = null;
		JsonArray blocks = jsonObject.getAsJsonArray("blocks");
		if (blocks != null) {
			materials = new HashSet<>();
			for (JsonElement block : blocks) {
				materials.add(Material.getMaterial(block.getAsString()));
			}
		}
		Action clickType = JsonUtils.parse(jsonObject, "type", type -> switch (type.toUpperCase(Locale.ROOT)) {
			case "RIGHT_CLICK" -> Action.RIGHT_CLICK_BLOCK;
			case "LEFT_CLICK" -> Action.LEFT_CLICK_BLOCK;
			default -> throw new RuntimeException("Invalid click type " + type);
		}, null);
		String command = JsonUtils.getString(jsonObject, "command");
		int maxDistance = JsonUtils.getInt(jsonObject, "max_distance");
		boolean ignoreTransparentBlocks = JsonUtils.getBoolean(jsonObject, "ignore_transparent_blocks");
		int minTicksBetweenClicks = JsonUtils.getInt(jsonObject, "min_ticks_between_clicks", 1);
		return new ZoneRemoteClickEvent(materials, clickType, maxDistance, ignoreTransparentBlocks, minTicksBetweenClicks, command);
	}

	public @Nullable Block getBlock(Player player, Action action) {

		// check click type
		if (mClickType != null
			    && action != mClickType
			    && (action != Action.RIGHT_CLICK_AIR || mClickType != Action.RIGHT_CLICK_BLOCK)
			    && (action != Action.LEFT_CLICK_AIR || mClickType != Action.LEFT_CLICK_BLOCK)) {
			return null;
		}

		// check that this event is not executed too often
		int currentTick = Bukkit.getCurrentTick();
		mLastClicks.values().removeIf(val -> val + mMinTicksBetweenClicks < currentTick); // clean up old click data for any player
		if (mLastClicks.containsKey(player.getUniqueId())) { // if there's still a stored click for this player, then it was too recent
			return null;
		}
		mLastClicks.put(player.getUniqueId(), currentTick);

		// find the targeted block
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
		return null;
	}
}
