package com.playmonumenta.scriptedquests.listeners;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.InteractableEntry.InteractType;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class InteractablesListener implements Listener {
	private static final String ADVENTURE_INTERACT_METAKEY = "ScriptedQuestsInteractable";
	private static final String ATTACK_INTERACT_METAKEY = "ScriptedQuestsAttackInteraction";
	private static final String ATTACK_INTERACT_CANCEL_METAKEY = "ScriptedQuestsAttackInteractionCancel";
	private final Plugin mPlugin;

	public InteractablesListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useItemInHand() != Event.Result.DENY && event.getAction() != Action.PHYSICAL) {
			Action action = event.getAction();
			Player player = event.getPlayer();

			//In case this gets fixed, force adventure mode left click block to go through playerAnimationEvent
			if (player.getGameMode() == GameMode.ADVENTURE && action == Action.LEFT_CLICK_BLOCK) {
				return;
			}

			ItemStack item = event.getItem();
			Block block = event.getClickedBlock();

			// If a left click was handled by an attack already, don't handle it again
			if (event.getAction() == Action.LEFT_CLICK_AIR && MetadataUtils.happenedThisTick(player, ATTACK_INTERACT_METAKEY, 0)) {
				if (MetadataUtils.happenedThisTick(player, ATTACK_INTERACT_CANCEL_METAKEY, 0)) {
					event.setCancelled(true);
				}
				return;
			}

			if (mPlugin.mInteractableManager.interactEvent(mPlugin, player, item, block, action)) {
				// interactEvent returning true means this event should be canceled
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
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

		// get target block and abort if air
		Block targetBlock = player.getTargetBlockExact(4);
		if (targetBlock == null || targetBlock.getType().isAir()) {
			return;
		}

		//Now we have definitely left clicked a block in adventure mode
		ItemStack item = player.getInventory().getItemInMainHand();

		if (mPlugin.mInteractableManager.interactEvent(mPlugin, player, item, targetBlock, Action.LEFT_CLICK_BLOCK)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		Player player = event.getPlayer();
		ItemStack item = event.getHand() == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

		if (mPlugin.mInteractableManager.interactEntityEvent(mPlugin, player, item, entity)) {
			// interactEntityEvent returning true means this event should be canceled
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		if (damager instanceof Player player
			    && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
			    && !player.isRiptiding()) {

			ItemStack item = player.getInventory().getItemInMainHand();
			if (mPlugin.mInteractableManager.attackEntityEvent(mPlugin, player, item, damagee)) {
				// interactEntityEvent returning true means this event should be canceled
				event.setCancelled(true);
			}

			// Handle attacks as left clicks on air, as no interact event will be fired if a block would have been hit if the mob wasn't there
			MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, ATTACK_INTERACT_METAKEY);
			if (mPlugin.mInteractableManager.interactEvent(mPlugin, player, item, null, Action.LEFT_CLICK_AIR)) {
				// interactEvent returning true does NOT mean that this event should be canceled, but that the following interact event should be cancelled.
				MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, ATTACK_INTERACT_CANCEL_METAKEY);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
			// don't trigger when clicking while having an item on the cursor
			return;
		}

		ItemStack item = event.getCurrentItem();
		ClickType type = event.getClick();
		InteractType interactType = switch (type) {
			case RIGHT -> InteractType.RIGHT_CLICK_INVENTORY;
			case LEFT -> InteractType.LEFT_CLICK_INVENTORY;
			case SWAP_OFFHAND -> InteractType.SWAP_HANDS_INVENTORY;
			default -> null;
		};

		if (item == null || item.getType() == Material.AIR || interactType == null) {
			// No point continuing
			return;
		}

		if (mPlugin.mInteractableManager.clickInventoryEvent(mPlugin, player, item, interactType)) {
			InventoryUtils.refreshOffhand(event);
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlugin.mInteractableManager.swapHandsEvent(mPlugin, event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand())) {
			event.setCancelled(true);
		}
	}
}
