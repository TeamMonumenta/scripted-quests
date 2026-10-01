package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogClickableTextEntry.PlayerClickableTextEntry;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.HashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class QuestTrigger implements BasicCommand {
	@Override
	public void execute(CommandSourceStack commandSourceStack, String @NonNull [] args) {
		Entity sender = commandSourceStack.getExecutor();
		// This command can be run by players at any time by typing /questtrigger or clicking
		// a chat message, potentially one that is old higher up in the chat.
		//
		// Therefore we must keep the state / arguments separate from the command itself, and
		// only use the command to know that one of the available dialog actions has been
		// chosen.

		// The player must be the CommandSender when they either type in /questtrigger or
		// click a dialog option in chat
		if (!(sender instanceof Player)) {
			sender.sendMessage(Component.text("This command can only be run by players", NamedTextColor.RED));
			return;
		}

		// Only one argument is allowed, an integer indicating which of the available options was chosen
		if (args.length != 1) {
			sender.sendMessage(Component.text("This command requires exactly one argument", NamedTextColor.RED));
			return;
		}

		Player player = (Player)sender;
		int triggerIndex;

		// Check if race allows this
		if (!Plugin.getInstance().mRaceManager.isNotRacingOrAllowsDialogClick(player)) {
			return;
		}

		try {
			triggerIndex = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			sender.sendMessage(Component.text("Argument parsing failed", NamedTextColor.RED));
			return;
		}

		// Get the list of available dialogs the player can currently click
		if (player.hasMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY)) {
			@SuppressWarnings("unchecked")
			HashMap<Integer, PlayerClickableTextEntry> availTriggers =
				(HashMap<Integer, PlayerClickableTextEntry>)
					player.getMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY).get(0).value();

			// Player can only click one dialog option per conversation
			player.removeMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY, Plugin.getInstance());

			PlayerClickableTextEntry entry = availTriggers.get(triggerIndex);
			if (entry != null) {
				entry.doActionsIfConditionsMatch(player);
			}
		}
	}

	@Override
	public @Nullable String permission() {
		return "scriptedquests.questtrigger";
	}
}
