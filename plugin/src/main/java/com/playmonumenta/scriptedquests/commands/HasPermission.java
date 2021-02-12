package com.playmonumenta.scriptedquests.commands;

import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;

public class HasPermission {
	public static void register() {
		new CommandAPICommand("haspermission")
			.withPermission(CommandPermission.fromString("scriptedquests.haspermission"))
			.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.withArguments(new TextArgument("permission"))
			.executes((sender, args) -> {
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
			})
			.register();
	}
}
