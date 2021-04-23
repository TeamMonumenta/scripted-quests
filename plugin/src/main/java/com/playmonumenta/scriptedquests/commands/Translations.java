package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class Translations {
	public static void register(Plugin plugin) {
		CommandPermission perm = CommandPermission.fromString("monumenta.translations");

		// reloadtranslations
		new CommandAPICommand("reloadtranslations")
			.withPermission(perm)
			.executes((sender, args) -> {
				plugin.mTranslationManager.reload(sender);
			})
			.register();

		// updatetranslationscsv
		new CommandAPICommand("updatetranslationtsv")
			.withPermission(perm)
			.executes((sender, args) -> {
				plugin.mTranslationManager.loadAndUpdateTSV(sender);
			})
			.register();
	}
}
