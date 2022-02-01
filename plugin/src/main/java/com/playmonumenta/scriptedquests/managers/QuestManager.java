package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.Quest;
import com.playmonumenta.scriptedquests.quests.QuestDataLink;
import com.playmonumenta.scriptedquests.quests.QuestLine;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class QuestManager {

	private final Map<String, Quest> mQuests = new HashMap<>();
	private final Map<String, QuestLine> mQuestLines = new HashMap<>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mQuests.clear();

		QuestUtils.loadScriptedQuests(plugin, "quests", sender, (object) -> {
			// Load this file into a Quest Object
			Quest quest = new Quest(object, false);

			mQuests.put(quest.getQuestId(), quest);

			return quest.getQuestId() + ":" + quest.getDisplayName() + ":" + quest.getStages().size() + " stages";
		});

		QuestUtils.loadScriptedQuests(plugin, "questdata", sender, (object) -> {
			// Load this file into a Quest Object
			Quest quest = new Quest(object, true);

			mQuests.put(quest.getQuestId(), quest);

			return quest.getQuestId() + ":" + quest.getDisplayName() + ":" + quest.getStages().size() + " stages";
		});

		QuestUtils.loadScriptedQuests(plugin, "questlines", sender, (object) -> {
			// Load this file into a Quest Object
			QuestLine questLine = new QuestLine(object);

			for (QuestLine.QuestGroup questGroup : questLine.getQuestGroups()) {
				for (String id : questGroup.getQuests()) {
					Quest quest = mQuests.get(id);
					if (quest != null) {
						if (quest.getQuestlineGroup() == null) {
							quest.setQuestlineGroup(questGroup);
						} else {
							plugin.getLogger().severe("Quest " + quest.getQuestId() + " already has Questline "
							+ questLine.getId() + " assigned to it! Skipping...");
							continue;
						}

					}
				}
			}

			mQuestLines.put(questLine.getId(), questLine);

			return questLine.getId() + ":" + questLine.getQuestlineName() + ":" + questLine.getQuestGroups().size() + " quest groups";
		});
	}

	public Quest getQuest(String id) {
		return mQuests.get(id);
	}

	public QuestLine getQuestline(String id) {
		return mQuestLines.get(id);
	}

}
