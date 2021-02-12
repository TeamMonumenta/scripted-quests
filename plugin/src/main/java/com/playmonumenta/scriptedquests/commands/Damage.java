package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;

import org.bukkit.entity.Damageable;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;

public class Damage {
	public static void register() {
		new CommandAPICommand("damage")
			.withPermission(CommandPermission.fromString("scriptedquests.damage"))
			.withArguments(new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES))
			.withArguments(new DoubleArgument("amount"))
			.withArguments(new BooleanArgument("respect_armor"))
			.executes((sender, args) -> {
				for (Object e : (Collection<?>)args[0]) {
					if (e instanceof Damageable) {
						Double amount = (Double)args[1];
						Damageable entity = (Damageable)e;
						if (!((boolean) args[2])) {
							if (entity.getHealth() - amount <= 0.0) {
								entity.setHealth(0);
							} else {
								entity.setHealth(entity.getHealth() - amount);
							}
							entity.damage(0); //fake damage animation
						} else {
							entity.damage(amount);
						}
					}
				}
			})
			.register();

	}
}
