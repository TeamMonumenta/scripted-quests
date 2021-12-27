package com.playmonumenta.scriptedquests.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.GuiManager;
import com.playmonumenta.scriptedquests.quests.Gui;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class GuiCommand {

	public static void register(Plugin plugin) {

		Argument guiNameArgument = new StringArgument("name")
			.replaceSuggestions(i -> plugin.mGuiManager.getGuiNames());

		Argument guiPageArgument = new StringArgument("page")
			.replaceSuggestions(i -> {
				Optional<Gui> gui = Arrays.stream(i.previousArgs()).filter(a -> a instanceof String).findFirst().map(label -> plugin.mGuiManager.getGui((String) label));
				return gui.isPresent() ? gui.get().getPages() : new String[0];
			});

		new CommandAPICommand("sqgui")
			.withPermission("scriptedquests.gui")
			.withSubcommand(
				new CommandAPICommand("show")
					.withPermission("scriptedquests.gui.show")
					.withArguments(guiNameArgument,
					               new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args[1]) {
							plugin.mGuiManager.showGui((String) args[0], player, GuiManager.MAIN_PAGE);
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("show")
					.withPermission("scriptedquests.gui.show")
					.withArguments(guiNameArgument,
					               new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
					               guiPageArgument)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args[1]) {
							plugin.mGuiManager.showGui((String) args[0], player, (String) args[2]);
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("edit")
					.withPermission("scriptedquests.gui.edit")
					.withArguments(guiNameArgument)
					.executesPlayer((sender, args) -> {
						plugin.mGuiManager.editGui((String) args[0], sender, GuiManager.MAIN_PAGE);
					})
			)
			.withSubcommand(
				new CommandAPICommand("edit")
					.withPermission("scriptedquests.gui.edit")
					.withArguments(guiNameArgument,
					               guiPageArgument)
					.executesPlayer((sender, args) -> {
						plugin.mGuiManager.editGui((String) args[0], sender, (String) args[1]);
					})
			)
			.register();

	}


}
