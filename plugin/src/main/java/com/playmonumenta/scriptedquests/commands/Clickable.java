package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

public class Clickable {
	public static void register(Plugin plugin) {
		Argument<String> labelArg = new StringArgument("label");
		new CommandAPICommand("clickable")
			.withPermission(CommandPermission.fromString("scriptedquests.clickable"))
			.withArguments(labelArg)
			.executes((sender, args) -> {
				return click(plugin, sender, args.getByArgument(labelArg));
			})
			.register();
	}

	private static int click(Plugin plugin, CommandSender sender, String label) {
		if (sender instanceof ProxiedCommandSender target) {
			sender = target.getCallee();
		}
		if (!(sender instanceof Player player)) {
			sender.sendMessage("This command can only be run by players");
			return 0;
		}
		if (plugin.mClickableManager != null) {
			if (plugin.mClickableManager.clickEvent(plugin, player, label)) {
				return 1;
			}
		}
		return 0;
	}
}
