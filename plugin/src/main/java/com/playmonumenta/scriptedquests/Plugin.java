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
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.scriptedquests.commands.Clickable;
import com.playmonumenta.scriptedquests.commands.Clock;
import com.playmonumenta.scriptedquests.commands.Code;
import com.playmonumenta.scriptedquests.commands.Cooldown;
import com.playmonumenta.scriptedquests.commands.Damage;
import com.playmonumenta.scriptedquests.commands.DebugZones;
import com.playmonumenta.scriptedquests.commands.GenerateCode;
import com.playmonumenta.scriptedquests.commands.GetDate;
import com.playmonumenta.scriptedquests.commands.GiveItemWithLore;
import com.playmonumenta.scriptedquests.commands.GiveLootTable;
import com.playmonumenta.scriptedquests.commands.Growable;
import com.playmonumenta.scriptedquests.commands.HasPermission;
import com.playmonumenta.scriptedquests.commands.Heal;
import com.playmonumenta.scriptedquests.commands.ImprovedClear;
import com.playmonumenta.scriptedquests.commands.InteractNpc;
import com.playmonumenta.scriptedquests.commands.Leaderboard;
import com.playmonumenta.scriptedquests.commands.Line;
import com.playmonumenta.scriptedquests.commands.QuestTrigger;
import com.playmonumenta.scriptedquests.commands.Race;
import com.playmonumenta.scriptedquests.commands.RandomNumber;
import com.playmonumenta.scriptedquests.commands.ReloadQuests;
import com.playmonumenta.scriptedquests.commands.ReloadZones;
import com.playmonumenta.scriptedquests.commands.ScheduleFunction;
import com.playmonumenta.scriptedquests.commands.SetVelocity;
import com.playmonumenta.scriptedquests.commands.TestZone;
import com.playmonumenta.scriptedquests.commands.TimerDebug;
import com.playmonumenta.scriptedquests.commands.Waypoint;
import com.playmonumenta.scriptedquests.listeners.EntityListener;
import com.playmonumenta.scriptedquests.listeners.InteractablesListener;
import com.playmonumenta.scriptedquests.listeners.PlayerListener;
import com.playmonumenta.scriptedquests.listeners.WorldListener;
import com.playmonumenta.scriptedquests.managers.ClickableManager;
import com.playmonumenta.scriptedquests.managers.CodeManager;
import com.playmonumenta.scriptedquests.managers.GrowableManager;
import com.playmonumenta.scriptedquests.managers.InteractableManager;
import com.playmonumenta.scriptedquests.managers.NpcTradeManager;
import com.playmonumenta.scriptedquests.managers.QuestCompassManager;
import com.playmonumenta.scriptedquests.managers.QuestDeathManager;
import com.playmonumenta.scriptedquests.managers.QuestLoginManager;
import com.playmonumenta.scriptedquests.managers.QuestNpcManager;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import com.playmonumenta.scriptedquests.managers.WaypointManager;
import com.playmonumenta.scriptedquests.managers.ZonePropertyManager;
import com.playmonumenta.scriptedquests.timers.CommandTimerManager;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import com.playmonumenta.scriptedquests.zones.ZoneManager;

public class Plugin extends JavaPlugin {
	private static Plugin INSTANCE = null;

	private FileConfiguration mConfig;
	private File mConfigFile;
	public Boolean mShowTimerNames = null;
	public boolean mShowZonesDynmap = false;
	public boolean mFallbackZoneLookup = false;

	public QuestCompassManager mQuestCompassManager;
	public QuestNpcManager mNpcManager;
	public ClickableManager mClickableManager;
	public InteractableManager mInteractableManager;
	public QuestLoginManager mLoginManager;
	public QuestDeathManager mDeathManager;
	public RaceManager mRaceManager;
	public NpcTradeManager mTradeManager;
	public CommandTimerManager mTimerManager;
	private TranslationsManager mTranslationsManager = null;
	public CodeManager mCodeManager;
	public ZoneManager mZoneManager;
	public ZonePropertyManager mZonePropertyManager;
	public WaypointManager mWaypointManager;
	public GrowableManager mGrowableManager;

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

		reloadConfigYaml(null);

