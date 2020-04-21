package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.CodeEntry;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

public class Code {
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.code");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("word1", new TextArgument().overrideSuggestions(CodeEntry.words));
		arguments.put("word2", new TextArgument().overrideSuggestions(CodeEntry.words));
		arguments.put("word3", new TextArgument().overrideSuggestions(CodeEntry.words));

		CommandAPI.getInstance().register("code",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  submitCode(plugin, sender, (String)args[0], (String)args[1], (String)args[2]);
		                                  }
		);
	}

	private static void submitCode(Plugin plugin, CommandSender sender, String word1, String word2, String word3) throws WrapperCommandSyntaxException {
		Player player = (Player)sender;

		// Players who are racing can not interact with NPCs
		if (plugin.mRaceManager.isRacing(player)) {
			CommandAPI.fail("You can not enter a code while you are racing");
		}

		plugin.mCodeManager.playerEnteredCodeEvent(plugin, player, word1 + " " + word2 + " " + word3);
	}
}
