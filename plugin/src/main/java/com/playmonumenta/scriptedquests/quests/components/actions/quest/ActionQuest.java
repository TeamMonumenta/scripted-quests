package com.playmonumenta.scriptedquests.quests.components.actions.quest;

import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import org.bukkit.Bukkit;

public class ActionQuest {

	protected String mQuestId;
	protected QuestPrerequisites mPrerequisites;

	public QuestPrerequisites getMergedPrerequisites() {
		return mPrerequisites;
	}

	public String getQuestId() {
		return mQuestId;
	}

	protected QuestPrerequisites mergePrerequisites(ActionNested parent) {
		QuestPrerequisites prerequisites = null;
		while (parent != null) {
			Bukkit.broadcastMessage("" + parent);
			if (parent.getPrerequisites() != null) {
				Bukkit.broadcastMessage(parent.getPrerequisites().getPrerequisites().size() + " found");
				if (prerequisites == null) {
					prerequisites = parent.getPrerequisites();
				} else {
					prerequisites = prerequisites.merge(parent.getPrerequisites());
				}
			}
			parent = parent.getParent();
		}

		return prerequisites;
	}

	protected void addToTopParent(ActionNested parent) {
		while (parent != null) {
			ActionNested topParent = parent.getParent();
			if (topParent != null) {
				parent = topParent;
			} else {
				parent.getQuestActions().add(this);
				break;
			}
		}
	}

}
