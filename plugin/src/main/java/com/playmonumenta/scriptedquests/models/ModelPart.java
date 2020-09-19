package com.playmonumenta.scriptedquests.models;

import org.bukkit.Location;
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

	public Location getCenterOffset() {
		return mStand.getLocation().subtract(mCenter);
	}

	public void cloneIntoStand(ArmorStand target) {
		mStand.setSmall(mStand.isSmall());
		mStand.setCustomName(mStand.getCustomName());
		mStand.setCustomNameVisible(mStand.isCustomNameVisible());
		mStand.setGravity(mStand.hasGravity());
		mStand.setVisible(mStand.isVisible());
		mStand.setArms(mStand.hasArms());
		mStand.setBasePlate(mStand.hasBasePlate());

		// Poses
		mStand.setBodyPose(mStand.getBodyPose());
		mStand.setHeadPose(mStand.getHeadPose());
		mStand.setRightArmPose(mStand.getRightArmPose());
		mStand.setLeftArmPose(mStand.getLeftArmPose());
		mStand.setLeftLegPose(mStand.getLeftLegPose());
		mStand.setRightLegPose(mStand.getRightLegPose());

		// Equips
		mStand.getEquipment().setArmorContents(mStand.getEquipment().getArmorContents());
		mStand.getEquipment().setItemInMainHand(mStand.getEquipment().getItemInMainHand());
		mStand.getEquipment().setItemInOffHand(mStand.getEquipment().getItemInOffHand());
	}

}
