package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.zones.ZoneManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DebugZones {
	public static void register() {
		Argument<Player> playerArg = new EntitySelectorArgument.OnePlayer("player");

		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(new LiteralArgument("full"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				ZoneManager.getInstance().sendDebugFull(sender, args.getByArgument(playerArg));
			})
			.register();

		Argument<Location> positionArg = new LocationArgument("position");

		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(new LiteralArgument("full"))
			.withArguments(positionArg)
			.executes((sender, args) -> {
				ZoneManager.getInstance().sendDebugFull(sender, args.getByArgument(positionArg));
			})
			.register();

		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(new LiteralArgument("minimal"))
			.withArguments(positionArg)
			.executes((sender, args) -> {
				ZoneManager.getInstance().sendDebugMinimal(sender, args.getByArgument(positionArg));
			})
			.register();

		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(new LiteralArgument("world"))
			.withArguments(positionArg)
			.executes((sender, args) -> {
				ZoneManager.getInstance().sendDebugWorld(sender, args.getByArgument(positionArg));
			})
			.register();
	}
}
