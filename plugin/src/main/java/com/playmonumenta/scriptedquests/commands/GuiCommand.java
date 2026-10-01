package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.GuiManager;
import com.playmonumenta.scriptedquests.quests.Gui;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Collection;
import org.bukkit.entity.Player;

public class GuiCommand {

	@SuppressWarnings("unchecked")
	public static void register() {
		Argument<String> guiNameArgument = new StringArgument("name")
			.replaceSuggestions(ArgumentSuggestions.strings(info -> Plugin.getInstance().mGuiManager.getGuiNames()));

		Argument<String> guiPageArgument = new StringArgument("page")
			.replaceSuggestions(ArgumentSuggestions.strings(info -> {
				String label = info.previousArgs().getByArgument(guiNameArgument);
				Gui gui = Plugin.getInstance().mGuiManager.getGui(label);
				if (gui == null) {
					return new String[0];
				}
				return gui.getPages();
			}));

		EntitySelectorArgument.ManyPlayers playerArgument = new EntitySelectorArgument.ManyPlayers("player");

		new CommandAPICommand("sqgui")
			.withPermission("scriptedquests.gui")
			.withSubcommand(
				new CommandAPICommand("show")
					.withPermission("scriptedquests.gui.show")
					.withArguments(guiNameArgument, playerArgument)
					.withOptionalArguments(guiPageArgument)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args.getByArgument(playerArgument)) {
							Plugin.getInstance().mGuiManager.showGui(args.getByArgument(guiNameArgument), player, args.getByArgumentOrDefault(guiPageArgument, GuiManager.MAIN_PAGE));
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("edit")
					.withPermission("scriptedquests.gui.edit")
					.withArguments(guiNameArgument)
					.withOptionalArguments(guiPageArgument)
					.executesPlayer((sender, args) -> {
						Plugin.getInstance().mGuiManager.editGui(args.getByArgument(guiNameArgument), sender, args.getByArgumentOrDefault(guiPageArgument, GuiManager.MAIN_PAGE));
					})
			).register();

	}


}
