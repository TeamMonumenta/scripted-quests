package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestDataLink;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class QuestDataLinkManager {

	private final Plugin mPlugin;
	private Map<String, QuestDataLink> mQuestDataLinks = new HashMap<>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mQuestDataLinks.clear();

		QuestUtils.loadScriptedQuests(plugin, "questdata", sender, (object) -> {
			// Load this file into a QuestNpc object
			QuestDataLink questDataLink = new QuestDataLink(object);

			mQuestDataLinks.put(questDataLink.mId, questDataLink);

			return questDataLink.mId + ":" + questDataLink.mDisplayName + ":" + questDataLink.getLinks().size();
		});
	}

	public QuestDataLinkManager(Plugin plugin) {
		mPlugin = plugin;
	}

	public QuestDataLink getQuestDataLink(String id) {
		return mQuestDataLinks.get(id);
	}

}
