package com.playmonumenta.scriptedquests.models;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionTimerCooldown;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteTimerCooldown;
import me.Novalescent.utils.VectorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModelInstance implements Cloneable {

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
	public Location mLoc;
	private double mYaw;
	public double mCDTicks = 0;

	public List<ArmorStand> mStands = new ArrayList<>();

	private List<UUID> mUsers = new ArrayList<>();
	private List<BukkitRunnable> mRunnables = new ArrayList<>();

	public ArmorStand mQuestAcceptStand;
	public ArmorStand mQuestTurninStand;

	public ModelInstance(Plugin plugin, Model model, Location loc, double yaw) {
		mPlugin = plugin;
		mModel = model;
		mLoc = loc;
		mYaw = yaw;
	}

	public void destroy() {
		remove();
		for (BukkitRunnable runnable : mRunnables) {
			runnable.cancel();
		}
		mRunnables.clear();
		mUsers.clear();
	}

	public void remove() {
		for (ArmorStand stand : mStands) {
			stand.removeMetadata(Constants.PART_MODEL_METAKEY, mPlugin);
			stand.remove();
		}

		if (mQuestAcceptStand != null) {
			mQuestAcceptStand.remove();
			mQuestAcceptStand = null;
		}

		if (mQuestTurninStand != null) {
			mQuestTurninStand.remove();
			mQuestTurninStand = null;
		}
		for (BukkitRunnable runnable : mRunnables) {
			runnable.cancel();
		}
		mRunnables.clear();
		mStands.clear();
	}

	public Model getModel() {
		return mModel;
	}

	public boolean isToggled() {
		return !mStands.isEmpty();
	}

	public void toggle() {
		if (mCDTicks > 0) {
			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					mCDTicks -= 1;

					if (mCDTicks <= 0) {
						this.cancel();
						mRunnables.remove(this);
						toggle();
					}

				}
			};
			runnable.runTaskTimer(mPlugin, 0, 1);
			mRunnables.add(runnable);
			return;
		}

		if (mStands.isEmpty()) {
			double rotation;
			if (mYaw >= 0) {
				rotation = mYaw;
			} else {
				rotation = 360 * Math.random();
			}

			mStands.add(mLoc.getWorld().spawn(mLoc, ArmorStand.class, (ArmorStand entity) -> {
				entity.setVisible(false);
				entity.setGravity(false);
				entity.setMarker(true);
				entity.setArms(true);
				entity.setInvulnerable(true);
				entity.setSilent(true);
				entity.addScoreboardTag(me.Novalescent.Constants.REMOVE_ONENABLE);
			}));

			for (ModelPart part : mModel.getModelParts()) {
				Vector vector = part.getCenterOffset().toVector();
				vector = VectorUtils.rotateYAxis(vector, rotation);
				ArmorStand stand = mLoc.getWorld().spawn(mLoc.clone().add(vector), ArmorStand.class, (ArmorStand entity) -> {
					part.cloneIntoStand(entity, (float) (rotation + mModel.mRotation));
				});
				stand.setMetadata(Constants.PART_MODEL_METAKEY, new FixedMetadataValue(mPlugin, this));
				stand.addScoreboardTag(Constants.REMOVE_ONENABLE);
				mStands.add(stand);
			}

			if (mModel.mQuestMarker) {
				mQuestAcceptStand = mLoc.getWorld().spawn(mLoc.clone().add(0, mModel.getHeight() + 0.1, 0), ArmorStand.class, (ArmorStand stand) -> {
					stand.setSilent(true);
					stand.getEquipment().setHelmet(new ItemStack(Material.GOLD_BLOCK));
					stand.setMarker(true);
					stand.setInvulnerable(true);
					stand.setVisible(false);
					stand.setGravity(false);
					stand.setSmall(true);
					stand.addScoreboardTag(me.Novalescent.Constants.REMOVE_ONENABLE);
				});

				mQuestTurninStand = mLoc.getWorld().spawn(mLoc.clone().add(0, mModel.getHeight() + 0.1, 0), ArmorStand.class, (ArmorStand stand) -> {
					stand.setSilent(true);
					ItemStack item = new ItemStack(Material.DIAMOND_BLOCK);
					item.addUnsafeEnchantment(Enchantment.PROTECTION_FALL, 1);
					stand.getEquipment().setHelmet(item);
					stand.setMarker(true);
					stand.setInvulnerable(true);
					stand.setVisible(false);
					stand.setGravity(false);
					stand.setSmall(true);
					stand.addScoreboardTag(me.Novalescent.Constants.REMOVE_ONENABLE);
				});

				BukkitRunnable runnable = new BukkitRunnable() {

					int rotation = 0;
					double y = 0.5;
					@Override
					public void run() {
						rotation += 5;
						Location loc = mQuestAcceptStand.getLocation();
						loc.setYaw(rotation);
						loc.add(0, Math.sin(y) * 0.015, 0);
						mQuestAcceptStand.teleport(loc);

						loc = mQuestTurninStand.getLocation();
						loc.setYaw(rotation);
						loc.add(0, Math.sin(y) * 0.015, 0);
						mQuestTurninStand.teleport(loc);

						y += 0.0725;
					}
				};
				runnable.runTaskTimer(mPlugin, 0, 1);
				mRunnables.add(runnable);
			}

		} else {
			remove();
		}
	}

	public void disable(int ticks) {
		if (isToggled() && ticks > 0) {
			toggle();
			mCDTicks = ticks;
			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					mCDTicks--;

					if (mCDTicks <= 0) {
						this.cancel();
						mRunnables.remove(this);
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

			if (mModel.mPrerequisites != null && !mModel.mPrerequisites.prerequisiteMet(player, mStands.get(0))) {
				for (QuestComponent component : mModel.getFailComponents()) {
					component.doActionsIfPrereqsMet(mPlugin, player, mStands.get(0));
				}
				return true;
			}

			if (mModel.mVisibilityPrerequisites != null && !mModel.mVisibilityPrerequisites.prerequisiteMet(player, mStands.get(0))) {
				player.sendMessage(ChatColor.RED + "You cannot interact with this...");
				return true;
			}

			mUsers.add(player.getUniqueId());

			if (mModel.mOnStart != null) {
				mModel.mOnStart.doActions(null, mLoc);
			}

			if (mModel.mUseTime <= 0) {
				success(player);
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

						if (player.getLocation().distance(mLoc) > 4) {
							player.sendMessage(ChatColor.RED + "You moved too far to continue using this...");
							mRunnables.remove(this);
							mUsers.remove(player.getUniqueId());
							this.cancel();
							return;
						}

						if (t >= mModel.mUseTime) {
							this.cancel();
							mRunnables.remove(this);
							success(player);
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

	public void success(Player player) {
		for (QuestComponent component : mModel.getComponents()) {
			component.doActionsIfPrereqsMet(mPlugin, player, mStands.get(0));
		}

		if (mModel.mTimer != null) {
			ActionTimerCooldown cooldown = new ActionTimerCooldown(mModel.mTimer, getTimerString());
			cooldown.doAction(mPlugin, player, mStands.get(0), null);
		}

		mUsers.remove(player.getUniqueId());
		disable(mModel.mUseDisableTime);
		if (mModel.mOnEnd != null) {
			mModel.mOnEnd.doActions(null, mLoc);
		}
	}

	public boolean isVisible(Player player) {
		PrerequisiteTimerCooldown prereq = null;
		if (mModel.mTimer != null) {
			prereq = new PrerequisiteTimerCooldown(mModel.mTimer, getTimerString());
		}
		return (mModel.mVisibilityPrerequisites == null || mModel.mVisibilityPrerequisites.prerequisiteMet(player, mStands.get(0)))
			&& (prereq == null || !prereq.prerequisiteMet(player, mStands.get(0)));
	}

	private String getTimerString() {
		return mModel.mId + "," + mLoc.getWorld().getName() + "," + mLoc.getX() + "," + mLoc.getY() + "," + mLoc.getZ();
	}

	@Override
	public ModelInstance clone() {
		ModelInstance clone = new ModelInstance(mPlugin, mModel, mLoc.clone(), mYaw);

		return clone;
	}

}
