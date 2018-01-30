package pe.scriptedquests.commands;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import pe.scriptedquests.Constants;
import pe.scriptedquests.Plugin;
import pe.scriptedquests.point.AreaBounds;
import pe.scriptedquests.quests.DialogClickableTextEntry;

public class QuestTrigger implements CommandExecutor {
	private Plugin mPlugin;

	public QuestTrigger(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		// This command can be run by players at any time by typing /questtrigger or clicking
		// a chat message, potentially one that is old higher up in the chat.
		//
		// Therefore we must keep the state / arguments separate from the command itself, and
		// only use the command to know that one of the available dialog actions has been
		// chosen.

		// The player must be the CommandSender when they either type in /questtrigger or
		// click a dialog option in chat
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command can only be run by players");
			return false;
		}

		// Only one argument is allowed, an integer indicating which of the available options was chosen
		if (arg3.length != 1) {
			sender.sendMessage(ChatColor.RED + "This command requires exactly one argument");
			return false;
		}

		Player player = (Player)sender;
		int triggerIndex;

		try {
			triggerIndex = Integer.parseInt(arg3[0]);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatColor.RED + "Argument parsing failed");
			return false;
		}

		// Get the list of available dialogs the player can currently click
		if (player.hasMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY) &&
			player.hasMetadata(Constants.PLAYER_CLICKABLE_DIALOG_LOCATION_METAKEY)) {

			AreaBounds validLocation = (AreaBounds)player.getMetadata(Constants.PLAYER_CLICKABLE_DIALOG_LOCATION_METAKEY).get(0).value();

			@SuppressWarnings("unchecked")
			ArrayList<DialogClickableTextEntry> availTriggers = (ArrayList<DialogClickableTextEntry>)
			                                                    player.getMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY).get(0).value();

			// Player can only click one dialog option per conversation
			player.removeMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY, mPlugin);
			player.removeMetadata(Constants.PLAYER_CLICKABLE_DIALOG_LOCATION_METAKEY, mPlugin);

			if (availTriggers != null && validLocation.within(player.getLocation())) {
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);

				for (DialogClickableTextEntry entry : availTriggers) {
					entry.doActionsIfIdxMatches(mPlugin, player, triggerIndex);
				}
			}
		}

		return true;
	}
}
