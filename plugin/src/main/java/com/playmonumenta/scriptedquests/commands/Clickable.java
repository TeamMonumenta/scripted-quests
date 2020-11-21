package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.Plugin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;

public class Clickable {
	public static void register(Plugin plugin) {
		/* First one of these has both required arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument());

		new CommandAPICommand("clickable")
			.withPermission(CommandPermission.fromString("scriptedquests.clickable"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				return click(plugin, sender, (String)args[0]);
			})
			.register();
	}

	private static int click(Plugin plugin, CommandSender sender, String label) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by players");
		}
		if (plugin.mClickableManager != null) {
			if (plugin.mClickableManager.clickEvent(plugin, (Player)sender, label)) {
				return 1;
			}
		}
		return 0;
	}
}
