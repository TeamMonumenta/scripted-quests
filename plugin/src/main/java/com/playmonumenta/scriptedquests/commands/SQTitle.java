package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

public class SQTitle {

	public static void register() {
		new CommandAPICommand("sqtitle")
				.withPermission("scriptedquests.sqtitle")
				.withArguments(new PlayerArgument("target"), new StringArgument("label"))
				.executes((sender, args) -> {
					Player p = (Player) args[0];
					String label = (String) args[1];
					Plugin.getInstance().mTitleManager.getTitle(label).runTitle(p)	;
				})
				.register();
	}
}
