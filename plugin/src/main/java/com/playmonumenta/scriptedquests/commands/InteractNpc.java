package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.EntityTypeArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class InteractNpc {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		/* First one of these has both required arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("npcName", new StringArgument());
		arguments.put("npcType", new EntityTypeArgument());

		CommandAPI.getInstance().register("interactnpc",
		                                  CommandPermission.fromString("scriptedquests.interactnpc"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      interact(plugin, sender, (Collection<Player>)args[0],
		                                               (String)args[1], (EntityType)args[2]);
		                                  }
		);

		/* Second one just has the npc name with VILLAGER as default */
		arguments = new LinkedHashMap<>();

		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("npcName", new StringArgument());

		CommandAPI.getInstance().register("interactnpc",
		                                  CommandPermission.fromString("scriptedquests.interactnpc"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      interact(plugin, sender, (Collection<Player>)args[0],
		                                               (String)args[1], EntityType.VILLAGER);
		                                  }
		);
	}

	private static void interact(Plugin plugin, CommandSender sender, Collection<Player>players,
	                             String npcName, EntityType npcType) {
		if (plugin.mNpcManager != null) {
			for (Player player : players) {
				if (!plugin.mNpcManager.interactEvent(plugin, player, npcName, npcType, null, true)) {
					sender.sendMessage(ChatColor.RED + "No interaction available for player '" + player.getName() +
									   "' and NPC '" + npcName + "'");
				}
			}
		}
	}
}
