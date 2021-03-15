package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogClickableTextEntry;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogScrollingText;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class ScrollText implements CommandExecutor {

	private Plugin mPlugin;

	public ScrollText(Plugin plugin) {
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

		Player player = (Player)sender;
		if (player.hasMetadata(Constants.PLAYER_SCROLLING_DIALOG_METAKEY)) {
			DialogScrollingText.ScrollingTextActive active = (DialogScrollingText.ScrollingTextActive)
				player.getMetadata(Constants.PLAYER_SCROLLING_DIALOG_METAKEY).get(0).value();
			active.next();
			player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
		}

		return true;
	}

}
