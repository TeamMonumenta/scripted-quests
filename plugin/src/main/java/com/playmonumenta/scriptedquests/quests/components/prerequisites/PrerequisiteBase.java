package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.playmonumenta.scriptedquests.quests.QuestContext;

public interface PrerequisiteBase {
	/**
	 * Function should check if the entity meets the prereqs
	 * <p>
	 * npc is carried through so that if use_npc_for_prereqs is used, entity can be
	 * set to the NPC for subsequent checks
	 * <p>
	 * npc might be null (for all interactions except those involving an NPC)
	 */
	boolean prerequisiteMet(QuestContext context);
}
