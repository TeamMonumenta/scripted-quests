package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;

public class TimerDebug {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("enabledOnly", new BooleanArgument());

		new CommandAPICommand("timerdebug")
			.withPermission(CommandPermission.fromString("scriptedquests.timerdebug"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				debug(plugin, sender, (Boolean)args[0]);
			})
			.register();
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

