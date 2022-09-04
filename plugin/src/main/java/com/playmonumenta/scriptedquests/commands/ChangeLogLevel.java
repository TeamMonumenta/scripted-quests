package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.MMLog;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.logging.Level;

public class ChangeLogLevel {
	public static void register() {
		new CommandAPICommand("scriptedquests")
			.withSubcommand(new CommandAPICommand("changeloglevel")
				.withPermission(CommandPermission.fromString("scriptedquests.changeloglevel"))
				.withSubcommand(new CommandAPICommand("INFO")
					.executes((sender, args) -> {
						MMLog.setLevel(Level.INFO);
					}))
				.withSubcommand(new CommandAPICommand("FINE")
					.executes((sender, args) -> {
						MMLog.setLevel(Level.FINE);
					}))
				.withSubcommand(new CommandAPICommand("FINER")
					.executes((sender, args) -> {
						MMLog.setLevel(Level.FINER);
					}))
				.withSubcommand(new CommandAPICommand("FINEST")
					.executes((sender, args) -> {
						MMLog.setLevel(Level.FINEST);
					}))
		).register();
	}
}
