package com.playmonumenta.scriptedquests.listeners;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockBreakEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockInteractEvent;
import java.util.HashSet;
import java.util.Set;
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

		for (ZoneLayer layer : mPlugin.mZoneManager.getLayers()) {
			for (Zone zone : layer.getZones()) {
				zone.getEvents(ZoneBlockBreakEvent.class).forEach(event -> mBlockBreakMaterials.addAll(event.getMaterials()));
				zone.getEvents(ZoneBlockInteractEvent.class).forEach(event -> mBlockInteractMaterials.addAll(event.getMaterials()));
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
		for (Zone zone : mPlugin.mZoneManager.getZones(clickedBlock.getLocation()).values()) {
			zone.getEvents(ZoneBlockInteractEvent.class)
				.filter(e -> e.appliesTo(event.getAction(), clickedBlockType))
				.forEach(e -> e.execute(event.getPlayer(), clickedBlock));
		}
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
		for (Zone zone : mPlugin.mZoneManager.getZones(block.getLocation()).values()) {
			zone.getEvents(ZoneBlockBreakEvent.class)
				.filter(e -> e.appliesTo(blockType))
				.forEach(e -> e.execute(entity, block));
		}
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
			for (Zone zone : mPlugin.mZoneManager.getZones(block.getLocation()).values()) {
				zone.getEvents(ZoneBlockBreakEvent.class)
					.filter(e -> e.appliesTo(blockType))
					.forEach(e -> e.execute(event.getBlock(), block));
			}
		}
	}

}
