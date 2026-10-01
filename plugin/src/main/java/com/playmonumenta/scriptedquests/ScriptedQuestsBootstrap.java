package com.playmonumenta.scriptedquests;

import com.playmonumenta.scriptedquests.commands.*;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.Random;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ScriptedQuestsBootstrap implements PluginBootstrap {
	private @MonotonicNonNull ScheduleFunction mScheduledFunctionsManager;

	@Override
	public void bootstrap(BootstrapContext context) {
		FontUtilsDebug.register();
		InteractNpc.register();
		Clickable.register();
		GiveLootTable.register(new Random());
		RaceCommand.register();
		Leaderboard.register();
		Line.register();
		RandomNumber.register();
		RandomSample.register();
		HasPermission.register();
		TimerDebug.register();
		GenerateCode.register();
		Code.register();
		SetVelocity.register();
		Heal.register();
		Damage.register();
		Cooldown.register();
		Clock.register();
		ImprovedClear.register();
		ReloadZones.register();
		GuiCommand.register();
		TradesCommand.register();
		Music.register();
		InvalidateCompassCacheCommand.register();

		mScheduledFunctionsManager = new ScheduleFunction();

		GrowableAPI.registerCommands();
		Waypoint.register();
		context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
			event.registrar().register("questtrigger", new QuestTrigger());
			event.registrar().register("reloadquests", new ReloadQuests());
		});
	}

	@Override
	public JavaPlugin createPlugin(PluginProviderContext context) {
		return new Plugin(mScheduledFunctionsManager);
	}
}
