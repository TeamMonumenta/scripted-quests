package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.CodeEntry;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.entity.Player;

public class Code {
	public static void register(Plugin plugin) {
		new CommandAPICommand("code")
			.withPermission(CommandPermission.fromString("scriptedquests.code"))
			.withArguments(new TextArgument("word1").replaceSuggestions(CodeEntry.SUGGESTIONS_WORDS))
			.withArguments(new TextArgument("word2").replaceSuggestions(CodeEntry.SUGGESTIONS_WORDS))
			.withArguments(new TextArgument("word3").replaceSuggestions(CodeEntry.SUGGESTIONS_WORDS))
			.executesPlayer((player, args) -> {
				submitCode(plugin, player, args.getUnchecked("word1"), args.getUnchecked("word2"), args.getUnchecked("word3"));
			})
			.register();
	}

	private static void submitCode(Plugin plugin, Player player, String word1, String word2, String word3) throws WrapperCommandSyntaxException {
		// Check if race allows this
		if (!plugin.mRaceManager.isNotRacingOrAllowsCode(player)) {
			throw CommandAPI.failWithString("You can not enter a code while you are racing");
		}

		plugin.mCodeManager.playerEnteredCodeEvent(plugin, player, word1 + " " + word2 + " " + word3);
	}
}
