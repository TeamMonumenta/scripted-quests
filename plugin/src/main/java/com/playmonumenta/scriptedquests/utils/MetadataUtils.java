package com.playmonumenta.scriptedquests.utils;

import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class MetadataUtils {
	/* This method can be used to wrap code that should only execute once per tick
	 * for a given metadata object (block, entity, etc.).
	 *
	 * If this function returns true, the program should proceed - this is the
	 * first time it has been invoked on this particular tick
	 *
	 * If this function returns false, then it has been called already this tick
	 * for this entity / metakey pair. When returning false, the code should not
	 * be executed.
	 */
	public static boolean checkOnceThisTick(Plugin plugin, Entity entity, String metakey) {
		if (entity.hasMetadata(metakey)
		    && entity.getMetadata(metakey).get(0).asInt() == entity.getTicksLived()) {
			return false;
		}

		entity.setMetadata(metakey, new FixedMetadataValue(plugin, entity.getTicksLived()));
		return true;
	}

	/**
	 * This is just another way to check if a certain metakey has been called.
	 *
	 * Comes with the ability to offset the tick amount being checked if ever needed (used for BukkitRunnables)
	 *
	 * @param entity The entity being checked
	 * @param metakey A unique key that will be checked
	 * @param tickOffset Offsets the tick amount checked
	 * @return A true/false. If true, this has been called already. If false, it has not been called.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean happenedThisTick(Entity entity, String metakey, int tickOffset) {
		return entity.hasMetadata(metakey)
		       && entity.getMetadata(metakey).get(0).asInt() == entity.getTicksLived() + tickOffset;
	}

	/**
	 * Yet another way to check if a metakey was called
	 *
	 * If #checkOnceThisTick was called with the same metakey within the past tickRange ticks, return true
	 *
	 * @param entity The entity being checked
	 * @param metakey A unique key that will be checked
	 * @param tickOffset Offsets the tick amount checked
	 * @return A true/false. If true, this has been called already. If false, it has not been called.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean happenedInRecentTicks(Entity entity, String metakey, int tickOffset) {
		return entity.hasMetadata(metakey)
			&& entity.getMetadata(metakey).get(0).asInt() + tickOffset >= entity.getTicksLived();
	}

	public static void removeAllMetadata(Plugin plugin) {
		NmsUtils.getVersionAdapter().removeAllMetadata(plugin);
	}

}
