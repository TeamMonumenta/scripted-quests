package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadZones {
	public static void register() {
		new CommandAPICommand("reloadzones")
			.withPermission(CommandPermission.fromString("scriptedquests.reloadzones"))
			.executes((sender, args) -> {
				sender.sendMessage(Component.text("Reloading config...", NamedTextColor.GOLD));
				Plugin.getInstance().reloadZones(sender);
				return 1;
			})
			.register();
	}
}
