package com.playmonumenta.scriptedquests;

import com.playmonumenta.scriptedquests.api.ClientChatProtocol;
import com.playmonumenta.scriptedquests.commands.*;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import com.playmonumenta.scriptedquests.listeners.EntityListener;
import com.playmonumenta.scriptedquests.listeners.InteractablesListener;
import com.playmonumenta.scriptedquests.listeners.PlayerListener;
import com.playmonumenta.scriptedquests.listeners.RedisSyncListener;
import com.playmonumenta.scriptedquests.listeners.WorldListener;
import com.playmonumenta.scriptedquests.listeners.ZoneEventListener;
import com.playmonumenta.scriptedquests.managers.ClickableManager;
import com.playmonumenta.scriptedquests.managers.CodeManager;
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
import com.playmonumenta.scriptedquests.zones.ZonePropertyGroupManager;
import java.io.File;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
	public ZonePropertyGroupManager mZonePropertyGroupManager;
	public WaypointManager mWaypointManager;
	public GuiManager mGuiManager;
	public ZoneEventListener mZoneEventListener;
	public @Nullable ProtocolLibIntegration mProtocolLibIntegration;

	public Random mRandom = new Random();
	private ScheduleFunction mScheduledFunctionsManager;
	private @Nullable CustomLogger mLogger = null;
	private SoundCategory mDefaultMusicSoundCategory = SoundCategory.RECORDS;

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

		ConfigurationSection translationsConfig = mConfig.getConfigurationSection("translations");
		if (translationsConfig != null) {
			mTranslationsManager = new TranslationsManager(this, translationsConfig);
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
		TestZone.register();
		Heal.register();
		Damage.register();
		Cooldown.register();
		Clock.register();
		ImprovedClear.register();
		ReloadZones.register(this);
		GuiCommand.register(this);
		ShowZones.register(this);
		Music.register();

		mScheduledFunctionsManager = new ScheduleFunction(this);

		GrowableAPI.registerCommands();
		Waypoint.register(this);
	}

	@Override
	public void onEnable() {
		INSTANCE = this;

		PluginManager manager = getServer().getPluginManager();

		mQuestCompassManager = new QuestCompassManager(this);
		mNpcManager = new QuestNpcManager();
		mClickableManager = new ClickableManager();
		mInteractableManager = new InteractableManager();
		mTradeManager = new NpcTradeManager();
		mLoginManager = new QuestLoginManager();
		mDeathManager = new QuestDeathManager();
		mRaceManager = new RaceManager(this);
		mCodeManager = new CodeManager();
		mZoneEventListener = new ZoneEventListener(this);
		mZoneManager = ZoneManager.createInstance(this);
		mZonePropertyManager = new ZonePropertyManager(this);
		mZonePropertyGroupManager = new ZonePropertyGroupManager();
		mTimerManager = new CommandTimerManager(this);
		mWaypointManager = new WaypointManager(this);
		mGuiManager = new GuiManager(this);

		manager.registerEvents(new EntityListener(this), this);
		manager.registerEvents(new InteractablesListener(this), this);
		manager.registerEvents(new PlayerListener(this), this);
		if (manager.isPluginEnabled("MonumentaRedisSync")) {
			manager.registerEvents(new RedisSyncListener(this), this);
		}
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

		Objects.requireNonNull(getCommand("reloadQuests")).setExecutor(new ReloadQuests(this));
		Objects.requireNonNull(getCommand("questTrigger")).setExecutor(new QuestTrigger(this));

		ClientChatProtocol.initialize(this);
		mZoneManager.doReload(this, true);

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
		getServer().getScheduler().cancelTasks(this);

		mRaceManager.cancelAllRaces();
		mTimerManager.unloadAll();
		mWaypointManager.cancelAll();

		MetadataUtils.removeAllMetadata(this);

		// Run all pending delayed commands
		ClientChatProtocol.getInstance().deinitialize();
		mScheduledFunctionsManager.cancel();
		mScheduledFunctionsManager = null;

		INSTANCE = null;
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
		GrowableAPI.reload(sender);
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
				sender.sendMessage("show_timer_names: " + mShowTimerNames);
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
			sender.sendMessage("show_zones_dynmap: " + mShowZonesDynmap);
		}

		if (mConfig.isBoolean("fallback_zone_lookup")) {
			mFallbackZoneLookup = mConfig.getBoolean("fallback_zone_lookup", false);
		} else {
			mFallbackZoneLookup = false;
		}
		if (sender != null) {
			sender.sendMessage("fallback_zone_lookup: " + mFallbackZoneLookup);
		}

		String defaultMusicCategoryStr = mConfig.getString("default_music_category");
		if (defaultMusicCategoryStr != null) {
			SoundCategory defaultMusicCategory = Constants.SOUND_CATEGORY_BY_NAME.get(defaultMusicCategoryStr);
			if (defaultMusicCategory == null) {
				if (sender != null) {
					sender.sendMessage("default_music_category is invalid category: " + defaultMusicCategoryStr);
				}
				mDefaultMusicSoundCategory = SoundCategory.RECORDS;
			} else {
				mDefaultMusicSoundCategory = defaultMusicCategory;
			}
		}
		if (sender != null) {
			sender.sendMessage("default_music_category: "
				+ Constants.SOUND_CATEGORY_NAMES.get(mDefaultMusicSoundCategory));
		}
	}

	public static Plugin getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("Attempted to access ScriptedQuests plugin before it loaded.");
		}
		return INSTANCE;
	}

	@Override
	public Logger getLogger() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}
		return mLogger;
	}

	public SoundCategory getDefaultMusicSoundCategory() {
		return mDefaultMusicSoundCategory;
	}
}
