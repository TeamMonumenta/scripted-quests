package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DebugZones {
	public static void register() {
		Argument<Player> playerArg = new EntitySelectorArgument.OnePlayer("player");

		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				ZoneManager.getInstance().sendDebug(sender, args.getByArgument(playerArg));
			})
			.register();

		Argument<Location> positionArg = new LocationArgument("position");

		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(positionArg)
			.executes((sender, args) -> {
				ZoneManager.getInstance().sendDebug(sender, args.getByArgument(positionArg));
			})
			.register();
	}
}
