package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.quests.TitleEntry;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * @author Tristian
 */
public class TitleManager {


	private Map<String, TitleEntry> titles;

	public TitleManager() {
		this.titles = new HashMap<>();
	}

	public void reload(Plugin plugin, CommandSender sender) {
		this.titles.clear();
		QuestUtils.loadScriptedQuests(plugin,"titles",sender,object -> {
			TitleEntry titleEntry = TitleEntry.read(object);
			titles.put(titleEntry.getLabel(), titleEntry);
			return titleEntry.getLabel();
		});
	}

	public Map<String, TitleEntry> getTitles() {
		return titles;
	}

	public TitleEntry getTitle(String label) {
		return titles.get(label);
	}
}
