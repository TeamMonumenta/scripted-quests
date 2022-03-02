package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import org.bukkit.Bukkit;

public class ActionFunction implements ActionBase {
	private final String mFunctionFileName;

	public ActionFunction(JsonElement element) throws Exception {
		mFunctionFileName = element.getAsString();
		if (mFunctionFileName == null) {
			throw new Exception("function value is not a string!");
		}
	}

	@Override
	public void doAction(QuestContext context) {
		// Because there's no currently good way to run functions we need to run them via the console....janky....I know.
		String commandStr = String.format("execute as %s at @s run function %s", context.getPlayer().getName(), mFunctionFileName);
		QuestContext.pushCurrentContext(context);
		try {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandStr);
		} finally {
			QuestContext.popCurrentContext();
		}
	}
}
