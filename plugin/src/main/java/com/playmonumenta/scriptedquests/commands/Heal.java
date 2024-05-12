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
		EntitySelectorArgument.ManyEntities entitiesArg = new EntitySelectorArgument.ManyEntities("entities");
		DoubleArgument amountArg = new DoubleArgument("amount");

		new CommandAPICommand("heal")
			.withPermission(CommandPermission.fromString("scriptedquests.heal"))
			.withArguments(entitiesArg)
			.withArguments(amountArg)
			.executes((sender, args) -> {
				Collection<Entity> entities = args.getByArgument(entitiesArg);
				double amount = args.getByArgument(amountArg);
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
