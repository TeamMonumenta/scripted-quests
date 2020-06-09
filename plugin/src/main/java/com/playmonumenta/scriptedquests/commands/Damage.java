package com.playmonumenta.scriptedquests.commands;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.DoubleArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import org.bukkit.entity.Damageable;

import java.util.Collection;
import java.util.LinkedHashMap;

public class Damage {
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("entities", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_ENTITIES));
		arguments.put("amount", new DoubleArgument());
		arguments.put("respect_armor", new BooleanArgument());

		CommandAPI.getInstance().register("damage",
			CommandPermission.fromString("scriptedquests.damage"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}
}
