package com.playmonumenta.scriptedquests.timers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.playmonumenta.scriptedquests.Plugin;

public class CommandTimerManager implements Listener {

	private final Plugin mPlugin;
	private final Map<Integer, CommandTimer> mCommandTimers;

	public CommandTimerManager(Plugin plugin) {
		mPlugin = plugin;
		mCommandTimers = new LinkedHashMap<Integer, CommandTimer>();

		/* When starting up, look for timer armor stands in all current world entities */
		for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
			if (!(entity instanceof ArmorStand)) {
				continue;
			}

			processEntity((ArmorStand)entity);
		}
	}

	/********************************************************************************
	 * Event Handlers
	 *******************************************************************************/

	@EventHandler(priority = EventPriority.LOWEST)
	public void EntitySpawnEvent(EntitySpawnEvent event) {
		processEntity(event.getEntity());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkLoadEvent(ChunkLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			processEntity(entity);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void ChunkUnloadEvent(ChunkUnloadEvent event) {
		Entity[] entities = event.getChunk().getEntities();

		for (Entity entity : entities) {
			unload(entity);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void EntityDeathEvent(EntityDeathEvent event) {
		unload(event.getEntity());
	}


	/********************************************************************************
	 * Public Methods
	 *******************************************************************************/

	public void unload(Entity entity) {
		if (entity != null && (entity instanceof ArmorStand)) {
			Set<String> tags = entity.getScoreboardTags();

			Integer period = getPeriod(entity, tags);
			if (period != null) {
				CommandTimer timer = mCommandTimers.get(period);
				if (timer == null) {
					/* Out of range */
					mPlugin.getLogger().severe("Attempted to remove timer armor stand with non-tracked period " + period.toString());
					return;
				}
				timer.unload((ArmorStand)entity);
			}
		}
	}

	public void unloadAll() {
		for (CommandTimer timer : mCommandTimers.values()) {
			timer.unloadAndAbort();
		}
		mCommandTimers.clear();
	}

	/********************************************************************************
	 * Private Methods
	 *******************************************************************************/

	private void processEntity(Entity entity) {
		if (entity != null && (entity instanceof ArmorStand)) {
			Set<String> tags = entity.getScoreboardTags();

			Integer period = getPeriod(entity, tags);
			if (period != null) {
				CommandTimer timer = mCommandTimers.get(period);
				if (timer == null) {
					timer = new CommandTimer(mPlugin, period);
					mCommandTimers.put(period, timer);
				}
				timer.addEntity((ArmorStand)entity, tags);
			}
		}
	}

	private Integer getPeriod(Entity entity, Set<String> tags) {
		if (tags != null && tags.contains("timer")) {
			for (String tag : tags) {
				if (tag.startsWith("period=")) {
					Integer period = Integer.parseInt(tag.substring(7));
					if (period == null || period <= 0 || period > 72000) {
						/* Out of range */
						mPlugin.getLogger().severe("Timer armor stand has invalid period '" + tag + "' at " + entity.getLocation().toString());
						return null;
					}
					/* Good value - return it */
					return period;
				}
			}
		}

		return null;
	}

	public void tellTimers(CommandSender sender, boolean enabledOnly) {
		for (CommandTimer timer : mCommandTimers.values()) {
			timer.tellTimers(sender, enabledOnly);
		}
	}
}
