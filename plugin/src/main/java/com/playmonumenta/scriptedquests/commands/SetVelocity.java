package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.Collection;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class SetVelocity {
	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("setvelocity")
			.withPermission(CommandPermission.fromString("scriptedquests.setvelocity"))
			.withArguments(new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES))
			.withArguments(new DoubleArgument("xvel"))
			.withArguments(new DoubleArgument("yvel"))
			.withArguments(new DoubleArgument("zvel"))
			.executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>)args[0]) {
					entity.setVelocity(new Vector((Double)args[1], (Double)args[2], (Double)args[3]));
				}
			})
			.register();
	}
}
