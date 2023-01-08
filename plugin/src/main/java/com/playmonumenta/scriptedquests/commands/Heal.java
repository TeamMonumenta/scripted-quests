package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.Collection;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

public class Heal {
	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("heal")
			.withPermission(CommandPermission.fromString("scriptedquests.heal"))
			.withArguments(new EntitySelectorArgument.ManyEntities("entities"))
			.withArguments(new DoubleArgument("amount"))
			.executes((sender, args) -> {
				Collection<Entity> entities = (Collection<Entity>) args[0];
				double amount = (double) args[1];
				for (Entity entity : entities) {
					if (entity instanceof Damageable damageable) {
						if (damageable instanceof Attributable attributable) {
							AttributeInstance attribute = attributable.getAttribute(Attribute.GENERIC_MAX_HEALTH);
							if (attribute != null && (damageable.getHealth() + amount) > attribute.getValue()) {
								damageable.setHealth(attribute.getValue());
							} else {
								damageable.setHealth(damageable.getHealth() + amount);
							}
						} else {
							damageable.setHealth(damageable.getHealth() + amount);
						}
					}
				}
			})
			.register();
	}
}