		InteractNpc.register(this);
		Clickable.register(this);
		GiveLootTable.register(mRandom);
		GiveItemWithLore.register();
		Race.register(this);
		Leaderboard.register(this);
		Line.register();
		RandomNumber.register();
		HasPermission.register();
		TimerDebug.register(this);
		GenerateCode.register(this);
		GetDate.register();
		Code.register(this);
		SetVelocity.register();
		DebugZones.register(this);
		TestZone.register(this);
		Heal.register();
		Damage.register();
		Cooldown.register();
		Clock.register();
		ImprovedClear.register();

		mScheduledFunctionsManager = new ScheduleFunction(this);
		mGrowableManager = new GrowableManager(this);

		Growable.register(mGrowableManager);
		Waypoint.register(this);
	}

	@Override
	public void onEnable() {
		INSTANCE = this;

		PluginManager manager = getServer().getPluginManager();

		mWorld = Bukkit.getWorlds().get(0);

		mQuestCompassManager = new QuestCompassManager(this);
		mNpcManager = new QuestNpcManager(this);
		mClickableManager = new ClickableManager();
		mInteractableManager = new InteractableManager();
		mTradeManager = new NpcTradeManager();
		mLoginManager = new QuestLoginManager();
		mDeathManager = new QuestDeathManager();
		mRaceManager = new RaceManager(this);
		mCodeManager = new CodeManager();
		mZoneManager = new ZoneManager(this);
		mZonePropertyManager = new ZonePropertyManager(this);
		mTimerManager = new CommandTimerManager(this);
		mWaypointManager = new WaypointManager(this);

		manager.registerEvents(new EntityListener(this), this);
		manager.registerEvents(new InteractablesListener(this), this);
		manager.registerEvents(new PlayerListener(this), this);
		manager.registerEvents(new WorldListener(this), this);
		if (mTranslationsManager != null) {
			manager.registerEvents(mTranslationsManager, this);
		}
		manager.registerEvents(mTimerManager, this);
		manager.registerEvents(mZonePropertyManager, this);
		manager.registerEvents(mTradeManager, this);

		getCommand("reloadQuests").setExecutor(new ReloadQuests(this));
		getCommand("reloadZones").setExecutor(new ReloadZones(this));
		getCommand("questTrigger").setExecutor(new QuestTrigger(this));

		/* Load the config 1 tick later to let other plugins load */
		new BukkitRunnable() {
			@Override
			public void run() {
				reloadConfig(null);
			}
		}.runTaskLater(this, 1);
	}

	@Override
	public void onDisable() {
		INSTANCE = null;

		getServer().getScheduler().cancelTasks(this);

		mRaceManager.cancelAllRaces();
		mTimerManager.unloadAll();
		mWaypointManager.cancelAll();

		MetadataUtils.removeAllMetadata(this);

		// Run all pending delayed commands
		mScheduledFunctionsManager.cancel();
		mScheduledFunctionsManager = null;
	}

	/* Sender will be sent debugging info if non-null */
	public void reloadConfig(CommandSender sender) {
		reloadConfigYaml(sender);
		mNpcManager.reload(this, sender);
		mClickableManager.reload(this, sender);
		mInteractableManager.reload(this, sender);
		mTradeManager.reload(this, sender);
		mQuestCompassManager.reload(this, sender);
		mLoginManager.reload(this, sender);
		mDeathManager.reload(this, sender);
		mRaceManager.reload(this, sender);
		mCodeManager.reload(this, sender);
		mZonePropertyManager.reload(this, sender);
		mGrowableManager.reload(this, sender);
		TranslationsManager.reload(sender);
	}

	public void reloadZones(CommandSender sender) {
		mZoneManager.reload(this, sender);
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

		if (mConfig.isBoolean("show_zones_dynmap")) {
			mShowZonesDynmap = mConfig.getBoolean("show_zones_dynmap", false);
		} else {
			mShowZonesDynmap = false;
		}
		if (sender != null) {
			sender.sendMessage("show_zones_dynmap: " + Boolean.toString(mShowZonesDynmap));
		}

		if (mConfig.isBoolean("fallback_zone_lookup")) {
			mFallbackZoneLookup = mConfig.getBoolean("fallback_zone_lookup", false);
		} else {
			mFallbackZoneLookup = false;
		}
		if (sender != null) {
			sender.sendMessage("fallback_zone_lookup: " + Boolean.toString(mFallbackZoneLookup));
		}

		if (mConfig.contains("translations")) {
			mTranslationsManager = new TranslationsManager(this, mConfig.getConfigurationSection("translations"));
		}
	}

	public static Plugin getInstance() {
		return INSTANCE;
	}
}
