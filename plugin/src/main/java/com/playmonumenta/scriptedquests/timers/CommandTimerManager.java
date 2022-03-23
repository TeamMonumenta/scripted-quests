package com.playmonumenta.scriptedquests.timers;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.scriptedquests.Plugin;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandTimerManager implements Listener {
	private final Plugin mPlugin;
	private final Map<Integer, CommandTimer> mCommandTimers;
	private final BukkitRunnable mRunnable;

	public CommandTimerManager(Plugin plugin) {
		mPlugin = plugin;
		mCommandTimers = new LinkedHashMap<Integer, CommandTimer>();

		/* When starting up, look for timer armor stands in all current world entities */
		for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
			if (!(entity instanceof ArmorStand)) {
				continue;
			}

			processEntity(entity);
		}

		mRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				for (CommandTimer timer : mCommandTimers.values()) {
					timer.runTimers();
				}
			}
		};

		mRunnable.runTaskTimer(plugin, 0, 1);
	}

	/********************************************************************************
	 * Event Handlers
	 *******************************************************************************/

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		processEntity(event.getEntity());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void rntityRemoveFromWorldEvent(EntityRemoveFromWorldEvent event) {
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
				timer.unload(entity);
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

	private @Nullable Integer getPeriod(Entity entity, Set<String> tags) {
		if (tags != null && tags.contains("timer")) {
			for (String tag : tags) {
				if (tag.startsWith("period=")) {
					Integer period;
					try {
						period = Integer.parseInt(tag.substring(7));
					} catch (Exception e) {
						mPlugin.getLogger().severe("Timer armor stand has invalid period '" + tag + "' at " + entity.getLocation().toString());
						return null;
					}
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
