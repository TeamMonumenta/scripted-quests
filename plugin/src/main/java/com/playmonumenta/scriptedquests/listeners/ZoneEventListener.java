package com.playmonumenta.scriptedquests.listeners;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.ZoneProperty;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockBreakEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockInteractEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneEntityDeathEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneRemoteClickEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ZoneEventListener implements Listener {

	private final Plugin mPlugin;

	private final Set<Material> mBlockBreakMaterials = new HashSet<>();
	private final Set<Material> mBlockInteractMaterials = new HashSet<>();
	private boolean mHasRemoteClickEvent = false;
	private static final String ENTITY_INTERACT_METAKEY = "ScriptedQuests_RemoteClickEntityInteract";

	public ZoneEventListener(Plugin plugin) {
		mPlugin = plugin;
	}

	public void update() {
		mBlockBreakMaterials.clear();
		mBlockInteractMaterials.clear();
		mHasRemoteClickEvent = false;

		for (Map<String, ZoneProperty> namespace : mPlugin.mZonePropertyManager.getZoneProperties().values()) {
			for (ZoneProperty zoneProperty : namespace.values()) {
				zoneProperty.getEvents(ZoneBlockBreakEvent.class).forEach(event -> mBlockBreakMaterials.addAll(event.getMaterials()));
				zoneProperty.getEvents(ZoneBlockInteractEvent.class).forEach(event -> mBlockInteractMaterials.addAll(event.getMaterials()));
				if (!mHasRemoteClickEvent && !zoneProperty.getEvents(ZoneRemoteClickEvent.class).isEmpty()) {
					mHasRemoteClickEvent = true;
				}
			}
		}
	}

	private interface EventAction<T extends ZoneEvent> {
		void execute(Collection<? extends T> events, String namespaceName, String propertyName);
	}

	private <T extends ZoneEvent> void execute(Location location, Class<T> eventClass, EventAction<T> action) {
		for (Map.Entry<String, Map<String, ZoneProperty>> namespace : mPlugin.mZonePropertyManager.getZoneProperties().entrySet()) {
			for (Map.Entry<String, ZoneProperty> property : namespace.getValue().entrySet()) {
				Collection<? extends T> events = property.getValue().getEvents(eventClass);
				if (!events.isEmpty() && ZoneManager.getInstance().hasProperty(location, namespace.getKey(), property.getKey())) {
					action.execute(events, namespace.getKey(), property.getKey());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (mHasRemoteClickEvent) {
			MetadataUtils.checkOnceThisTick(mPlugin, event.getPlayer(), ENTITY_INTERACT_METAKEY);
			execute(event.getPlayer().getLocation(), ZoneRemoteClickEvent.class, (events, namespaceName, propertyName) -> {
				for (ZoneRemoteClickEvent e : events) {
					Block block = e.getBlock(event.getPlayer(), Action.RIGHT_CLICK_AIR);
					if (block != null && ZoneManager.getInstance().hasProperty(block.getLocation(), namespaceName, propertyName)) {
						e.execute(event.getPlayer(), block);
					}
				}
			});
		}
	}

	// Cancelled PlayerInteractEvents are jank. Checking for denied interactions is similarly jank.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		if (mHasRemoteClickEvent) {
			if (!MetadataUtils.happenedThisTick(event.getPlayer(), ENTITY_INTERACT_METAKEY, 0)) { // entity interact events also cause a left click event that must be ignored
				execute(event.getPlayer().getLocation(), ZoneRemoteClickEvent.class, (events, namespaceName, propertyName) -> {
					for (ZoneRemoteClickEvent e : events) {
						Block block = e.getBlock(event.getPlayer(), event.getAction());
						if (block != null && ZoneManager.getInstance().hasProperty(block.getLocation(), namespaceName, propertyName)) {
							e.execute(event.getPlayer(), block);
						}
					}
				});
			}
		}
		if (event.useInteractedBlock() == Event.Result.DENY
			    || mBlockInteractMaterials.isEmpty()) {
			return;
		}
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null) {
			return;
		}
		Material clickedBlockType = clickedBlock.getType();
		if (!mBlockInteractMaterials.contains(clickedBlockType)) {
			return;
		}
		execute(clickedBlock.getLocation(), ZoneBlockInteractEvent.class, (events, namespaceName, propertyName) -> {
			for (ZoneBlockInteractEvent e : events) {
				if (e.appliesTo(event.getAction(), clickedBlockType)) {
					e.execute(event.getPlayer(), clickedBlock);
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (mBlockBreakMaterials.isEmpty()) {
			return;
		}
		handleBlockBreak(event.getPlayer(), event.getBlock());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		if (mBlockBreakMaterials.isEmpty()) {
			return;
		}
		Entity entity = event.getEntity();
		for (Block block : event.blockList()) {
			handleBlockBreak(entity, block);
		}
	}

	private void handleBlockBreak(Entity entity, Block block) {
		Material blockType = block.getType();
		if (!mBlockBreakMaterials.contains(blockType)) {
			return;
		}
		execute(block.getLocation(), ZoneBlockBreakEvent.class, (events, namespaceName, propertyName) -> {
			for (ZoneBlockBreakEvent e : events) {
				if (e.appliesTo(blockType)) {
					e.execute(entity, block);
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		if (event.getEntity() instanceof Player) {
			return;
		}

		Player killer = event.getEntity().getKiller();
		if (killer == null) {
			return;
		}

		String entityName = event.getEntity().getName();
		execute(event.getEntity().getLocation(), ZoneEntityDeathEvent.class, (events, layer, propertyName) -> {
			for (ZoneEntityDeathEvent e : events) {
				if (e.appliesTo(entityName.replaceAll("§\\d", ""))) {
					e.execute(killer, event.getEntity());
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		if (mBlockBreakMaterials.isEmpty()) {
			return;
		}
		for (Block block : event.blockList()) {
			Material blockType = block.getType();
			if (!mBlockBreakMaterials.contains(blockType)) {
				return;
			}
			execute(block.getLocation(), ZoneBlockBreakEvent.class, (events, namespaceName, propertyName) -> {
				for (ZoneBlockBreakEvent e : events) {
					if (e.appliesTo(blockType)) {
						e.execute(event.getBlock(), block);
					}
				}
			});
		}
	}

}
