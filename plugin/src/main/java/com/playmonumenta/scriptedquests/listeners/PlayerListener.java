package com.playmonumenta.scriptedquests.listeners;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.QuestDeath.DeathActions;
import com.playmonumenta.scriptedquests.quests.components.DeathLocation;
import com.playmonumenta.scriptedquests.trades.NpcTrader;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

public class PlayerListener implements Listener {
	private Plugin mPlugin = null;

	public PlayerListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		Block block = event.getClickedBlock();

		mPlugin.mInteractableManager.interactEvent(mPlugin, player, item, block, action);

		if (item != null && item.getType() == Material.COMPASS &&
		    player != null && !player.isSneaking()) {
			if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
				mPlugin.mQuestCompassManager.showCurrentQuest(player);
			} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
				mPlugin.mQuestCompassManager.cycleQuestTracker(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		Player player = event.getPlayer();
		if (entity instanceof Villager) {
			Villager villager = (Villager)entity;

			if (event.isCancelled() || villager.hasMetadata(NpcTrader.TRADER_MODIFIED_METAKEY)) {
				event.setCancelled(true);
				return;
			}

			if (!villager.isTrading() && MetadataUtils.checkOnceThisTick(mPlugin, player, "ScriptedQuestsTraderNonce")) {
				mPlugin.mTradeManager.setNpcTrades(mPlugin, villager, player);
			}
		}
		if (!event.isCancelled()) {
			ItemStack item;
			if (event.getHand() == EquipmentSlot.HAND) {
				item = player.getInventory().getItemInMainHand();
			} else {
				item = player.getInventory().getItemInOffHand();
			}
			mPlugin.mInteractableManager.interactEntityEvent(mPlugin, player, item, entity);
		}
	}


	@SuppressWarnings("unchecked")
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		mPlugin.mDeathManager.deathEvent(mPlugin, event);

		List<DeathLocation> deathEntries;
		// Check if the player has died already since last plugin load
		if (player.hasMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY)) {
			// Yes - need to get previous list of death locations from metadata
			deathEntries = (List<DeathLocation>)player.getMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY).get(0).value();
		} else {
			// No - need a new list to keep track.
			deathEntries = new LinkedList<DeathLocation>();
		}

		// Add this death location to the beginning of the list
		deathEntries.add(0, new DeathLocation(event.getEntity().getLocation(), System.currentTimeMillis()));
		if (deathEntries.size() > 3) {
			deathEntries.remove(3);
		}

		// Set the updated list on the player
		player.setMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY, new FixedMetadataValue(mPlugin, deathEntries));
	}

	@SuppressWarnings("unchecked")
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		// Stop racing (if applicable)
		mPlugin.mRaceManager.cancelRaceByDeath(event.getPlayer());

		/*
		 * If the player died and has metadata indicating they should respawn somewhere,
		 * set their respawn location there and remove the metadata
		 */
		if (player.hasMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY)) {
			Point respawnPoint = (Point)player.getMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY).get(0).value();
			event.setRespawnLocation(respawnPoint.toLocation(mPlugin.mWorld));
			player.removeMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY, mPlugin);
		}

		/*
		 * If the player died and triggered death quest, run the actions stored in metadata
		 * after the player respawns, then remove the metadata
		 */
		if (player.hasMetadata(Constants.PLAYER_RESPAWN_ACTIONS_METAKEY)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					List<DeathActions> actions = (List<DeathActions>)player.getMetadata(Constants.PLAYER_RESPAWN_ACTIONS_METAKEY).get(0).value();
					for (DeathActions action : actions) {
						action.doActions(mPlugin, player);
					}
					player.removeMetadata(Constants.PLAYER_RESPAWN_ACTIONS_METAKEY, mPlugin);
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerJoinEvent(PlayerJoinEvent event) {
		// Handle login quest events
		mPlugin.mLoginManager.loginEvent(mPlugin, event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerQuitEvent(PlayerQuitEvent event) {
		// Stop racing (if applicable)
		mPlugin.mRaceManager.cancelRace(event.getPlayer());
	}
}
