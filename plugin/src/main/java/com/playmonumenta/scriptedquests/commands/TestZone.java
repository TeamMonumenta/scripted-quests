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
			.withArguments(new TextArgument("layer").replaceSuggestions(ZoneManager.getLayerNameArgumentSuggestions()))
			.executes((sender, args) -> {
				return inLayer((Location) args[0], (String) args[1]);
			})
			.register();

		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(new LocationArgument("location"))
			.withArguments(new TextArgument("layer").replaceSuggestions(ZoneManager.getLayerNameArgumentSuggestions()))
			.withArguments(new TextArgument("property").replaceSuggestions(ZoneManager.getLoadedPropertyArgumentSuggestions(1)))
			.executes((sender, args) -> {
				return hasProperty((Location) args[0], (String) args[1], (String) args[2]);
			})
			.register();

		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(new LocationArgument("location"))
			.withArguments(new TextArgument("layer").replaceSuggestions(ZoneManager.getLayerNameArgumentSuggestions()))
			.withArguments(new TextArgument("property").replaceSuggestions(ZoneManager.getLoadedPropertyArgumentSuggestions(1)))
			.withArguments(new MultiLiteralArgument("tellresult"))
			.executes((sender, args) -> {
				String layerName = (String) args[1];
				String propertyName = (String) args[2];
				int result = hasProperty((Location) args[0], layerName, propertyName);
				String message;
				if (result == 0) {
					message = "Did not find zone in " + layerName + " with property " + propertyName;
					sender.sendMessage(Component.text(message, NamedTextColor.RED));
				} else {
					message = "Found zone in " + layerName + " with property " + propertyName;
					sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
				}
				return result;
			})
			.register();
	}

	private static int inLayer(Location loc, String layer) {
		if (ZoneManager.getInstance().getZone(loc, layer) == null) {
			return 0;
		}
		return 1;
	}

	private static int hasProperty(Location loc, String layer, String property) {
		boolean negated = property.startsWith("!");
		if (negated) {
			property = property.substring(1);
		}
		if (negated ^ ZoneManager.getInstance().hasProperty(loc, layer, property)) {
			return 1;
		}
		return 0;
	}
}
