package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.models.ModelTreeNode;
import com.playmonumenta.scriptedquests.scriptedtimer.Timer;
import com.playmonumenta.scriptedquests.scriptedtimer.TimerTreeNode;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import me.Novalescent.utils.quadtree.reworked.QuadTree;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TimerManager {

	private static BukkitRunnable mTimerRunnable = null;
	private final Plugin mPlugin;
	private List<Timer> mTimers = new ArrayList<>();
	public Map<UUID, QuadTree<TimerTreeNode>> mQuadTrees = new HashMap<>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		saveTimers();
		for (QuadTree quadTree : mQuadTrees.values()) {
			quadTree.destroy();
		}

		mQuadTrees.clear();
		mTimers.clear();

		QuestUtils.loadScriptedQuests(plugin, "timers", sender, (object) -> {
			Timer timer = new Timer(plugin, object);

			timer.updateTimer(System.currentTimeMillis());
			mTimers.add(timer);

			return timer.mId + ":" + timer.getResetCounter();
		});


		if (mTimerRunnable != null && !mTimerRunnable.isCancelled()) {
			mTimerRunnable.cancel();
		}

		mTimerRunnable = new BukkitRunnable() {

			int t = 0;
			@Override
			public void run() {
				t++;

				updateTimers();
				// Save the Timers periodically in-case of a server crash
				if (t % 60 == 0) {
					saveTimers();
				}

			}

		};
		mTimerRunnable.runTaskTimer(plugin, 0, 20);
	}

	public TimerManager(Plugin plugin) {
		mPlugin = plugin;
	}

	public QuadTree<TimerTreeNode> getQuadTree(World world) {
		return mQuadTrees.get(world.getUID());
	}

	public void updateTimers() {
		long currentTime = System.currentTimeMillis();
		for (Timer timer : mTimers) {
			timer.updateTimer(currentTime);
		}
	}

	public void saveTimers() {
		for (Timer timer : mTimers) {
			timer.saveTimerData();
		}
	}

	public Timer getTimer(String id) {
		for (Timer timer : mTimers) {
			if (timer.mId.equalsIgnoreCase(id)) {
				return timer;
			}
		}
		return null;
	}

}
