package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.mojang.brigadier.ParseResults;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

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

		ParseResults<?> pr = NmsUtils.getVersionAdapter().parseCommand(mCommand);
		if (pr != null && pr.getReader().canRead()) {
			throw new Exception("Invalid command: '" + mCommand + "'");
		}
	}

	@Override
	public void doAction(QuestContext context) {
		// Because there's no currently good way to run commands we need to run them via the console....janky....I know.
		String commandStr = mCommand;
		if (context.getNpcEntity() == null) {
			if (commandStr.contains("@N")) {
				String commandErr = ChatColor.RED + "Cannot run ScriptedQuest command without direct NPC interaction: /" + commandStr;
				context.getPlayer().spigot().sendMessage(ChatMessageType.SYSTEM, TextComponent.fromLegacyText(commandErr));
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
