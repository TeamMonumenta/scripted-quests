package pe.scriptedquests.quests;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.scriptedquests.Plugin;

class ActionCommand implements ActionBase {
	private String mCommand;

	ActionCommand(JsonElement element) throws Exception {
		mCommand = element.getAsString();
		if (mCommand == null) {
			throw new Exception("Command value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs) {
		//	Because there's no currently good way to run commands we need to run them via the console....janky....I know.
		String commandStr = mCommand.replaceAll("@S", player.getName());
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);
	}
}
