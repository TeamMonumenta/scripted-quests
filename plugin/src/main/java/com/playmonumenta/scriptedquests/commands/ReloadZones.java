package com.playmonumenta.scriptedquests.commands;

import org.bukkit.ChatColor;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

import com.playmonumenta.scriptedquests.Plugin;

public class ReloadZones {
	public static void register(Plugin plugin) {
		new CommandAPICommand("reloadzones")
			.withPermission(CommandPermission.fromString("scriptedquests.reloadzones"))
			.executes((sender, args) -> {
				sender.sendMessage(ChatColor.GOLD + "Reloading config...");
				plugin.reloadZones(sender);
				return 1;
			})
			.register();
	}
}
