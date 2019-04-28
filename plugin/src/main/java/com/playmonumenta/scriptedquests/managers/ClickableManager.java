package com.playmonumenta.scriptedquests.managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.ClickableEntry;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

public class ClickableManager {
	private final Map<String, ClickableEntry> mClickables = new HashMap<String, ClickableEntry>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mClickables.clear();

		QuestUtils.loadScriptedQuests(plugin, "clickables", sender, (object) -> {
			// Load this file into a NpcTrader object
			ClickableEntry clickable = new ClickableEntry(object);

			if (mClickables.containsKey(clickable.getLabel())) {
				throw new Exception(clickable.getLabel() + "' already exists!");
			}

			mClickables.put(clickable.getLabel(), clickable);

			return clickable.getLabel() + ":" + Integer.toString(clickable.getComponents().size());
		});
	}

	public ClickableManager(Plugin plugin) {
		reload(plugin, null);
	}

	public boolean clickEvent(Plugin plugin, Player player, String label) {
		// Players who are racing can not click things
		if (plugin.mRaceManager.isRacing(player)) {
			player.sendMessage(ChatColor.RED + "Can not do this while racing!");
			return false;
		}

		if (label == null || ClickableEntry.squashLabel(label).isEmpty()) {
			player.sendMessage(ChatColor.RED + "Invalid clickable label");
			return false;
		}

		ClickableEntry entry = mClickables.get(ClickableEntry.squashLabel(label));
		if (entry == null) {
			player.sendMessage(ChatColor.RED + "No clickable matching '" + label + "'");
			return false;
		}

		entry.clickEvent(plugin, player);
		return true;
	}
}


