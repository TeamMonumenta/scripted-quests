package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.entity.Player;

public class HasPermission {
	public static void register() {
		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		TextArgument permissionArg = new TextArgument("permission");

		new CommandAPICommand("haspermission")
			.withPermission(CommandPermission.fromString("scriptedquests.haspermission"))
			.withArguments(playerArg)
			.withArguments(permissionArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				String perm = args.getByArgument(permissionArg);
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
