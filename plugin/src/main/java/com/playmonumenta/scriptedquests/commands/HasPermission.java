package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import org.bukkit.entity.Player;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class HasPermission {
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("players", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("permission", new TextArgument());

		CommandAPI.getInstance().register("haspermission",
		                                  CommandPermission.fromString("scriptedquests.haspermission"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      Player player = (Player)args[0];
		                                      String perm = (String)args[1];
		                                      if (perm.equals("*")) {
		                                          // Test if opped
		                                          if (player.isOp()) {
		                                              sender.sendMessage(player.getName() + " is an operator");
		                                              return 1;
		                                          } else {
		                                              sender.sendMessage(player.getName() + " is not an operator");
		                                              return 0;
		                                          }
		                                      } else {
		                                          // Test for permission
		                                          if (player.hasPermission(perm)) {
		                                              sender.sendMessage(player.getName() + " has permission '" + perm + "'");
		                                              return 1;
		                                          } else {
		                                              sender.sendMessage(player.getName() + " does not have permission '" + perm + "'");
		                                              return 0;
		                                          }
		                                      }
		                                  }
		);
	}
}
