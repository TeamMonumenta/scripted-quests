package com.playmonumenta.scriptedquests.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;

public class EntityListener implements Listener {
	private Plugin mPlugin;

	public EntityListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		if (damager instanceof Player) {
			Player player = (Player)damager;
			if (player.isRiptiding()) {
				return;
			}

			QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(damagee.getCustomName(), damagee.getType());
			if (npc != null) {
				/*
				 * This is definitely a quest NPC, even if the player might not be able to interact with it
				 * Cancel all damage done to it
				 */
				event.setCancelled(true);

				/* Only trigger quest interactions via melee attack */
				if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
					mPlugin.mNpcManager.interactEvent(mPlugin, player, damagee.getCustomName(),
					                                  damagee.getType(), damagee, npc, false);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityCombustByBlockEvent(EntityCombustByBlockEvent event) {
		if (!event.isCancelled()) {
			cancelIfNpc(event.getEntity(), event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		if (!event.isCancelled()) {
			cancelIfNpc(event.getEntity(), event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (!event.isCancelled()
			&& event.getCause() != DamageCause.CUSTOM && event.getCause() != DamageCause.VOID) {
			cancelIfNpc(event.getEntity(), event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (!event.isCancelled() && event.getAction().equals(EntityPotionEffectEvent.Action.ADDED)
			&& !event.getNewEffect().getType().equals(PotionEffectType.HEAL)) {
			cancelIfNpc(event.getEntity(), event);
		}
	}

	private void cancelIfNpc(Entity damagee, Cancellable event) {
		QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(damagee.getCustomName(), damagee.getType());
		if (npc != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void potionSplashEvent(PotionSplashEvent event) {
		// Don't apply potion effects to quest entities
		event.getAffectedEntities().removeIf(entity -> mPlugin.mNpcManager.getInteractNPC(entity.getCustomName(), entity.getType()) != null);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void areaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		// Don't apply potion effects to quest entities
		event.getAffectedEntities().removeIf(entity -> mPlugin.mNpcManager.getInteractNPC(entity.getCustomName(), entity.getType()) != null);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerLeashEntityEvent(PlayerLeashEntityEvent event) {
		Entity leashee = event.getEntity();
		QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(leashee.getCustomName(), leashee.getType());

		if (npc != null) {
			event.setCancelled(true);
		}
	}
}
