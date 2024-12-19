package com.playmonumenta.scriptedquests.listeners;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.List;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EntityListener implements Listener {
	private final Plugin mPlugin;

	public EntityListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		if (damager instanceof Player player) {
			if (player.isRiptiding()) {
				return;
			}

			List<QuestNpc> npc = mPlugin.mNpcManager.getInteractNPC(damagee);
			if (npc != null) {
				/*
				 * This is definitely a quest NPC, even if the player might not be able to interact with it
				 * Cancel all damage done to it
				 */
				event.setCancelled(true);

				String damageeName = MessagingUtils.plainText(damagee.customName());

				/* Only trigger quest interactions via melee attack */
				if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
					mPlugin.mNpcManager.interactEvent(new QuestContext(mPlugin, player, damagee, false, null, player.getInventory().getItemInMainHand()),
						damageeName, damagee.getType(), npc, false);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Entity target = event.getRightClicked();
		Player player = event.getPlayer();

		List<QuestNpc> npc = mPlugin.mNpcManager.getInteractNPC(target);
		if (npc != null) {
			/*
			 * This is definitely a quest NPC, even if the player might not be able to interact with it
			 */

			String targetName = MessagingUtils.plainText(target.customName());
			QuestContext context = new QuestContext(mPlugin, player, target, false, null, player.getInventory().getItemInMainHand())
				.withPunch(false);
			mPlugin.mNpcManager.interactEvent(context, targetName, target.getType(), npc, false);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityCombustByBlockEvent(EntityCombustByBlockEvent event) {
		cancelIfNpc(event.getEntity(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		cancelIfNpc(event.getEntity(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (event.getCause() != DamageCause.CUSTOM && event.getCause() != DamageCause.VOID) {
			cancelIfNpc(event.getEntity(), event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		PotionEffect potionEffect = event.getNewEffect();
		if (
			potionEffect != null
				&& event.getAction().equals(EntityPotionEffectEvent.Action.ADDED)
				&& !potionEffect.getType().equals(PotionEffectType.HEAL)
		) {
			cancelIfNpc(event.getEntity(), event);
		}
	}

	private void cancelIfNpc(Entity damagee, Cancellable event) {
		if (mPlugin.mNpcManager.isQuestNPC(damagee)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void potionSplashEvent(PotionSplashEvent event) {
		// Don't apply potion effects to quest entities
		event.getAffectedEntities().removeIf(entity -> mPlugin.mNpcManager.isQuestNPC(entity));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void areaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		// Don't apply potion effects to quest entities
		event.getAffectedEntities().removeIf(entity -> mPlugin.mNpcManager.isQuestNPC(entity));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerLeashEntityEvent(PlayerLeashEntityEvent event) {
		cancelIfNpc(event.getEntity(), event);
	}
}
