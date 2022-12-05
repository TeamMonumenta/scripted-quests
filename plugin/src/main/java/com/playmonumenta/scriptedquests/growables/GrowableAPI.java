package com.playmonumenta.scriptedquests.growables;

import com.google.gson.GsonBuilder;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
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
		reload(null);
		INSTANCE = this;
	}

	/**
	 * Registers /growable commands with CommandAPI.
	 *
	 * This isn't really part of the API and should only be called once by ScriptedQuests plugin registration.
	 */
	public static void registerCommands() {
		CommandPermission perm = CommandPermission.fromString("monumenta.growstructure");

		new CommandAPICommand("growable")
			.withPermission(perm)
			.withSubcommand(new CommandAPICommand("grow")
				.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
				.withArguments(new StringArgument("label").replaceSuggestions(info -> {
					return getLabels();
				}))
				.withArguments(new IntegerArgument("ticksPerStep", 1))
				.withArguments(new IntegerArgument("blocksPerStep", 1))
				.withArguments(new BooleanArgument("callStructureGrowEvent"))
				.executes((sender, args) -> {
					try {
						String label = (String)args[1];
						sender.sendMessage("Started growing '" + label);
						grow(label, (Location)args[0], (Integer)args[2], (Integer)args[3], (Boolean)args[4]).whenComplete((growable) -> {
							if (growable.wasCancelled()) {
								sender.sendMessage("Growable '" + label + "' was cancelled after placing " + growable.getBlocksPlaced() + " blocks");
							} else {
								sender.sendMessage("Successfully grew '" + label + "' placing " + growable.getBlocksPlaced() + " blocks");
							}
						});
					} catch (Exception e) {
						CommandAPI.fail(e.getMessage());
					}
				}))
			.withSubcommand(new CommandAPICommand("add")
				.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
				.withArguments(new StringArgument("label"))
				.withArguments(new IntegerArgument("maxDepth", 1))
				.executes((sender, args) -> {
					try {
						String label = (String)args[1];
						GrowableStructure growable = add(label, (Location)args[0], (Integer)args[2]);
						sender.sendMessage("Successfully saved '" + label + "' containing " + growable.getSize() + " blocks");
					} catch (Exception e) {
						CommandAPI.fail(e.getMessage());
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
			return growable.getLabel() + ":" + Integer.toString(growable.getSize());
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
