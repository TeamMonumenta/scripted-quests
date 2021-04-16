package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class ReloadTranslations {
	public static void register(Plugin plugin) {
		CommandPermission perm = CommandPermission.fromString("monumenta.reloadtranslations");

		new CommandAPICommand("reloadtranslations")
			.withPermission(perm)
			.executes((sender, args) -> {
				plugin.mTranslationManager.reload(plugin, sender);
			})
			.register();
	}
}
