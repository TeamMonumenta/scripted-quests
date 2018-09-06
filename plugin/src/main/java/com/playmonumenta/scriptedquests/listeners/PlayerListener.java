package com.playmonumenta.scriptedquests.listeners;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.DeathLocation;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;

public class PlayerListener implements Listener {
	Plugin mPlugin = null;

	public PlayerListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (item != null && item.getType() == Material.COMPASS &&
		    player != null && !player.isSneaking()) {
			if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
				mPlugin.mQuestCompassManager.showCurrentQuest(player);
			} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
				mPlugin.mQuestCompassManager.cycleQuestTracker(player);
			}
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

		// Set the updated list on the player
		player.setMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY, new FixedMetadataValue(mPlugin, deathEntries));
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		/*
		 * If the player died and has metadata indicating they should respawn somewhere,
		 * set their respawn location there and remove the metadata
		 */
		if (player.hasMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY)) {
			Point respawnPoint = (Point)player.getMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY).get(0).value();
			event.setRespawnLocation(respawnPoint.toLocation(mPlugin.mWorld));
			player.removeMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY, mPlugin);
		}
	}
}
