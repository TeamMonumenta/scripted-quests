package com.playmonumenta.scriptedquests.listeners;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.ZoneProperty;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockBreakEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockInteractEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ZoneEventListener implements Listener {

	private final Plugin mPlugin;

	private final Set<Material> mBlockBreakMaterials = new HashSet<>();
	private final Set<Material> mBlockInteractMaterials = new HashSet<>();

	public ZoneEventListener(Plugin plugin) {
		mPlugin = plugin;
	}

	public void update() {
		mBlockBreakMaterials.clear();
		mBlockInteractMaterials.clear();

		for (Map<String, ZoneProperty> layer : mPlugin.mZonePropertyManager.getZoneProperties().values()) {
			for (ZoneProperty zoneProperty : layer.values()) {
				zoneProperty.getEvents(ZoneBlockBreakEvent.class).forEach(event -> mBlockBreakMaterials.addAll(event.getMaterials()));
				zoneProperty.getEvents(ZoneBlockInteractEvent.class).forEach(event -> mBlockInteractMaterials.addAll(event.getMaterials()));
			}
		}
	}

	private <T extends ZoneEvent> void execute(Location location, Class<T> eventClass, Consumer<Collection<? extends T>> action) {
		for (Map.Entry<String, Map<String, ZoneProperty>> layer : mPlugin.mZonePropertyManager.getZoneProperties().entrySet()) {
			for (Map.Entry<String, ZoneProperty> property : layer.getValue().entrySet()) {
				Collection<? extends T> events = property.getValue().getEvents(eventClass);
				if (!events.isEmpty() && mPlugin.mZoneManager.hasProperty(location, layer.getKey(), property.getKey())) {
					action.accept(events);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (mBlockInteractMaterials.isEmpty()
			    || event.useInteractedBlock() == Event.Result.DENY) {
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
		execute(clickedBlock.getLocation(), ZoneBlockInteractEvent.class, events -> {
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
		execute(block.getLocation(), ZoneBlockBreakEvent.class, events -> {
			for (ZoneBlockBreakEvent e : events) {
				if (e.appliesTo(blockType)) {
					e.execute(entity, block);
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
			execute(block.getLocation(), ZoneBlockBreakEvent.class, events -> {
				for (ZoneBlockBreakEvent e : events) {
					if (e.appliesTo(blockType)) {
						e.execute(event.getBlock(), block);
					}
				}
			});
		}
	}

}
