package com.playmonumenta.scriptedquests.quests.components.actions;

import com.playmonumenta.scriptedquests.quests.QuestContext;

public class ActionCancelEvent implements ActionBase {

	private final boolean mCancel;

	public static CancelState mCancelEvent = CancelState.DEFAULT;

	public enum CancelState {
		DEFAULT, CANCEL, UNCANCEL
	}

	public ActionCancelEvent(boolean cancel) {
		mCancel = cancel;
	}

	@Override
	public void doActions(QuestContext context) {
		mCancelEvent = mCancel ? CancelState.CANCEL : CancelState.UNCANCEL;
	}

}
