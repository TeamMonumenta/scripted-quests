package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class TimerDebug {
	public static void register(Plugin plugin) {
		new CommandAPICommand("timerdebug")
			.withPermission(CommandPermission.fromString("scriptedquests.timerdebug"))
			.withArguments(new BooleanArgument("enabledOnly"))
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

