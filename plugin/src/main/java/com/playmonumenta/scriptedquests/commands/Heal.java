package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelector;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Damageable;

public class Heal {
	public static void register() {

		new CommandAPICommand("heal")
			.withPermission(CommandPermission.fromString("scriptedquests.heal"))
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelector.MANY_ENTITIES),
				new DoubleArgument("amount")
			)
			.executes((sender, args) -> {
				if (args[0] instanceof Collection<?>) {
					for (Object e : (Collection<?>) args[0]) {
						if (e instanceof Damageable) {
							Damageable d = (Damageable)e;
							if (e instanceof Attributable) {
								Attributable a = (Attributable)e;
								AttributeInstance attr = a.getAttribute(Attribute.GENERIC_MAX_HEALTH);
								if (attr != null && (d.getHealth() + (Double)args[1]) > attr.getValue()) {
									d.setHealth(attr.getValue());
								} else {
									d.setHealth(d.getHealth() + (Double)args[1]);
								}
							} else {
								d.setHealth(d.getHealth() + (Double)args[1]);
							}
						}
					}
				}
			})
			.register();

	}
}
