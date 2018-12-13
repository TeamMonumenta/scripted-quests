package com.playmonumenta.scriptedquests.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.playmonumenta.scriptedquests.Plugin;

public class ReloadQuests implements CommandExecutor {
	Plugin mPlugin;

	public ReloadQuests(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 0) {
			sender.sendMessage(ChatColor.RED + "No parameters are needed for this function!");
			return false;
		}

		sender.sendMessage(ChatColor.GOLD + "Reloading config...");
		mPlugin.mNpcManager.reload(mPlugin, sender);
		mPlugin.mQuestCompassManager.reload(mPlugin, sender);
		mPlugin.mDeathManager.reload(mPlugin, sender);
		mPlugin.mRaceManager.reload(mPlugin, sender);

		return true;
	}
}
