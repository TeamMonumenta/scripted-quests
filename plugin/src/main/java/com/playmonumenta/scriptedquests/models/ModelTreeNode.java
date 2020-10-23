package com.playmonumenta.scriptedquests.models;

import me.Novalescent.mobs.spawners.RPGSpawnerGroup;
import me.Novalescent.utils.quadtree.reworked.QuadTreeValue;
import org.bukkit.Location;

public class ModelTreeNode extends QuadTreeValue {

	public ModelInstance mModel;
	public boolean mQuestMarkers;
	public ModelTreeNode(ModelInstance model) {
		super(model.mLoc);
		mModel = model;
	}

	@Override
	public void destroy() {

	}
}
