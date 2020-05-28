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
						if (!((boolean) args[2])) {
							((Damageable) e).setHealth(((Damageable) e).getHealth() - (Double)args[1]);
						} else {
							((Damageable) e).damage((Double)args[1]);
						}
					}
				}
			}
		);
	}
}
