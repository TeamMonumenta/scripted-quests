package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadQuests implements CommandExecutor {
	Plugin mPlugin;

	public ReloadQuests(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 0) {
			sender.sendMessage(Component.text("No parameters are needed for this function!", NamedTextColor.RED));
			return false;
		}

		sender.sendMessage(Component.text("Reloading config...", NamedTextColor.GOLD));

		mPlugin.reloadConfig(sender);

		return true;
	}
}
