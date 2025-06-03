package com.playmonumenta.scriptedquests.listeners;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.QuestCompassGui;
import com.playmonumenta.scriptedquests.managers.QuestCompassManager;
import com.playmonumenta.scriptedquests.managers.SongManager;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.QuestDeath.DeathActions;
import com.playmonumenta.scriptedquests.quests.components.DeathLocation;
import com.playmonumenta.scriptedquests.trades.NpcTrader;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

public class PlayerListener implements Listener {

	private static final String ADVENTURE_INTERACT_METAKEY = "ScriptedQuestsInteractable";

	private final Plugin mPlugin;

	public PlayerListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
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
		    && item.getType() == Material.COMPASS) {
			if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && !player.isSneaking()) {
				mPlugin.mQuestCompassManager.showCurrentQuest(player);
			} else if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && !player.isSneaking()) {
				new QuestCompassGui(player, mPlugin.mQuestCompassManager).openInventory(player, mPlugin);
			} else if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && player.isSneaking()) {
				mPlugin.mQuestCompassManager.cycleQuestTracker(player);
			}
		}

		// race actions
		if (player.isSneaking()) {
			if (action == Action.LEFT_CLICK_AIR) {
				mPlugin.mRaceManager.cancelRaceByClick(player);
			} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
				mPlugin.mRaceManager.restartRaceByClick(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerAnimationEvent(PlayerAnimationEvent event) {

		Player player = event.getPlayer();
		//This only applies to players in adventure mode looking at blocks (not air)
		if (player.getGameMode() != GameMode.ADVENTURE || event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
			return;
		}

		//If the player recently used a right click or left click air
		if (MetadataUtils.happenedInRecentTicks(player, ADVENTURE_INTERACT_METAKEY, 4)) {
			return;
		}

		// abort if targeting nothing/air
		Block targetBlock = player.getTargetBlockExact(4);
		if (targetBlock == null || targetBlock.getType().isAir()) {
			return;
		}

		//Now we have definitely left-clicked a block in adventure mode
		ItemStack item = player.getInventory().getItemInMainHand();

		//Compass
		if (item.getType() == Material.COMPASS && !player.isSneaking()) {
			mPlugin.mQuestCompassManager.showCurrentQuest(player);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		Player player = event.getPlayer();
		ItemStack item = event.getHand() == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

		if (entity instanceof Villager villager) {
			// We don't want any vanilla trades to occur, regardless of if there's trades or if trades were changed or not.
			// As a side effect, right-clicking a villager will not activate interactables
			// This is fine for now, but if we ever want interactables to work on villagers, we need to change this
			event.setCancelled(true);
			if (!villager.isTrading() && MetadataUtils.checkOnceThisTick(mPlugin, player, "ScriptedQuestsTraderNonce")) {
				mPlugin.mTradeManager.trade(mPlugin, villager, player);
			}
		} else {
			List<NpcTrader> trades = mPlugin.mTradeManager.getTrades(entity.getName());
			if (trades != null) {
				event.setCancelled(true);
				if (MetadataUtils.checkOnceThisTick(mPlugin, player, "ScriptedQuestsTraderNonce")) {
					mPlugin.mTradeManager.trade(mPlugin, trades, null, entity.customName(), player);
				}
			}
		}
		if (mPlugin.mInteractableManager.interactEntityEvent(mPlugin, player, item, entity)) {
			// interactEntityEvent returning true means this event should be canceled
			event.setCancelled(true);
		}
	}


	@SuppressWarnings("unchecked")
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		mPlugin.mDeathManager.deathEvent(mPlugin, event);
		SongManager.onDeath(player);

		List<DeathLocation> deathEntries;
		// Check if the player has died already since last plugin load
		if (player.hasMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY)) {
			// Yes - need to get previous list of death locations from metadata
			deathEntries = (List<DeathLocation>)player.getMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY).get(0).value();
			// This shouldn't happen, but it makes a warning go away
			if (deathEntries == null) {
				deathEntries = new ArrayList<>(4);
			}
		} else {
			// No - need a new list to keep track.
			deathEntries = new ArrayList<>(4);
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
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		// Stop racing (if applicable)
		mPlugin.mRaceManager.cancelRaceByDeath(event.getPlayer());

		/*
		 * If the player died and has metadata indicating they should respawn somewhere,
		 * set their respawn location there and remove the metadata
		 */
		if (player.hasMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY)) {
			Point respawnPoint = (Point) Objects.requireNonNull(player.getMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY).get(0).value());
			boolean changeRespawnPoint = true;
			if (player.hasMetadata(Constants.PLAYER_RESPAWN_POINT_PREDICATE_METAKEY)) {
				Predicate<Location> respawnLocationPredicate = (Predicate<Location>) Objects.requireNonNull(player.getMetadata(Constants.PLAYER_RESPAWN_POINT_PREDICATE_METAKEY).get(0).value());
				changeRespawnPoint = respawnLocationPredicate.test(event.getRespawnLocation());
			}
			if (changeRespawnPoint) {
				event.setRespawnLocation(respawnPoint.toLocation(player.getWorld()));
			}
			player.removeMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY, mPlugin);
			player.removeMetadata(Constants.PLAYER_RESPAWN_POINT_PREDICATE_METAKEY, mPlugin);
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
					// This should never happen, but it makes a warning go away
					if (actions != null) {
						for (DeathActions action : actions) {
							action.doActions(mPlugin, player);
						}
					}
					player.removeMetadata(Constants.PLAYER_RESPAWN_ACTIONS_METAKEY, mPlugin);
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerJoinEvent(PlayerJoinEvent event) {
		// Handle login quest events
		// TODO: This works around an annoying interaction with Monumenta player data transfer. It should be removed later.
		new BukkitRunnable() {
			@Override
			public void run() {
				mPlugin.mRaceManager.onLogin(event.getPlayer());
				mPlugin.mLoginManager.loginEvent(mPlugin, event);
			}
		}.runTaskLater(mPlugin, 3);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// Stop racing (if applicable)
		mPlugin.mRaceManager.cancelRace(player);

		// Remove all zone properties from the player
		ZoneManager.getInstance().unregisterPlayer(player);

		// Stop any scheduled music for this player
		SongManager.onLogout(player);

		// Remove all metadata set by this plugin for the player
		player.removeMetadata(Constants.PLAYER_DEATH_LOCATION_METAKEY, mPlugin);
		player.removeMetadata(Constants.PLAYER_RESPAWN_ACTIONS_METAKEY, mPlugin);
		player.removeMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY, mPlugin);
		player.removeMetadata(Constants.PLAYER_RESPAWN_POINT_PREDICATE_METAKEY, mPlugin);
		player.removeMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY, mPlugin);
		player.removeMetadata(Constants.PLAYER_VOICE_OVER_METAKEY, mPlugin);
	}

}
