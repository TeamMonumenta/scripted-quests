package com.playmonumenta.scriptedquests.senderid;

import javax.annotation.Nullable;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public abstract class SenderId {
	public static SenderId of(CommandSender sender) {
		if (sender instanceof ProxiedCommandSender proxiedSender) {
			return new ProxiedSenderId(proxiedSender);
		}
		if (sender instanceof Player player) {
			return new PlayerSenderId(player);
		}
		if (sender instanceof Entity entity) {
			return new EntitySenderId(entity);
		}
		if (sender instanceof BlockCommandSender blockSender) {
			return new BlockSenderId(blockSender);
		}
		if (sender instanceof RemoteConsoleCommandSender remoteSender) {
			return new RemoteConsoleSenderId(remoteSender);
		}
		if (sender instanceof ConsoleCommandSender consoleSender) {
			return new ConsoleSenderId(consoleSender);
		}
		return new UnknownSenderId(sender);
	}

	public abstract String getName();

	public abstract boolean isLoaded();

	public abstract @Nullable CommandSender callee();

	public abstract SenderId calleeId();

	public abstract @Nullable CommandSender caller();

	public abstract SenderId callerId();

	@Override
	public abstract boolean equals(Object other);

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public abstract String toString();
}
