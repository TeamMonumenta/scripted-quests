package com.playmonumenta.scriptedquests.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.playmonumenta.scriptedquests.Plugin;

public class ReloadZones implements CommandExecutor {
	Plugin mPlugin;

	public ReloadZones(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 0) {
			sender.sendMessage(ChatColor.RED + "No parameters are needed for this function!");
			return false;
		}

		sender.sendMessage(ChatColor.GOLD + "Reloading config...");

		mPlugin.reloadZones(sender);

		return true;
	}
}
