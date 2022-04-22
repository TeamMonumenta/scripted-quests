package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
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

		//Sets command waypoint
		new CommandAPICommand("waypoint")
			.withPermission(perm)
			.withSubcommand(new CommandAPICommand("set")
				.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
				.withArguments(new TextArgument("title"))
				.withArguments(new TextArgument("label"))
				.withArguments(new LocationArgument("location", LocationType.PRECISE_POSITION))
				.executes((sender, args) -> {
					if (plugin.mQuestCompassManager != null) {
						List<Location> waypoint = new ArrayList<>();
						waypoint.add((Location)args[3]);
						plugin.mQuestCompassManager.setCommandWaypoint((Player)args[0], waypoint, (String)args[1] + ChatColor.RESET, (String)args[2]);
					} else {
						CommandAPI.fail("Quest Compass Manager does not exist!");
					}
				}))
			.withSubcommand(new CommandAPICommand("remove")
				.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
				.executes((sender, args) -> {
					if (plugin.mQuestCompassManager != null) {
						plugin.mQuestCompassManager.removeCommandWaypoint((Player)args[0]);
					} else {
						CommandAPI.fail("Quest Compass Manager does not exist!");
					}
				}))
			.register();
	}
}
