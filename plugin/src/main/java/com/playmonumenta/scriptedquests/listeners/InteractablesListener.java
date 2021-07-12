package com.playmonumenta.scriptedquests.listeners;

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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.InteractableEntry.InteractType;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

public class InteractablesListener implements Listener {
	private static final String ADVENTURE_INTERACT_METAKEY = "ScriptedQuestsInteractable";
	private final Plugin mPlugin;

	public InteractablesListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useItemInHand() != Event.Result.DENY) {
			Action action = event.getAction();
			Player player = event.getPlayer();

			//In case this gets fixed, force adventure mode left click block to go through playerAnimationEvent
			if (player.getGameMode() == GameMode.ADVENTURE && action == Action.LEFT_CLICK_BLOCK) {
				return;
			}

			ItemStack item = event.getItem();
			Block block = event.getClickedBlock();

			if (item != null && !item.getType().isAir() && player.getCooldown(item.getType()) > 0) {
				/* Player's item is on cooldown, don't use it to interact to trigger interactions */
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
		if (player.getGameMode() != GameMode.ADVENTURE || event.getAnimationType() != PlayerAnimationType.ARM_SWING || player.getTargetBlock(null, 4).getType() == Material.AIR) {
			return;
		}

		//If the player recently used a right click or left click air
		if (MetadataUtils.happenedInRecentTicks(player, ADVENTURE_INTERACT_METAKEY, 4)) {
			return;
		}

		//Now we have definitely left clicked a block in adventure mode
		ItemStack item = player.getInventory().getItemInMainHand();

		if (item != null && !item.getType().isAir() && player.getCooldown(item.getType()) > 0) {
			/* Player's item is on cooldown, don't use it to interact to trigger interactions */
			return;
		}

		if (mPlugin.mInteractableManager.interactEvent(mPlugin, player, item, player.getTargetBlock(null, 4), Action.LEFT_CLICK_BLOCK)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		Player player = event.getPlayer();
		ItemStack item = event.getHand() == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

		if (item != null && !item.getType().isAir() && player.getCooldown(item.getType()) > 0) {
			/* Player's item is on cooldown, don't use it to interact to trigger interactions */
			return;
		}

		if (mPlugin.mInteractableManager.interactEntityEvent(mPlugin, player, item, entity)) {
			// interactEntityEvent returning true means this event should be canceled
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		if (damager instanceof Player) {
			Player player = (Player)damager;
			ItemStack item = player.getInventory().getItemInMainHand();
			if (player.isRiptiding()) {
				return;
			}

			if (item != null && !item.getType().isAir() && player.getCooldown(item.getType()) > 0) {
				/* Player's item is on cooldown, don't use it to interact to trigger interactions */
				return;
			}

			if (mPlugin.mInteractableManager.attackEntityEvent(mPlugin, player, item, damagee)) {
				// interactEntityEvent returning true means this event should be canceled
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		Player player = (Player)event.getWhoClicked();
		ItemStack item = event.getCurrentItem();
		ClickType type = event.getClick();
		InteractType interactType = null;

		switch (type) {
		case RIGHT:
			interactType = InteractType.RIGHT_CLICK_INVENTORY;
			break;
		case LEFT:
			interactType = InteractType.LEFT_CLICK_INVENTORY;
			break;
		default:
			break;
		}

		if (item == null || item.getType() == Material.AIR || interactType == null) {
			// No point continuing
			return;
		}

		if (item != null && !item.getType().isAir() && player.getCooldown(item.getType()) > 0) {
			/* Player's item is on cooldown, don't use it to interact to trigger interactions */
			return;
		}

		if (mPlugin.mInteractableManager.clickInventoryEvent(mPlugin, player, item, interactType)) {
			event.setCancelled(true);
		}
	}
}
