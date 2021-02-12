package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.models.Model;
import com.playmonumenta.scriptedquests.models.ModelInstance;
import com.playmonumenta.scriptedquests.models.ModelTreeNode;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import me.Novalescent.utils.quadtree.reworked.QuadTree;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModelManager {

	private final Plugin mPlugin;
	private Map<String, Model> mModels = new HashMap<>();
	public Map<UUID, QuadTree<ModelTreeNode>> mQuadTrees = new HashMap<>();
	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		destroyModels();

		QuestUtils.loadScriptedQuests(plugin, "models", sender, (object) -> {
			Model model = new Model(plugin, object);

			mModels.put(model.mId, model);

			return model.mId + ":" + model.getModelParts().size() + " parts";
		});
	}

	public void destroyModels() {
		for (QuadTree quadTree : mQuadTrees.values()) {
			quadTree.destroy();
		}

		mQuadTrees.clear();
		for (Model model : mModels.values()) {
			for (ModelInstance instance : model.getInstances()) {
				instance.destroy();
			}
		}
		mModels.clear();

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

	public Model getModel(String id) {
		for (Model model : mModels.values()) {
			if (model.mId.equalsIgnoreCase(id)) {
				return model;
			}
		}
		return null;
	}

}
