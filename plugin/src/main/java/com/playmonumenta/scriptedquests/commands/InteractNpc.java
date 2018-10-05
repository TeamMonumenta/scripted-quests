package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntityTypeArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.CommandAPI;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class InteractNpc {
	public static void register(Plugin plugin) {
		/* First one of these has both required arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("npcName", new StringArgument());
		arguments.put("npcType", new EntityTypeArgument());

		CommandAPI.getInstance().register("interactnpc",
		                                  new CommandPermission("scriptedquests.interactnpc"),
		                                  arguments,
										  (sender, args) -> {
			                                  interact(plugin, sender, (String)args[0], (EntityType)args[1]);
		                                  }
		);

		/* Second one just has the npc name with VILLAGER as default */
		arguments = new LinkedHashMap<>();

		arguments.put("npcName", new StringArgument());

		CommandAPI.getInstance().register("interactnpc",
		                                  new CommandPermission("scriptedquests.interactnpc"),
		                                  arguments,
										  (sender, args) -> {
			                                  interact(plugin, sender, (String)args[0], EntityType.VILLAGER);
		                                  }
		);
	}

	private static void interact(Plugin plugin, CommandSender sender, String npcName, EntityType npcType) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (!plugin.mNpcManager.interactEvent(plugin, player, npcName, npcType, true)) {
				sender.sendMessage(ChatColor.RED + "No interaction available for player '" + player.getName() +
				                   "' and NPC '" + npcName + "'");
			}
		} else {
			sender.sendMessage("This command must be run by/on a player!");
		}
	}
}
