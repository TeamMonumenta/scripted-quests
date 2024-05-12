package com.playmonumenta.scriptedquests.growables;

import com.google.gson.GsonBuilder;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.StringArgument;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

/**
 * Manager of growable structures, used as an API and controller.
 */
public class GrowableAPI {
	private static @Nullable GrowableAPI INSTANCE = null;

	private final Map<String, GrowableStructure> mGrowables = new HashMap<>();

	private GrowableAPI() {
		INSTANCE = this;
		reload(null);
	}

	/**
	 * Registers /growable commands with CommandAPI.
	 *
	 * This isn't really part of the API and should only be called once by ScriptedQuests plugin registration.
	 */
	public static void registerCommands() {
		CommandPermission perm = CommandPermission.fromString("monumenta.growstructure");

		LocationArgument locationArg = new LocationArgument("location", LocationType.BLOCK_POSITION);
		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(ArgumentSuggestions.strings(info -> getLabels()));
		IntegerArgument ticksArg = new IntegerArgument("ticksPerStep", 1);
		IntegerArgument blocksArg = new IntegerArgument("blocksPerStep", 1);
		BooleanArgument eventArg = new BooleanArgument("callStructureGrowEvent");
		IntegerArgument depthArg = new IntegerArgument("maxDepth", 1);

		new CommandAPICommand("growable")
			.withPermission(perm)
			.withSubcommand(new CommandAPICommand("grow")
				.withArguments(locationArg)
				.withArguments(labelArg)
				.withArguments(ticksArg)
				.withArguments(blocksArg)
				.withArguments(eventArg)
				.executes((sender, args) -> {
					try {
						String label = args.getByArgument(labelArg);
						sender.sendMessage("Started growing '" + label);
						grow(label, args.getByArgument(locationArg), args.getByArgument(ticksArg), args.getByArgument(blocksArg), args.getByArgument(eventArg)).whenComplete((growable) -> {
							if (growable.wasCancelled()) {
								sender.sendMessage("Growable '" + label + "' was cancelled after placing " + growable.getBlocksPlaced() + " blocks");
							} else {
								sender.sendMessage("Successfully grew '" + label + "' placing " + growable.getBlocksPlaced() + " blocks");
							}
						});
					} catch (Exception e) {
						throw CommandAPI.failWithString(e.getMessage());
					}
				}))
			.withSubcommand(new CommandAPICommand("add")
				.withArguments(locationArg)
				.withArguments(labelArg)
				.withArguments(depthArg)
				.executes((sender, args) -> {
					try {
						String label = args.getByArgument(labelArg);
						GrowableStructure growable = add(label, args.getByArgument(locationArg), args.getByArgument(depthArg));
						sender.sendMessage("Successfully saved '" + label + "' containing " + growable.getSize() + " blocks");
					} catch (Exception e) {
						throw CommandAPI.failWithString(e.getMessage());
					}
				}))
			.register();
	}

	/**
	 * Reload growables.
	 *
	 * If sender is non-null, it will be sent debugging information.
	 */
	public static void reload(@Nullable CommandSender sender) {
		getInstance().mGrowables.clear();
		QuestUtils.loadScriptedQuests(Plugin.getInstance(), "growables", sender, (object, file) -> {
			GrowableStructure growable = new GrowableStructure(file.getPath(), object);
			getInstance().mGrowables.put(growable.getLabel(), growable);
			return growable.getLabel() + ":" + growable.getSize();
		});
	}

	/**
	 * Grow the specified growable at the specified location at the specified speed.
	 *
	 * Returns an object that tracks the progress of this instance of the growing structure and can be used to cancel it.
	 *
	 * Throws an exception if the growable isn't loaded.
	 */
	public static GrowableProgress grow(String label, Location origin, int ticksPerStep, int blocksPerStep, boolean callStructureGrowEvent) throws IllegalArgumentException {
		GrowableStructure growable = getInstance().mGrowables.get(label);
		if (growable == null) {
			throw new IllegalArgumentException("Growable '" + label + "' does not exist");
		}

		return growable.grow(origin, ticksPerStep, blocksPerStep, callStructureGrowEvent);
	}

	/**
	 * Captures and saves the specified growable at the specified location.
	 *
	 * Throws an exception if saving the growable to the auto-computed path fails.
	 */
	public static GrowableStructure add(String label, Location origin, int maxDepth) throws Exception {
		String path = Paths.get(Plugin.getInstance().getDataFolder().getPath(), "growables", "common", label + ".json").toString();

		GrowableStructure growable = new GrowableStructure(path, origin, label, maxDepth);
		getInstance().mGrowables.put(label, growable);

		try {
			FileUtils.writeFile(path, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(growable.getAsJsonObject()));
		} catch (Exception ex) {
			throw new Exception("Failed to save growable '" + label + "' to '" + path + "': " + ex.getMessage());
		}

		return growable;
	}

	/**
	 * Gets a list of all currently loaded growables.
	 */
	public static String[] getLabels() {
		return getInstance().mGrowables.keySet().toArray(new String[getInstance().mGrowables.size()]);
	}

	/**
	 * Gets the API instance, of which only one can ever exist.
	 *
	 * Will create the instance (and load growables) if not already loaded.
	 */
	public static GrowableAPI getInstance() {
		if (INSTANCE == null) {
			return new GrowableAPI();
		}
		return INSTANCE;
	}
}
