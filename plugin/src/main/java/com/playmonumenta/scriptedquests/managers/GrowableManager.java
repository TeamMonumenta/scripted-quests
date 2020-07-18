package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.GrowableStructure;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public class GrowableManager {
	private final Map<String, GrowableStructure> mGrowables = new HashMap<>();
	private final Plugin mPlugin;

	public GrowableManager(Plugin plugin) {
		mPlugin = plugin;
		reload(plugin, null);
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mGrowables.clear();
		QuestUtils.loadScriptedQuests(plugin, "growables", sender, (object) -> {
			GrowableStructure growable = new GrowableStructure(object);
			mGrowables.put(growable.getLabel(), growable);
			return growable.getLabel() + ":" + Integer.toString(growable.getSize());
		});
	}

	public int grow(String label, Location origin, int ticksPerStep, int blocksPerStep, boolean callStructureGrowEvent) throws Exception {
		GrowableStructure growable = mGrowables.get(label);
		if (growable == null) {
			throw new Exception("Growable '" + label + "' does not exist");
		}

		return growable.grow(mPlugin, origin, ticksPerStep, blocksPerStep, callStructureGrowEvent);
	}

	public int add(String label, Location origin, int maxDepth) throws Exception {
		GrowableStructure growable = new GrowableStructure(mPlugin, origin, label, maxDepth);
		mGrowables.put(label, growable);

		String path = mPlugin.getDataFolder() + File.separator + "growables" + File.separator + "common" + File.separator + label + ".json";
		FileUtils.writeFile(path, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(growable.getAsJsonObject()));

		return growable.getSize();
	}

	public String[] getLabels() {
		return mGrowables.keySet().toArray(new String[mGrowables.size()]);
	}
}
