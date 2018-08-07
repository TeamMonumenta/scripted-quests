package com.playmonumenta.scriptedquests.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class GiveLootTable implements CommandExecutor {
	Plugin mPlugin;

	public GiveLootTable(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		// Check argument count
		if (arg3.length != 1) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		String lootPath = arg3[0];
		if (!lootPath.contains(":")) {
			sender.sendMessage(ChatColor.RED + "Loot table path should be of the form prefix:path/to/loot");
			sender.sendMessage(ChatColor.RED + "For example: 'minecraft:entities/creeper'");
			return false;
		}

		Player player = null;
		if (sender instanceof Player) {
			player = (Player)sender;
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Player) {
				player = (Player)callee;
			}
		}

		if (player == null) {
            sender.sendMessage(ChatColor.RED + "This command must be run by/on a player!");
			return false;
		}

		try {
			InventoryUtils.giveLootTableContents(player, lootPath);
		} catch (Exception e) {
			player.sendMessage(ChatColor.RED + "BUG! Server failed to give you loot from the table '" + lootPath + "'");
			player.sendMessage(ChatColor.RED + "Please hover over the following message, take a screenshot, and report this to a moderator");
			MessagingUtils.sendStackTrace(player, e);
		}

		return true;
	}
}
