package com.playmonumenta.scriptedquests.models;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;

public class ModelInstance {

	private Plugin mPlugin;
	private Model mModel;
	private Location mLoc;

	private List<ArmorStand> mStands = new ArrayList<>();
	public ModelInstance(Plugin plugin, Model model, Location loc) {
		mPlugin = plugin;
		mModel = model;
		mLoc = loc;
	}

	public Model getModel() {
		return mModel;
	}

	public boolean isToggled() {
		return !mStands.isEmpty();
	}

	public void toggle() {
		if (mStands.isEmpty()) {
			for (ModelPart part : mModel.getModelParts()) {
				Location synced = mLoc.clone().add(part.getCenterOffset());
				ArmorStand stand = mLoc.getWorld().spawn(synced, ArmorStand.class, (ArmorStand entity) -> {
					part.cloneIntoStand(entity);
				});
				stand.setMetadata(Constants.PART_MODEL_METAKEY, new FixedMetadataValue(mPlugin, this));
				stand.addScoreboardTag(Constants.REMOVE_ONENABLE);
				mStands.add(stand);
			}
		} else {
			for (ArmorStand stand : mStands) {
				stand.remove();
			}
			mStands.clear();
		}
	}

}
