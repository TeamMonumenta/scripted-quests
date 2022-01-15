package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.CodeEntry;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class Code {
	public static void register(Plugin plugin) {
		new CommandAPICommand("code")
			.withPermission(CommandPermission.fromString("scriptedquests.code"))
			.withArguments(new TextArgument("word1").replaceSuggestions(info -> {
				return CodeEntry.words;
			}))
			.withArguments(new TextArgument("word2").replaceSuggestions(info -> {return CodeEntry.words;}))
			.withArguments(new TextArgument("word3").replaceSuggestions(info -> {return CodeEntry.words;}))
			.executes((sender, args) -> {
				submitCode(plugin, sender, (String)args[0], (String)args[1], (String)args[2]);
			})
			.register();
	}

	private static void submitCode(Plugin plugin, CommandSender sender, String word1, String word2, String word3) throws WrapperCommandSyntaxException {
		Player player = (Player)sender;

		// Check if race allows this
		if (!plugin.mRaceManager.isNotRacingOrAllowsCode(player)) {
			CommandAPI.fail("You can not enter a code while you are racing");
		}

		plugin.mCodeManager.playerEnteredCodeEvent(plugin, player, word1 + " " + word2 + " " + word3);
	}
}
