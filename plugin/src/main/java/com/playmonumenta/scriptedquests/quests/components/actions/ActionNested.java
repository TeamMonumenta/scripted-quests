package com.playmonumenta.scriptedquests.quests.components.actions;

import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.quest.ActionQuest;
import org.bukkit.entity.Entity;

import java.util.List;

public interface ActionNested {

	ActionNested getParent();

	QuestPrerequisites getPrerequisites();

	List<ActionQuest> getQuestActions();

	List<QuestComponent> getQuestComponents(Entity entity);

}
