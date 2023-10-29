package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.zones.ZoneManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

public class TestZone {
	public static void register() {
		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(new LocationArgument("location"))
			.withArguments(new TextArgument("namespace").replaceSuggestions(ZoneManager.getNamespaceArgumentSuggestions()))
			.executes((sender, args) -> {
				return inNamespace((Location) args[0], (String) args[1]);
			})
			.register();

		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(new LocationArgument("location"))
			.withArguments(new TextArgument("namespace").replaceSuggestions(ZoneManager.getNamespaceArgumentSuggestions()))
			.withArguments(new TextArgument("property").replaceSuggestions(ZoneManager.getLoadedPropertyArgumentSuggestions(1)))
			.executes((sender, args) -> {
				return hasProperty((Location) args[0], (String) args[1], (String) args[2]);
			})
			.register();

		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(new LocationArgument("location"))
			.withArguments(new TextArgument("namespace").replaceSuggestions(ZoneManager.getNamespaceArgumentSuggestions()))
			.withArguments(new TextArgument("property").replaceSuggestions(ZoneManager.getLoadedPropertyArgumentSuggestions(1)))
			.withArguments(new MultiLiteralArgument("tellresult"))
			.executes((sender, args) -> {
				String namespaceName = (String) args[1];
				String propertyName = (String) args[2];
				int result = hasProperty((Location) args[0], namespaceName, propertyName);
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
