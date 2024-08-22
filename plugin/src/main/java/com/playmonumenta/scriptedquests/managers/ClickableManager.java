package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.ClickableEntry;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ClickableManager implements Reloadable {
	private final Map<String, ClickableEntry> mClickables = new HashMap<String, ClickableEntry>();

	public void reload(Plugin plugin, @Nullable CommandSender sender) {
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

	public boolean clickEvent(Plugin plugin, Player player, String label) {
		// Check if race allows this
		if (!plugin.mRaceManager.isNotRacingOrAllowsClickables(player)) {
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


