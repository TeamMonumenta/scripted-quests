package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.CodeEntry;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class Code {
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.code");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("word1", new TextArgument().overrideSuggestions(CodeEntry.words));
		arguments.put("word2", new TextArgument().overrideSuggestions(CodeEntry.words));
		arguments.put("word3", new TextArgument().overrideSuggestions(CodeEntry.words));

		new CommandAPICommand("code")
			.withPermission(perms)
			.withArguments(arguments)
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
