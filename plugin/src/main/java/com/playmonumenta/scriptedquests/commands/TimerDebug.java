package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.playmonumenta.scriptedquests.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;

public class TimerDebug {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("enabledOnly", new BooleanArgument());

		CommandAPI.getInstance().register("timerdebug",
		                                  CommandPermission.fromString("scriptedquests.timerdebug"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      debug(plugin, sender, (Boolean)args[0]);
		                                  }
		);
	}

	private static void debug(Plugin plugin, CommandSender sender, boolean enabledOnly) {
		if (plugin.mTimerManager != null) {
			sender.sendMessage("");
			if (enabledOnly) {
				sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Currently active timers:");
			} else {
				sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "All timers:");
			}
			plugin.mTimerManager.tellTimers(sender, enabledOnly);
		}
	}
}

