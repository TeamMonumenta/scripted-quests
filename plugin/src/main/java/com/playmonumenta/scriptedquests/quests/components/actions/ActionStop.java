package com.playmonumenta.scriptedquests.quests.components.actions;

import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;

public class ActionStop implements ActionBase {

	public int mLevelsToSkip;

	public ActionStop(int levelsToSkip) {
		mLevelsToSkip = levelsToSkip;
	}

	@Override
	public void doActions(QuestContext context) {
		QuestActions.mSkipLevels = mLevelsToSkip;
	}

}
