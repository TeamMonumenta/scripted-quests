package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import java.util.Collection;
import java.util.SplittableRandom;
import java.util.TreeSet;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Objective;

public class RandomSample {
	private static final SplittableRandom mRandom = new SplittableRandom();

	@SuppressWarnings("unchecked")
	public static void register() {
		EntitySelectorArgument.ManyEntities entitiesArg = new EntitySelectorArgument.ManyEntities("entities");
		ObjectiveArgument objectiveArg = new ObjectiveArgument("objective");
		IntegerArgument minArg = new IntegerArgument("min");
		IntegerArgument maxArg = new IntegerArgument("max");

		new CommandAPICommand("random")
			.withPermission(CommandPermission.fromString("scriptedquests.random.sample"))
			.withArguments(new LiteralArgument("sample"))
			.withArguments(entitiesArg)
			.withArguments(objectiveArg)
			.withArguments(minArg)
			.withArguments(maxArg)
			.executes((sender, args) -> {
				int min = args.getByArgument(minArg);
				int max = args.getByArgument(maxArg);
				if (max < min) {
					throw CommandAPI.failWithString("max < min");
				}

				long rangeSize = (long) max - (long) min + 1;
				Collection<Entity> entities = args.getByArgument(entitiesArg);
				if (entities.size() > rangeSize) {
					throw CommandAPI.failWithString("Entity count exceeds range from min to max");
				}

				Objective objective = args.getByArgument(objectiveArg);

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
