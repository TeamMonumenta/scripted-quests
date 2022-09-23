package com.playmonumenta.scriptedquests;

import com.playmonumenta.scriptedquests.api.ClientChatProtocol;
import com.playmonumenta.scriptedquests.commands.ChangeLogLevel;
import com.playmonumenta.scriptedquests.commands.Clickable;
import com.playmonumenta.scriptedquests.commands.Clock;
import com.playmonumenta.scriptedquests.commands.Code;
import com.playmonumenta.scriptedquests.commands.Cooldown;
import com.playmonumenta.scriptedquests.commands.Damage;
import com.playmonumenta.scriptedquests.commands.DebugZones;
import com.playmonumenta.scriptedquests.commands.GenerateCode;
import com.playmonumenta.scriptedquests.commands.GetDate;
import com.playmonumenta.scriptedquests.commands.GiveLootTable;
import com.playmonumenta.scriptedquests.commands.Growable;
import com.playmonumenta.scriptedquests.commands.GuiCommand;
import com.playmonumenta.scriptedquests.commands.HasPermission;
import com.playmonumenta.scriptedquests.commands.Heal;
import com.playmonumenta.scriptedquests.commands.ImprovedClear;
import com.playmonumenta.scriptedquests.commands.InteractNpc;
import com.playmonumenta.scriptedquests.commands.Leaderboard;
import com.playmonumenta.scriptedquests.commands.Line;
import com.playmonumenta.scriptedquests.commands.QuestTrigger;
import com.playmonumenta.scriptedquests.commands.RaceCommand;
import com.playmonumenta.scriptedquests.commands.RandomNumber;
import com.playmonumenta.scriptedquests.commands.RandomSample;
import com.playmonumenta.scriptedquests.commands.ReloadQuests;
import com.playmonumenta.scriptedquests.commands.ReloadZones;
import com.playmonumenta.scriptedquests.commands.ScheduleFunction;
import com.playmonumenta.scriptedquests.commands.SetVelocity;
import com.playmonumenta.scriptedquests.commands.ShowZones;
import com.playmonumenta.scriptedquests.commands.TestZone;
import com.playmonumenta.scriptedquests.commands.TimerDebug;
import com.playmonumenta.scriptedquests.commands.Waypoint;
import com.playmonumenta.scriptedquests.listeners.EntityListener;
import com.playmonumenta.scriptedquests.listeners.InteractablesListener;
import com.playmonumenta.scriptedquests.listeners.PlayerListener;
import com.playmonumenta.scriptedquests.listeners.WorldListener;
import com.playmonumenta.scriptedquests.listeners.ZoneEventListener;
import com.playmonumenta.scriptedquests.managers.ClickableManager;
import com.playmonumenta.scriptedquests.managers.CodeManager;
import com.playmonumenta.scriptedquests.managers.GrowableManager;
import com.playmonumenta.scriptedquests.managers.GuiManager;
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
import com.playmonumenta.scriptedquests.protocollib.ProtocolLibIntegration;
import com.playmonumenta.scriptedquests.timers.CommandTimerManager;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import java.io.File;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Plugin extends JavaPlugin {
	private static @Nullable Plugin INSTANCE = null;

	private FileConfiguration mConfig;
	private File mConfigFile;
	public @Nullable Boolean mShowTimerNames = null;
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
	private TranslationsManager mTranslationsManager;
	public CodeManager mCodeManager;
	public ZoneManager mZoneManager;
	public ZonePropertyManager mZonePropertyManager;
	public WaypointManager mWaypointManager;
	public GrowableManager mGrowableManager;
	public GuiManager mGuiManager;
	public ZoneEventListener mZoneEventListener;
	public @Nullable ProtocolLibIntegration mProtocolLibIntegration;

	public Random mRandom = new Random();
	private ScheduleFunction mScheduledFunctionsManager;
	private @Nullable CustomLogger mLogger = null;

	@Override
	public void onLoad() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}

		NmsUtils.loadVersionAdapter(this.getServer().getClass(), getLogger());

		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */

		reloadConfigYaml(null);

		if (mConfig.contains("translations")) {
			mTranslationsManager = new TranslationsManager(this, mConfig.getConfigurationSection("translations"));
		}

		ChangeLogLevel.register();
		InteractNpc.register(this);
		Clickable.register(this);
		GiveLootTable.register(mRandom);
		RaceCommand.register(this);
		Leaderboard.register(this);
		Line.register();
		RandomNumber.register();
		RandomSample.register();
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
		ReloadZones.register(this);
		GuiCommand.register(this);
		ShowZones.register(this);

		mScheduledFunctionsManager = new ScheduleFunction(this);
		mGrowableManager = new GrowableManager(this);

		Growable.register(mGrowableManager);
		Waypoint.register(this);
	}

	@Override
	public void onEnable() {
		INSTANCE = this;

		PluginManager manager = getServer().getPluginManager();

		mQuestCompassManager = new QuestCompassManager(this);
		mNpcManager = new QuestNpcManager(this);
		mClickableManager = new ClickableManager();
		mInteractableManager = new InteractableManager();
		mTradeManager = new NpcTradeManager();
		mLoginManager = new QuestLoginManager();
		mDeathManager = new QuestDeathManager();
		mRaceManager = new RaceManager(this);
		mCodeManager = new CodeManager();
		mZoneEventListener = new ZoneEventListener(this);
		mZoneManager = new ZoneManager(this);
		mZoneManager.doReload(this);
		mZonePropertyManager = new ZonePropertyManager(this);
		mTimerManager = new CommandTimerManager(this);
		mWaypointManager = new WaypointManager(this);
		mGuiManager = new GuiManager(this);

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
		manager.registerEvents(mZoneEventListener, this);

		// Hook into ProtocolLib if present
		if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
			mProtocolLibIntegration = new ProtocolLibIntegration(this);
		}

		getCommand("reloadQuests").setExecutor(new ReloadQuests(this));
		getCommand("questTrigger").setExecutor(new QuestTrigger(this));

		ClientChatProtocol.initialize(this);

		/* Load the config 1 tick later to let other plugins load */
		new BukkitRunnable() {
			@Override
			public void run() {
				reloadConfig(null);
				mZoneManager.reload(INSTANCE, Bukkit.getConsoleSender());
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
		ClientChatProtocol.getInstance().deinitialize();
		mScheduledFunctionsManager.cancel();
		mScheduledFunctionsManager = null;
	}

	/* Sender will be sent debugging info if non-null */
	public void reloadConfig(@Nullable CommandSender sender) {
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
		mZoneEventListener.update();
		mGrowableManager.reload(this, sender);
		mGuiManager.reload(this, sender);
		if (mProtocolLibIntegration != null) {
			mProtocolLibIntegration.reload();
		}
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
	}

	@SuppressWarnings("NullAway") // Never returns null unless the server is horribly broken anyway
	public static Plugin getInstance() {
		return INSTANCE;
	}

	@Override
	public Logger getLogger() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}
		return mLogger;
	}
}
