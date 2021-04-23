package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.TextArgument;

public class Translations {
	public static void register(Plugin plugin) {
		CommandPermission perm = CommandPermission.fromString("monumenta.translations");

		// reloadtranslations
		new CommandAPICommand("reloadtranslations")
			.withPermission(perm)
			.executes((sender, args) -> {
				plugin.mTranslationManager.reload(plugin, sender);
			})
			.register();

		// updatetranslationscsv
		new CommandAPICommand("updatetranslationtsv")
			.withPermission(perm)
			.executes((sender, args) -> {
				plugin.mTranslationManager.loadAndUpdateTSV(sender);
			})
			.register();

		// synctranslationsheet
		new CommandAPICommand("synctranslationsheet")
			.withPermission(perm)
			.executes((sender, args) -> {
				plugin.mTranslationManager.syncTranslationSheet(sender, null);
			})
			.register();

		new CommandAPICommand("synctranslationsheet")
			.withPermission(perm)
			.withArguments(new TextArgument("callbackKey"))
			.executes((sender, args) -> {
				plugin.mTranslationManager.syncTranslationSheet(sender, (String)args[0]);
			}).register();
	}
}
