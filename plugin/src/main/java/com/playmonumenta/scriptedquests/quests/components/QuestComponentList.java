package com.playmonumenta.scriptedquests.quests.components;

import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.ArrayList;
import java.util.List;

public class QuestComponentList {

	private final List<QuestComponent> mComponents = new ArrayList<>();

	/**
	 * Executes the list of quest components with proper handling of the "stop" action
	 *
	 * @return Whether any action has executed (prerequisites met and not skipped over)
	 */
	public boolean run(QuestContext context) {
		QuestActions.mSkipLevels = 0;
		boolean anyPrereqsMet = false;
		for (QuestComponent component : mComponents) {
			if (component.doActionsIfPrereqsMet(context)) {
				anyPrereqsMet = true;
			}
			if (QuestActions.mSkipLevels > 0) {
				QuestActions.mSkipLevels = 0; // don't propagate to other files that may have called the current file via command/rerunning/etc
				break;
			}
		}
		return anyPrereqsMet;
	}

	public void add(QuestComponent questComponent) {
		mComponents.add(questComponent);
	}

	public List<QuestComponent> getComponents() {
		return mComponents;
	}

}
