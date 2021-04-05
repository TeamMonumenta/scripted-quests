package com.playmonumenta.scriptedquests.listeners;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
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
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

public class PlayerListener implements Listener {

	private static final String ADVENTURE_INTERACT_METAKEY = "ScriptedQuestsInteractable";

	private Plugin mPlugin;

	public PlayerListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();

		//In case this gets fixed, force adventure mode left click block to go through playerAnimationEvent
		if (player.getGameMode() == GameMode.ADVENTURE && action == Action.LEFT_CLICK_BLOCK) {
			return;
		}
		//Disable left click events for the player for the next few ticks
		//Enable them again a few ticks later
		MetadataUtils.checkOnceThisTick(mPlugin, player, ADVENTURE_INTERACT_METAKEY);

		ItemStack item = event.getItem();
		Event.Result useItem = event.useItemInHand();

		// compass
		if (useItem != Event.Result.DENY && item != null
		    && item.getType() == Material.COMPASS && !player.isSneaking()) {
			if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
				mPlugin.mQuestCompassManager.showCurrentQuest(player);
			} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
				mPlugin.mQuestCompassManager.cycleQuestTracker(player);
			}
		}

		// race actions
		if (player.isSneaking()) {
			if (action == Action.LEFT_CLICK_AIR) {
				mPlugin.mRaceManager.cancelRaceByClick(player);
			} else if (action == Action.RIGHT_CLICK_AIR) {
				mPlugin.mRaceManager.restartRaceByClick(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerAnimationEvent(PlayerAnimationEvent event) {

		Player player = event.getPlayer();
		//This only applies to players in adventure mode looking at blocks (not air)
		if (player.getGameMode() != GameMode.ADVENTURE || event.getAnimationType() != PlayerAnimationType.ARM_SWING || player.getTargetBlock(null, 4).getType() == Material.AIR) {
			return;
		}

		//If the player recently used a right click or left click air
		if (MetadataUtils.happenedInRecentTicks(player, ADVENTURE_INTERACT_METAKEY, 4)) {
			return;
		}

		//Now we have definitely left clicked a block in adventure mode
		ItemStack item = player.getInventory().getItemInMainHand();

		//Compass
		if (item.getType() == Material.COMPASS && !player.isSneaking()) {
			mPlugin.mQuestCompassManager.showCurrentQuest(player);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		Player player = event.getPlayer();
		ItemStack item = event.getHand() == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

		if (entity instanceof Villager) {
			Villager villager = (Villager)entity;

			if (event.isCancelled()) {
				return;
			}

			if (!villager.isTrading() && MetadataUtils.checkOnceThisTick(mPlugin, player, "ScriptedQuestsTraderNonce")) {
				mPlugin.mTradeManager.trade(mPlugin, villager, player, event);
			}
		}
		if (!event.isCancelled()
		    && mPlugin.mInteractableManager.interactEntityEvent(mPlugin, player, item, entity)) {
			// interactEntityEvent returning true means this event should be canceled
			event.setCancelled(true);
		}
	}


	@SuppressWarnings("unchecked")
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerDeathEvent(PlayerDeathEvent event) {
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

		// Prevent safe deaths from being counted
		if (event.getKeepInventory()) {
			return;
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
	public void playerRespawnEvent(PlayerRespawnEvent event) {
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
	public void playerJoinEvent(PlayerJoinEvent event) {
		// Handle login quest events
		// TODO: This works around an annoying interaction with Monumenta player data transfer. It should be removed later.
		new BukkitRunnable() {
			@Override
			public void run() {
				mPlugin.mLoginManager.loginEvent(mPlugin, event);
			}
		}.runTaskLater(mPlugin, 3);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerQuitEvent(PlayerQuitEvent event) {
		// Stop racing (if applicable)
		mPlugin.mRaceManager.cancelRace(event.getPlayer());

		// Remove all zone properties from the player
		mPlugin.mZoneManager.unregisterPlayer(event.getPlayer());
	}

}
