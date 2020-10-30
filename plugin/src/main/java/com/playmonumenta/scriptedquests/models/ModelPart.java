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

	public void cloneIntoStand(ArmorStand target, float angle) {
		target.setSilent(true);
		target.setPersistent(true);
		target.setVisible(mStand.isVisible());
		target.setSmall(mStand.isSmall());
		target.setCustomName(mStand.getCustomName());
		target.setCustomNameVisible(mStand.isCustomNameVisible());
		target.setGravity(mStand.hasGravity());

		target.setArms(true);
		target.setBasePlate(mStand.hasBasePlate());


		// Poses
		target.setBodyPose(mStand.getBodyPose());
		target.setHeadPose(mStand.getHeadPose());
		target.setRightArmPose(mStand.getRightArmPose());
		target.setLeftArmPose(mStand.getLeftArmPose());
		target.setLeftLegPose(mStand.getLeftLegPose());
		target.setRightLegPose(mStand.getRightLegPose());
		Location loc = target.getLocation();
		loc.setYaw(mStand.getLocation().getYaw() + angle);
		target.teleport(loc);

		// Equips
		target.getEquipment().setArmorContents(mStand.getEquipment().getArmorContents());
		target.getEquipment().setItemInMainHand(mStand.getEquipment().getItemInMainHand());
		target.getEquipment().setItemInOffHand(mStand.getEquipment().getItemInOffHand());
	}

}
