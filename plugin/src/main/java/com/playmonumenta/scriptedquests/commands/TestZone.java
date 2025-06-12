package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.zones.ZoneManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

public class TestZone {
	public static void register() {
		LocationArgument locationArg = new LocationArgument("location");

		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(locationArg)
			.withArguments(ZoneManager.getNamespaceArg())
			.executes((sender, args) -> {
				return inNamespace(args.getByArgument(locationArg), args.getByArgument(ZoneManager.getNamespaceArg()));
			})
			.register();

		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(locationArg)
			.withArguments(ZoneManager.getNamespaceArg())
			.withArguments(ZoneManager.getPropertyArg())
			.executes((sender, args) -> {
				return hasProperty(args.getByArgument(locationArg), args.getByArgument(ZoneManager.getNamespaceArg()), args.getByArgument(ZoneManager.getPropertyArg()));
			})
			.register();

		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(locationArg)
			.withArguments(ZoneManager.getNamespaceArg())
			.withArguments(ZoneManager.getPropertyArg())
			.withArguments(new LiteralArgument("tellresult"))
			.executes((sender, args) -> {
				String namespaceName = args.getByArgument(ZoneManager.getNamespaceArg());
				String propertyName = args.getByArgument(ZoneManager.getPropertyArg());
				int result = hasProperty(args.getByArgument(locationArg), namespaceName, propertyName);
				String message;
				if (result == 0) {
					message = "Did not find zone in " + namespaceName + " with property " + propertyName;
					sender.sendMessage(Component.text(message, NamedTextColor.RED));
				} else {
					message = "Found zone in " + namespaceName + " with property " + propertyName;
					sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
				}
				return result;
			})
			.register();
	}

	private static int inNamespace(Location loc, String namespaceName) {
		if (ZoneManager.getInstance().getZone(loc, namespaceName) == null) {
			return 0;
		}
		return 1;
	}

	private static int hasProperty(Location loc, String namespaceName, String property) {
		boolean negated = property.startsWith("!");
		if (negated) {
			property = property.substring(1);
		}
		if (negated ^ ZoneManager.getInstance().hasProperty(loc, namespaceName, property)) {
			return 1;
		}
		return 0;
	}
}
