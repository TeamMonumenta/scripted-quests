package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.models.Model;
import com.playmonumenta.scriptedquests.models.ModelInstance;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;

import java.util.HashMap;
import java.util.Map;

public class ModelManager {

	private final Plugin mPlugin;
	private Map<String, Model> mModels = new HashMap<>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		for (Model model : mModels.values()) {
			for (ModelInstance instance : model.getInstances()) {
				instance.destroy();
 			}
		}
		mModels.clear();

		QuestUtils.loadScriptedQuests(plugin, "models", sender, (object) -> {
			// Load this file into a QuestNpc object
			Model model = new Model(plugin, object);

			mModels.put(model.mId, model);

			return model.mId + ":" + model.getModelParts().size() + " parts";
		});
	}

	public ModelManager(Plugin plugin) {
		mPlugin = plugin;
	}

	public ModelInstance getModel(ArmorStand stand) {
		if (stand.hasMetadata(Constants.PART_MODEL_METAKEY)) {
			ModelInstance model = (ModelInstance) stand.getMetadata(Constants.PART_MODEL_METAKEY).get(0).value();

			if (model != null) {
				return model;
			}
		}
		return null;
	}

}
