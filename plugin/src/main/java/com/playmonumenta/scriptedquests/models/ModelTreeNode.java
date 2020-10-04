package com.playmonumenta.scriptedquests.models;

import me.Novalescent.mobs.spawners.RPGSpawnerGroup;
import me.Novalescent.utils.quadtree.QuadTreeNode;
import org.bukkit.Location;

public class ModelTreeNode extends QuadTreeNode {

	public ModelInstance mModel;
	public ModelTreeNode(ModelInstance model) {
		super(model.mLoc);
		mModel = model;
	}

	@Override
	public void destroy() {

		if (mChildNodes != null) {
			for (QuadTreeNode childNode : mChildNodes) {
				if (childNode != null) {
					childNode.destroy();
				}
			}
		}

		mChildNodes = null;
	}
}
