package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public class TimerDebug {
	public static void register(Plugin plugin) {
		BooleanArgument enabledOnlyArg = new BooleanArgument("enabledOnly");

		new CommandAPICommand("timerdebug")
			.withPermission(CommandPermission.fromString("scriptedquests.timerdebug"))
			.withArguments(enabledOnlyArg)
			.executes((sender, args) -> {
				debug(plugin, sender, args.getByArgument(enabledOnlyArg));
			})
			.register();
	}

	private static void debug(Plugin plugin, CommandSender sender, boolean enabledOnly) {
		if (plugin.mTimerManager != null) {
			sender.sendMessage("");
			if (enabledOnly) {
				sender.sendMessage(Component.text("Currently active timers:", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
			} else {
				sender.sendMessage(Component.text("All timers:", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
			}
			plugin.mTimerManager.tellTimers(sender, enabledOnly);
		}
	}
}

