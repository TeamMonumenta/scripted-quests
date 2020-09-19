package com.playmonumenta.scriptedquests.models;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class ModelPart {

	public final ArmorStand mStand;
	public final Vector mCenter;

	public ModelPart(ArmorStand stand, Vector center) {
		mStand = stand;
		mCenter = center;
	}

}
