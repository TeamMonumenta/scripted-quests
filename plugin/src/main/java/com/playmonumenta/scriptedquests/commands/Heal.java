package com.playmonumenta.scriptedquests.commands;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DoubleArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.LinkedHashMap;

public class Heal {
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("entities", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_ENTITIES));
		arguments.put("amount", new DoubleArgument());

		CommandAPI.getInstance().register("heal",
			CommandPermission.fromString("scriptedquests.heal"),
			arguments,
			(sender, args) -> {
				for (Entity e : (Collection<Entity>)args[0]) {
					if (e instanceof Damageable) {
						((Damageable) e).setHealth(((Damageable) e).getHealth() + (Double)args[1]);
					}
				}
			}
		);
	}
}
