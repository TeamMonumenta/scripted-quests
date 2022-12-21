package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import java.util.Collection;
import java.util.SplittableRandom;
import java.util.TreeSet;
import org.bukkit.entity.Entity;

public class RandomSample {
	private static final SplittableRandom mRandom = new SplittableRandom();

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("random")
			.withPermission(CommandPermission.fromString("scriptedquests.random.sample"))
			.withArguments(new MultiLiteralArgument("sample"))
			.withArguments(new EntitySelectorArgument.ManyEntities("entities"))
			.withArguments(new ObjectiveArgument("objective"))
			.withArguments(new IntegerArgument("min"))
			.withArguments(new IntegerArgument("max"))
			.executes((sender, args) -> {
				int min = (Integer)args[3];
				int max = (Integer)args[4];
				if (max < min) {
					throw CommandAPI.failWithString("max < min");
				}

				long rangeSize = (long) max - (long) min + 1;
				Collection<Entity> entities = (Collection<Entity>)args[1];
				if (entities.size() > rangeSize) {
					throw CommandAPI.failWithString("Entity count exceeds range from min to max");
				}

				String objective = (String)args[2];

				int randomMax = max + 1;
				TreeSet<Integer> previousValues = new TreeSet<>();
				for (Entity entity : entities) {
					int value = mRandom.nextInt(min, randomMax);
					for (int previousValue : previousValues) {
						if (value < previousValue) {
							break;
						}
						++value;
					}
					previousValues.add(value);
					ScoreboardUtils.setScoreboardValue(entity, objective, value);
					--randomMax;
				}
				return entities.size();
			})
			.register();
	}
}
