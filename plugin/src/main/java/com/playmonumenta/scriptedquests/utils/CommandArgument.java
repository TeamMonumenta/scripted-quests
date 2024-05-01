package com.playmonumenta.scriptedquests.utils;

import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public class CommandArgument extends GreedyStringArgument {

	public CommandArgument(String nodeName) {
		super(nodeName);
		replaceSuggestions(ArgumentSuggestions.strings(this::suggestions));
	}

	private String[] suggestions(SuggestionInfo<CommandSender> info) {
		String input = info.currentArg();
		CommandSender sender = info.sender();
		String[] cmd = input.split(" ", Integer.MAX_VALUE);
		if (cmd.length == 1) {
			String prefix = cmd[0];
			ArrayList<String> completions = new ArrayList<>();
			for (Map.Entry<String, Command> knownCommand : Bukkit.getCommandMap().getKnownCommands().entrySet()) {
				if (knownCommand.getValue().testPermissionSilent(sender)) {
					String name = knownCommand.getKey();
					if (StringUtil.startsWithIgnoreCase(name, prefix)) {
						completions.add(name);
					}
				}
			}
			completions.sort(String.CASE_INSENSITIVE_ORDER);
			return completions.toArray(new String[0]);
		} else {
			Command command = Bukkit.getCommandMap().getCommand(cmd[0]);
			if (command == null || !command.testPermissionSilent(sender)) {
				return new String[0];
			}
			List<String> tabComplete = command.tabComplete(sender, cmd[0], Arrays.copyOfRange(cmd, 1, cmd.length), null);
			String lastArg = cmd[cmd.length - 1];
			if (tabComplete.stream().allMatch(tc -> StringUtils.startsWithIgnoreCase(tc, lastArg))) {
				String allButLastArg = input.substring(0, input.lastIndexOf(' '));
				return tabComplete.stream().map(s -> allButLastArg + ' ' + s).toArray(String[]::new);
			} else {
				// Bukkit's tab completion API is missing the "range" of a suggestion, so we don't know which parts to overwrite.
				// Just add it at the end, and the user will have to correct it if it's wrong.
				return tabComplete.stream().map(s -> input + s).toArray(String[]::new);
			}
		}
	}

}
