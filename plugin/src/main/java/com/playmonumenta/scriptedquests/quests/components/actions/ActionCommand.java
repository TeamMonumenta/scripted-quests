package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.mojang.brigadier.ParseResults;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

public class ActionCommand implements ActionBase {
	private String mCommand;

	public ActionCommand(JsonElement element) throws Exception {
		mCommand = element.getAsString();
		if (mCommand == null) {
			throw new Exception("Command value is not a string!");
		}

		//Need to make sure command does not have the / at the start - this breaks the Parser
		if (mCommand.charAt(0) == '/') {
			mCommand = mCommand.substring(1);
		}

		MMLog.finer("Parsing ActionCommand '" + mCommand + "'");
		String commandToTest = mCommand.replaceAll("@S", "dummy").replaceAll("@N", "00000000-0000-0000-0000-000000000000").replaceAll("@U", "00000000-0000-0000-0000-000000000000");
		MMLog.finer("Testing ActionCommand with substitutions '" + commandToTest + "'");
		ParseResults<?> pr = NmsUtils.getVersionAdapter().parseCommand(commandToTest);
		if (pr != null && pr.getReader().canRead()) {
			throw new Exception("Invalid command: '" + mCommand + "'");
		}
		MMLog.fine("Successfully parsed ActionCommand '" + mCommand + "'");
	}

	@Override
	public void doActions(QuestContext context) {
		// Because there's no currently good way to run commands we need to run them via the console....janky....I know.
		String commandStr = mCommand;
		if (context.getNpcEntity() == null) {
			if (commandStr.contains("@N")) {
				Component commandErr = Component.text("Cannot run ScriptedQuest command without direct NPC interaction: /" + commandStr, NamedTextColor.RED);
				context.getPlayer().sendMessage(commandErr);
				return;
			}
		} else {
			commandStr = commandStr.replaceAll("@N", context.getNpcEntity().getUniqueId().toString());
		}
		commandStr = commandStr.replaceAll("@S", context.getPlayer().getName()).replaceAll("@U", context.getPlayer().getUniqueId().toString().toLowerCase());
		QuestContext.pushCurrentContext(context);
		try {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandStr);
		} finally {
			QuestContext.popCurrentContext();
		}
	}
}
