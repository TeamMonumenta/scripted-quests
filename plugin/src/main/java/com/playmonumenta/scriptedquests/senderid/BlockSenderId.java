package com.playmonumenta.scriptedquests.senderid;

import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;

/*
 * NOTE: Cannot get BlockCommandSender from block or create new instance
 * The sender must be held in memory along with the ID, which goes against
 * the original intention of the design. Rework when possible.
 */
public class BlockSenderId extends SenderId {
	public static final Set<Material> COMMAND_BLOCK_MATERIALS = Set.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK
	);

	private final BlockCommandSender mSender;
	private final @Nullable String mCommand;

	public BlockSenderId(BlockCommandSender sender) {
		mSender = sender;

		Block block = getBlock();
		BlockState blockState = block.getState();
		if (blockState instanceof CommandBlock commandBlock) {
			mCommand = commandBlock.getCommand();
		} else {
			mCommand = null;
		}
	}

	public String getName() {
		return mSender.getName();
	}

	public boolean isLoaded() {
		if (mCommand == null) {
			return false;
		}
		Location location = getLocation();
		if (location == null) {
			return false;
		}
		if (!location.isWorldLoaded() || !location.isChunkLoaded()) {
			return false;
		}
		Block block = getBlock();
		if (!COMMAND_BLOCK_MATERIALS.contains(block.getType())) {
			return false;
		}
		BlockState blockState = block.getState();
		if (blockState instanceof CommandBlock commandBlock) {
			return mCommand.equals(commandBlock.getCommand());
		} else {
			return false;
		}
	}

	public @Nullable CommandSender callee() {
		return mSender;
	}

	public SenderId calleeId() {
		return this;
	}

	public @Nullable CommandSender caller() {
		return mSender;
	}

	public SenderId callerId() {
		return this;
	}

	public boolean equals(Object other) {
		if (other instanceof BlockSenderId otherBlockSenderId) {
			return getLocation().equals(otherBlockSenderId.getLocation());
		}
		return false;
	}

	public String toString() {
		return "BlockSenderId " +
			getName() +
			" at " +
			getLocation();
	}

	public Block getBlock() {
		return mSender.getBlock();
	}

	public Location getLocation() {
		return getBlock().getLocation();
	}
}
