package com.playmonumenta.scriptedquests.zones.event;

import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public abstract class ZoneEvent {

	protected final String mCommand;

	protected ZoneEvent(String command) {
		mCommand = command;
	}

	public void execute(Player player, Block block) {
		NmsUtils.getVersionAdapter().runConsoleCommandSilently(
			"execute as " + player.getName() + " at @s positioned "
				+ block.getX() + " " + block.getY() + " " + block.getZ() + " run " + mCommand);
	}

	public void execute(Entity entity, Block block) {
		NmsUtils.getVersionAdapter().runConsoleCommandSilently(
			"execute as " + entity.getUniqueId() + " at @s positioned "
				+ block.getX() + " " + block.getY() + " " + block.getZ() + " run " + mCommand);
	}

	public void execute(Block cause, Block block) {
		NmsUtils.getVersionAdapter().executeCommandAsBlock(cause,
			"execute positioned " + block.getX() + " " + block.getY() + " " + block.getZ() + " run " + mCommand);
	}

	public void execute(Entity deadEntity) {
		Location entityLoc = deadEntity.getLocation();
		NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute in " + deadEntity.getLocation().getWorld().getKey().asString() +
			" positioned " + entityLoc.getX() + " " + entityLoc.getY() + " " + entityLoc.getZ() + " run " + mCommand);
	}

}
