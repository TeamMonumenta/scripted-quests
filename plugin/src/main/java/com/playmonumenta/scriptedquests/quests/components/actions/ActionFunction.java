package com.playmonumenta.scriptedquests.quests;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import com.playmonumenta.scriptedquests.Plugin;

class ActionFunction implements ActionBase {
	private String mFunctionFileName;

	ActionFunction(JsonElement element) throws Exception {
		mFunctionFileName = element.getAsString();
		if (mFunctionFileName == null) {
			throw new Exception("function value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs) {
		//	Because there's no currently good way to run functions we need to run them via the console....janky....I know.
		String commandStr = String.format("execute %s ~ ~ ~ function %s", player.getName(), mFunctionFileName);
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);
	}
}
