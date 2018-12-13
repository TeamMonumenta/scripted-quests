package com.playmonumenta.scriptedquests;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.scriptedquests.commands.GiveLootTable;
import com.playmonumenta.scriptedquests.commands.HasPermission;
import com.playmonumenta.scriptedquests.commands.InteractNpc;
import com.playmonumenta.scriptedquests.commands.Leaderboard;
import com.playmonumenta.scriptedquests.commands.QuestTrigger;
import com.playmonumenta.scriptedquests.commands.Race;
import com.playmonumenta.scriptedquests.commands.RandomNumber;
import com.playmonumenta.scriptedquests.commands.ReloadQuests;
import com.playmonumenta.scriptedquests.commands.ScheduleFunction;
import com.playmonumenta.scriptedquests.listeners.EntityListener;
import com.playmonumenta.scriptedquests.listeners.PlayerListener;
import com.playmonumenta.scriptedquests.managers.QuestCompassManager;
import com.playmonumenta.scriptedquests.managers.QuestDeathManager;
import com.playmonumenta.scriptedquests.managers.QuestNpcManager;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

public class Plugin extends JavaPlugin {
	public QuestCompassManager mQuestCompassManager;
	public QuestNpcManager mNpcManager;
	public QuestDeathManager mDeathManager;
	public RaceManager mRaceManager;

	public World mWorld;
	public Random mRandom = new Random();
	private ScheduleFunction mScheduledFunctionsManager;

	@Override
	public void onLoad() {
		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */

		InteractNpc.register(this);
		GiveLootTable.register(mRandom);
		Race.register(this);
		Leaderboard.register();
		RandomNumber.register();
		HasPermission.register();

		mScheduledFunctionsManager = new ScheduleFunction(this);
	}

	@Override
	public void onEnable() {
		PluginManager manager = getServer().getPluginManager();

		mWorld = Bukkit.getWorlds().get(0);

		manager.registerEvents(new EntityListener(this), this);
		manager.registerEvents(new PlayerListener(this), this);

		mQuestCompassManager = new QuestCompassManager(this);
		mNpcManager = new QuestNpcManager(this);
		mDeathManager = new QuestDeathManager(this);
		mRaceManager = new RaceManager(this);

		getCommand("reloadQuests").setExecutor(new ReloadQuests(this));
		getCommand("questTrigger").setExecutor(new QuestTrigger(this));
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);

		mRaceManager.cancelAllRaces();

		MetadataUtils.removeAllMetadata(this);

		// Run all pending delayed commands
		mScheduledFunctionsManager.cancel();
		mScheduledFunctionsManager = null;
	}
}
