package com.playmonumenta.scriptedquests.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;

import com.playmonumenta.scriptedquests.Plugin;

public class InteractNpc implements CommandExecutor {
	Plugin mPlugin;

	public InteractNpc(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		Player player;

		// Check argument count
		if (arg3.length != 1 && arg3.length != 2) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		// Parse NPC name
		String npcName = arg3[0];

		// Parse optional NPC EntityType
		EntityType npcType;
		if (arg3.length == 2) {
			try {
				npcType = EntityType.valueOf(arg3[1]);
			} catch (IllegalArgumentException e) {
				sender.sendMessage(ChatColor.RED +
				                   "Invalid EntityType! Must exactly match one of Spigot's EntityType values.");
				return false;
			}
		} else {
			npcType = EntityType.VILLAGER;
		}

		// Figure out what player was the target of the command
		if (sender instanceof Player) {
			player = (Player)sender;
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Player) {
				player = (Player)callee;
			} else {
				sender.sendMessage(ChatColor.RED + "Execute command detected with non-player target!");
				return false;
			}
		} else {
			sender.sendMessage(ChatColor.RED + "This command must be run by/on a player!");
			return false;
		}

		if (!mPlugin.mNpcManager.interactEvent(mPlugin, player, npcName, npcType)) {
			sender.sendMessage(ChatColor.RED + "No interaction available for player '" + player.getName() +
			                   "' and NPC '" + npcName + "'");
			return false;
		}

		return true;
	}
}
