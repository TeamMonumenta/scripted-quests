package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Waypoint {
	public static void register(Plugin plugin) {
		CommandPermission perm = CommandPermission.fromString("scriptedquests.waypoint");

		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		TextArgument titleArg = new TextArgument("title");
		TextArgument labelArg = new TextArgument("label");
		LocationArgument locationArg = new LocationArgument("location", LocationType.PRECISE_POSITION);

		//Sets command waypoint
		new CommandAPICommand("waypoint")
			.withPermission(perm)
			.withSubcommand(new CommandAPICommand("set")
				.withArguments(playerArg)
				.withArguments(titleArg)
				.withArguments(labelArg)
				.withArguments(locationArg)
				.executes((sender, args) -> {
					Player targetPlayer = args.getByArgument(playerArg);
					if (sender instanceof Player player
						    && player != targetPlayer
						    && !player.hasPermission("scriptedquests.waypoint.others")) {
						throw CommandAPI.failWithString("You do not have permission to run this as another player.");
					}
					if (plugin.mQuestCompassManager != null) {
						List<Location> waypoint = new ArrayList<>();
						waypoint.add(args.getByArgument(locationArg));
						plugin.mQuestCompassManager.setCommandWaypoint(targetPlayer, waypoint, args.getByArgument(titleArg) + ChatColor.RESET, args.getByArgument(labelArg));
					} else {
						throw CommandAPI.failWithString("Quest Compass Manager does not exist!");
					}
				}))
			.withSubcommand(new CommandAPICommand("remove")
				.withArguments(playerArg)
				.executes((sender, args) -> {
					Player targetPlayer = args.getByArgument(playerArg);
					if (sender instanceof Player player
						    && player != targetPlayer
						    && !player.hasPermission("scriptedquests.waypoint.others")) {
						throw CommandAPI.failWithString("You do not have permission to run this as another player.");
					}
					if (plugin.mQuestCompassManager != null) {
						plugin.mQuestCompassManager.removeCommandWaypoint(targetPlayer);
					} else {
						throw CommandAPI.failWithString("Quest Compass Manager does not exist!");
					}
				}))
			.register();
	}
}
