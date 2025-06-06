package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Damageable;

public class Damage {
	public static void register() {
		EntitySelectorArgument.ManyEntities entitiesArg = new EntitySelectorArgument.ManyEntities("entities");
		Argument<Double> amountArg = new DoubleArgument("amount");
		Argument<Boolean> armorArg = new BooleanArgument("respect_armor");

		new CommandAPICommand("damage")
			.withPermission(CommandPermission.fromString("scriptedquests.damage"))
			.withArguments(entitiesArg)
			.withArguments(amountArg)
			.withArguments(armorArg)
			.executes((sender, args) -> {
				double amount = args.getByArgument(amountArg);
				boolean respectArmor = args.getByArgument(armorArg);
				for (Object e : args.getByArgument(entitiesArg)) {
					if (e instanceof Damageable entity) {
						if (!respectArmor) {
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
