package com.playmonumenta.scriptedquests.growables;

import com.playmonumenta.scriptedquests.Plugin;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the current state of a specific grow of a growable.
 */
public class GrowableProgress {
	private @Nullable BukkitRunnable mRunnable = null;
	private boolean mFinished = false;
	private int mBlocksPlaced = 0;
	private final int mTotalBlocks;
	private boolean mWasCancelled = false;
	private @Nullable Consumer<GrowableProgress> mWhenComplete = null;

	protected GrowableProgress(List<BlockState> states, Location origin, int ticksPerStep, int blocksPerStep, boolean callStructureGrowEvent) {
		mTotalBlocks = states.size();

		if (callStructureGrowEvent) {
			StructureGrowEvent event = new StructureGrowEvent(origin, TreeType.TREE, false, null, states);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				setFinished(true);
				return;
			}
			states = event.getBlocks();
		}

		List<BlockState> blocks = states;

		mRunnable = new BukkitRunnable() {
			int mIdx = 0;

			@Override
			public void run() {
				for (int i = 0; i < blocksPerStep; i++) {
					if (mIdx >= blocks.size()) {
						setFinished(false); // Note: Cancels runnable
						return;
					}
					blocks.get(mIdx).update(true);

					mIdx++;
					mBlocksPlaced += 1;

				}
			}
		};

		mRunnable.runTaskTimer(Plugin.getInstance(), 0, ticksPerStep);
	}

	/**
	 * Cancel any remaining block placement for this growable instance.
	 *
	 * Will cause it to be considered complete, running any post - completion action.
	 * Has no effect if growing was already complete or previously cancelled.
	 * It is safe to run cancel() multiple times.
	 */
	public void cancel() {
		if (!mFinished) {
			setFinished(true);
		}
	}

	/**
	 * Gets whether the structure has finished growing.
	 */
	public boolean isFinished() {
		return mFinished;
	}

	/**
	 * Gets whether the structure was cancelled before it finished growing.
	 */
	public boolean wasCancelled() {
		return mWasCancelled;
	}

	/**
	 * Gets the total number of blocks the growable would place if allowed to run to completion.
	 */
	public int getTotalBlocks() {
		return mTotalBlocks;
	}

	/**
	 * Gets the total number of blocks so far.
	 */
	public int getBlocksPlaced() {
		return mBlocksPlaced;
	}

	/**
	 * Run the supplied runnable after growing is complete.
	 *
	 * Note that if the growable is already complete, this will run immediately.
	 * Growables always grow on the main thread, so this also runs on the main thread.
	 */
	public void whenComplete(Consumer<GrowableProgress> runnable) {
		if (mFinished) {
			runnable.accept(this);
		} else {
			mWhenComplete = runnable;
		}
	}

	private void setFinished(boolean cancelled) {
		mFinished = true;
		mWasCancelled = cancelled;
		if (mRunnable != null) {
			mRunnable.cancel();
			mRunnable = null;
		}
		if (mWhenComplete != null) {
			mWhenComplete.accept(this);
			mWhenComplete = null;
		}
	}
}
