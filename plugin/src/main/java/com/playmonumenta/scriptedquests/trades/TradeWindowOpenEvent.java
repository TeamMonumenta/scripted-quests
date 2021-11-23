package com.playmonumenta.scriptedquests.trades;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.MerchantRecipe;

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

	private final Player player;
	private final List<MerchantRecipe> trades;

	private boolean cancelled;

	public TradeWindowOpenEvent(Player player, List<MerchantRecipe> trades) {
		this.player = player;
		this.trades = trades;
	}

	public Player getPlayer() {
		return player;
	}

	public List<MerchantRecipe> getTrades() {
		return trades;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
