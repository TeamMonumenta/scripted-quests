package com.playmonumenta.scriptedquests.trades;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player right-clicks a villager to start trading.
 * It contains all trades available to the player as per defined prerequisites.
 * The list of trades can be modified in this event and the changed trades will be displayed to the player instead, and the event can be cancelled completely to not open the trade window at all.
 * <p>
 * Note: if any trades have actions defined on them, their indices must not be changed in the trades list, or the actions will run for the wrong trades!
 * Preferably only add new trades or remove trades from the end.
 */
public class TradeWindowOpenEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private final Player mPlayer;
	private final List<MerchantRecipe> mTrades;

	private boolean mCancelled;

	public TradeWindowOpenEvent(Player player, List<MerchantRecipe> trades) {
		this.mPlayer = player;
		this.mTrades = trades;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public List<MerchantRecipe> getTrades() {
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
