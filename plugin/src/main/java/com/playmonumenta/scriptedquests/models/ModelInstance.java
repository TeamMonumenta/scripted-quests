package com.playmonumenta.scriptedquests.models;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import me.Novalescent.utils.VectorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModelInstance {

	private class ProgressBar {

		private int mMaxTicks;

		public ProgressBar(int maxTicks) {
			mMaxTicks = maxTicks;
		}

		public String getProgress(String message, int tick) {
			int health_length = message.length();
			char[] chars = ("||||||" + message + "||||||").toCharArray();
			double percentPerChar = 1D / (double) chars.length;
			double percent = (double) tick / (double) mMaxTicks;

			String str = "";
			for (int i = 0; i < chars.length; i++) {
				if (percent >= percentPerChar * (i)) {
					if (i >= 6 && health_length > 0) {
						health_length--;
						str += ChatColor.DARK_GREEN + Character.toString(chars[i]);
					} else {
						str += ChatColor.GREEN + Character.toString(chars[i]);
					}
				} else {
					if (i >= 6 && health_length > 0) {
						health_length--;
						str += ChatColor.GRAY + Character.toString(chars[i]);
					} else {
						str += ChatColor.DARK_GRAY + Character.toString(chars[i]);
					}
				}
			}
			return str;
		}
	}

	private Plugin mPlugin;
	private Model mModel;
	private Location mLoc;

	private List<ArmorStand> mStands = new ArrayList<>();

	private List<UUID> mUsers = new ArrayList<>();
	private List<BukkitRunnable> mRunnables = new ArrayList<>();
	public ModelInstance(Plugin plugin, Model model, Location loc) {
		mPlugin = plugin;
		mModel = model;
		mLoc = loc;
	}

	public void destroy() {
		if (isToggled()) {
			toggle();
		}
		for (BukkitRunnable runnable : mRunnables) {
			runnable.cancel();
		}
		mRunnables.clear();
		mUsers.clear();
	}

	public Model getModel() {
		return mModel;
	}

	public boolean isToggled() {
		return !mStands.isEmpty();
	}

	public void toggle() {
		if (mStands.isEmpty()) {
			double randomRotation = 360 * Math.random();

			for (ModelPart part : mModel.getModelParts()) {
				Vector vector = part.getCenterOffset().toVector();
				vector = VectorUtils.rotateYAxis(vector, randomRotation);
				ArmorStand stand = mLoc.getWorld().spawn(mLoc.clone().add(vector), ArmorStand.class, (ArmorStand entity) -> {
					part.cloneIntoStand(entity, (float) randomRotation);
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

	public void disable(int ticks) {
		if (isToggled() && ticks > 0) {
			toggle();
			BukkitRunnable runnable = new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					t++;

					if (t >= ticks) {
						this.cancel();
						toggle();
					}

				}

			};
			runnable.runTaskTimer(mPlugin, 0, 1);
			mRunnables.add(runnable);
		}
	}

	public boolean use(Player player) {
		if (!mUsers.contains(player.getUniqueId())) {

			if (mModel.mUseDisableTime > 0 && mUsers.size() > 0) {
				return false;
			}
			mUsers.add(player.getUniqueId());

			if (mModel.mOnStart != null) {
				mModel.mOnStart.doActions(null, mLoc);
			}
			if (mModel.mUseTime <= 0) {
				for (QuestComponent component : mModel.getComponents()) {
					component.doActionsIfPrereqsMet(mPlugin, player, mStands.get(0));
				}
				mUsers.remove(player.getUniqueId());
				if (mModel.mOnEnd != null) {
					mModel.mOnEnd.doActions(null, mLoc);
				}
				disable(mModel.mUseDisableTime);
			} else {
				Location loc = mLoc.clone().add(0, mModel.getHeight(), 0);
				ProgressBar bar = new ProgressBar(mModel.mUseTime);
				ArmorStand timeStand = loc.getWorld().spawn(loc, ArmorStand.class, (ArmorStand stand) -> {

					stand.setVisible(false);
					stand.setGravity(false);
					stand.setMarker(true);
					stand.setCustomNameVisible(true);
					stand.setCustomName(ChatColor.DARK_GREEN + "[" + bar.getProgress(mModel.mUseMessage, 0) + ChatColor.DARK_GREEN + "]");
					stand.addScoreboardTag(Constants.REMOVE_ONENABLE);
					});

				BukkitRunnable runnable = new BukkitRunnable() {

					int t = 0;
					@Override
					public void cancel() {
						super.cancel();
						timeStand.remove();
					}

					@Override
					public void run() {
						if (t % mModel.mOnUseTickRate == 0) {
							if (mModel.mOnTick != null) {
								mModel.mOnTick.doActions(null, mLoc);
							}
						}
						t++;
						timeStand.setCustomName(ChatColor.DARK_GREEN + "[" + bar.getProgress(mModel.mUseMessage, t) + ChatColor.DARK_GREEN + "]");

						if (t >= mModel.mUseTime) {
							this.cancel();
							for (QuestComponent component : mModel.getComponents()) {
								component.doActionsIfPrereqsMet(mPlugin, player, mStands.get(0));
							}
							mRunnables.remove(this);
							mUsers.remove(player.getUniqueId());
							disable(mModel.mUseDisableTime);
							if (mModel.mOnEnd != null) {
								mModel.mOnEnd.doActions(null, mLoc);
							}
						}

					}

				};
				runnable.runTaskTimer(mPlugin, 0, 1);
				mRunnables.add(runnable);
			}
			return true;
		}

		return false;
	}

}
