package com.playmonumenta.scriptedquests;

import java.io.File;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
import com.playmonumenta.scriptedquests.commands.TimerDebug;
import com.playmonumenta.scriptedquests.listeners.EntityListener;
import com.playmonumenta.scriptedquests.listeners.PlayerListener;
import com.playmonumenta.scriptedquests.managers.NpcTradeManager;
import com.playmonumenta.scriptedquests.managers.QuestCompassManager;
import com.playmonumenta.scriptedquests.managers.QuestDeathManager;
import com.playmonumenta.scriptedquests.managers.QuestNpcManager;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.timers.CommandTimerManager;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

public class Plugin extends JavaPlugin {
	private FileConfiguration mConfig;
	private File mConfigFile;
	public Boolean mShowTimerNames = null;

	public QuestCompassManager mQuestCompassManager;
	public QuestNpcManager mNpcManager;
	public QuestDeathManager mDeathManager;
	public RaceManager mRaceManager;
	public NpcTradeManager mTradeManager;
	public CommandTimerManager mTimerManager;

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
		TimerDebug.register(this);

		mScheduledFunctionsManager = new ScheduleFunction(this);
	}

	@Override
	public void onEnable() {
		reloadConfigYaml(null);

		PluginManager manager = getServer().getPluginManager();

		mWorld = Bukkit.getWorlds().get(0);

		mQuestCompassManager = new QuestCompassManager(this);
		mNpcManager = new QuestNpcManager(this);
		mTradeManager = new NpcTradeManager(this);
		mDeathManager = new QuestDeathManager(this);
		mRaceManager = new RaceManager(this);
		mTimerManager = new CommandTimerManager(this);

		manager.registerEvents(new EntityListener(this), this);
		manager.registerEvents(new PlayerListener(this), this);
		manager.registerEvents(mTimerManager, this);

		getCommand("reloadQuests").setExecutor(new ReloadQuests(this));
		getCommand("questTrigger").setExecutor(new QuestTrigger(this));
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);

		mRaceManager.cancelAllRaces();
		mTimerManager.unloadAll();

		MetadataUtils.removeAllMetadata(this);

		// Run all pending delayed commands
		mScheduledFunctionsManager.cancel();
		mScheduledFunctionsManager = null;
	}

	/* Sender will be sent debugging info if non-null */
	public void reloadConfig(CommandSender sender) {
		reloadConfigYaml(sender);
		mNpcManager.reload(this, sender);
		mTradeManager.reload(this, sender);
		mQuestCompassManager.reload(this, sender);
		mDeathManager.reload(this, sender);
		mRaceManager.reload(this, sender);
	}

	private void reloadConfigYaml(CommandSender sender) {
		if (mConfigFile == null) {
			mConfigFile = new File(getDataFolder(), "config.yml");
		}

		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);

		if (mConfig.isBoolean("show_timer_names")) {
			mShowTimerNames = mConfig.getBoolean("show_timer_names", false);
			if (sender != null) {
				sender.sendMessage("show_timer_names: " + mShowTimerNames.toString());
			}
		} else {
			mShowTimerNames = null;
			if (sender != null) {
				sender.sendMessage("show_timer_names: null / not automatically changed");
			}
		}
	}
}
