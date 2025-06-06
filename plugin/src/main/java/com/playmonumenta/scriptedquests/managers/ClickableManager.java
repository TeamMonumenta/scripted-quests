package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.ClickableEntry;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ClickableManager {
	private final Map<String, ClickableEntry> mClickables = new HashMap<String, ClickableEntry>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
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
			player.sendMessage(Component.text("Can not do this while racing!", NamedTextColor.RED));
			return false;
		}

		if (label == null || ClickableEntry.squashLabel(label).isEmpty()) {
			player.sendMessage(Component.text("Invalid clickable label", NamedTextColor.RED));
			return false;
		}

		ClickableEntry entry = mClickables.get(ClickableEntry.squashLabel(label));
		if (entry == null) {
			player.sendMessage(Component.text("No clickable matching '" + label + "'", NamedTextColor.RED));
			return false;
		}

		entry.clickEvent(plugin, player);
		return true;
	}
}


