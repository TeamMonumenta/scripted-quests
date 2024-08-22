package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.apache.commons.lang.NotImplementedException;

public class Handbook {

	public static void register(Plugin plugin) {

		new CommandAPICommand("handbook")
			.withPermission(CommandPermission.fromString("scriptedquests.handbook"))
			.withSubcommand(new CommandAPICommand("opencategory"));

	}
}
