package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;

public class DebugZones {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		//arguments.put("position", new LocationArgument());

		CommandAPI.getInstance().register("debugzones",
		                                  CommandPermission.fromString("scriptedquests.debugzones"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      plugin.mZoneManager.sendDebug(sender, (Player) args[0]);
		                                  }
		);

		arguments = new LinkedHashMap<>();
		arguments.put("position", new LocationArgument());

		CommandAPI.getInstance().register("debugzones",
		                                  CommandPermission.fromString("scriptedquests.debugzones"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      plugin.mZoneManager.sendDebug(sender, ((Location) args[0]).toVector());
		                                  }
		);
	}
}
