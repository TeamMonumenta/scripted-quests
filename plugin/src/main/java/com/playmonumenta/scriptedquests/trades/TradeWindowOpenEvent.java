package com.playmonumenta.scriptedquests.trades;

import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player right-clicks a villager to start trading.
 * It contains all trades available to the player as per defined prerequisites.
 * The list of trades can be modified in this event and the changed trades will be displayed to the player instead, and the event can be cancelled completely to not open the trade window at all.
 */
public class TradeWindowOpenEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private final Player mPlayer;
	private final List<Trade> mTrades;

	private boolean mCancelled;

	public static class Trade {
		private MerchantRecipe mRecipe;
		private @Nullable QuestActions mActions;
		private int mCount;
		private @Nullable ItemStack mOriginalResult;

		public Trade(Trade original) {
			this(original.getRecipe(), original.getActions(), original.getCount(), original.getOriginalResult());
		}

		public Trade(MerchantRecipe recipe, @Nullable NpcTrade npcTrade) {
			this(recipe, npcTrade == null ? null : npcTrade.getActions(), npcTrade == null ? -1 : npcTrade.getCount(), npcTrade == null ? null : npcTrade.getOriginalResult());
		}

		public Trade(MerchantRecipe recipe, @Nullable QuestActions actions, int count, @Nullable ItemStack originalResult) {
			this.mRecipe = recipe;
			this.mActions = actions;
			this.mCount = count;
			this.mOriginalResult = originalResult;
		}

		public void setRecipe(MerchantRecipe recipe) {
			mRecipe = recipe;
		}

		public MerchantRecipe getRecipe() {
			return mRecipe;
		}

		public void setActions(@Nullable QuestActions actions) {
			mActions = actions;
		}

		public @Nullable QuestActions getActions() {
			return mActions;
		}

		public void setCount(int count) {
			mCount = count;
		}

		public int getCount() {
			return mCount;
		}

		public void setOriginalResult(@Nullable ItemStack originalResult) {
			mOriginalResult = originalResult;
		}

		public @Nullable ItemStack getOriginalResult() {
			return mOriginalResult;
		}
	}

	public TradeWindowOpenEvent(Player player, List<Trade> trades) {
		this.mPlayer = player;
		this.mTrades = trades;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public List<Trade> getTrades() {
		return mTrades;
	}

	@Override
	public boolean isCancelled() {
		return mCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		mCancelled = cancel;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
