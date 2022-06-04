package com.playmonumenta.scriptedquests.senderid;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerSenderId extends SenderId {
	private final UUID mUuid;
	private String mName;

	public PlayerSenderId(Player sender) {
		mUuid = sender.getUniqueId();
		mName = sender.getName();
	}

	@Override
	public String getName() {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(mUuid);
		@Nullable String name = offlinePlayer.getName();
		if (name != null) {
			mName = name;
		}
		return mName;
	}

	@Override
	public boolean isLoaded() {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(mUuid);
		return offlinePlayer.isOnline();
	}

	@Override
	public @Nullable CommandSender callee() {
		return Bukkit.getPlayer(mUuid);
	}

	@Override
	public SenderId calleeId() {
		return this;
	}

	@Override
	public @Nullable CommandSender caller() {
		return Bukkit.getPlayer(mUuid);
	}

	@Override
	public SenderId callerId() {
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof PlayerSenderId otherPlayerSenderId) {
			return mUuid.equals(otherPlayerSenderId.mUuid);
		}
		return false;
	}

	@Override
	public String toString() {
		return "PlayerSenderId "
			+ mUuid
			+ " "
			+ getName();
	}
}
