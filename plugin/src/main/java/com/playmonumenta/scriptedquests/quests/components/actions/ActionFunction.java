package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.NmsUtils;

public class ActionFunction implements ActionBase {
	private final String mFunctionFileName;

	public ActionFunction(JsonElement element) throws Exception {
		mFunctionFileName = element.getAsString();
		if (mFunctionFileName == null) {
			throw new Exception("function value is not a string!");
		}
	}

	@Override
	public void doActions(QuestContext context) {
		String commandStr = String.format("execute as %s at @s run function %s", context.getPlayer().getName(), mFunctionFileName);
		QuestContext.pushCurrentContext(context);
		try {
			NmsUtils.getVersionAdapter().runConsoleCommandSilently(commandStr);
		} finally {
			QuestContext.popCurrentContext();
		}
	}
}
