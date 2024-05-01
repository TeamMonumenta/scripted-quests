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
		EntitySelectorArgument.ManyEntities entitiesArg = new EntitySelectorArgument.ManyEntities("entities");
		DoubleArgument xArg = new DoubleArgument("xVel");
		DoubleArgument yArg = new DoubleArgument("yVel");
		DoubleArgument zArg = new DoubleArgument("zVel");


		new CommandAPICommand("setvelocity")
			.withPermission(CommandPermission.fromString("scriptedquests.setvelocity"))
			.withArguments(entitiesArg)
			.withArguments(xArg)
			.withArguments(yArg)
			.withArguments(zArg)
			.executes((sender, args) -> {
				Collection<Entity> entities = args.getByArgument(entitiesArg);
				for (Entity entity : entities) {
					entity.setVelocity(new Vector(args.getByArgument(xArg), args.getByArgument(yArg), args.getByArgument(zArg)));
				}
			})
			.register();
	}
}
