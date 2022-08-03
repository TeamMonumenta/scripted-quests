package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

public class InteractNpc {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		/* First one of these has both required arguments */

		new CommandAPICommand("interactnpc")
			.withPermission(CommandPermission.fromString("scriptedquests.interactnpc"))
			.withArguments(
				new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS),
				new StringArgument("npcName"),
				new EntityTypeArgument("npcType")
			)
			.executes((sender, args) -> {
				interact(plugin, sender, (Collection<Player>)args[0],
					(String)args[1], (EntityType)args[2]);
			})
			.register();

		/* Second one just has the npc name with VILLAGER as default */

		new CommandAPICommand("interactnpc")
			.withPermission(CommandPermission.fromString("scriptedquests.interactnpc"))
			.withArguments(
				new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS),
				new StringArgument("npcName")
			)
			.executes((sender, args) -> {
				interact(plugin, sender, (Collection<Player>)args[0],
					(String)args[1], EntityType.VILLAGER);
			})
			.register();

	}

	private static void interact(Plugin plugin, CommandSender sender, Collection<Player> players,
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
