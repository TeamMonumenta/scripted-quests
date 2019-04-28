package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class Clickable {
	public static void register(Plugin plugin) {
		/* First one of these has both required arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument());

		CommandAPI.getInstance().register("clickable",
		                                  CommandPermission.fromString("scriptedquests.clickable"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      return click(plugin, sender, (String)args[0]);
		                                  }
		);
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
