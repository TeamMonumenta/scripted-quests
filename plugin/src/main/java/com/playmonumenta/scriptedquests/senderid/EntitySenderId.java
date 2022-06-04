package com.playmonumenta.scriptedquests.senderid;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class EntitySenderId extends SenderId {
	private final UUID mUuid;
	private String mName;

	public EntitySenderId(Entity sender) {
		mUuid = sender.getUniqueId();
		mName = sender.getName();
	}

	public String getName() {
		@Nullable Entity entity = Bukkit.getEntity(mUuid);
		if (entity != null) {
			mName = entity.getName();
		}
		return mName;
	}

	public boolean isLoaded() {
		@Nullable Entity entity = Bukkit.getEntity(mUuid);
		return entity != null;
	}

	public @Nullable CommandSender callee() {
		return Bukkit.getEntity(mUuid);
	}

	public SenderId calleeId() {
		return this;
	}

	public @Nullable CommandSender caller() {
		return Bukkit.getEntity(mUuid);
	}

	public SenderId callerId() {
		return this;
	}

	public boolean equals(Object other) {
		if (other instanceof EntitySenderId otherEntitySenderId) {
			return mUuid.equals(otherEntitySenderId.mUuid);
		}
		return false;
	}

	public String toString() {
		return "EntitySenderId "
			+ mUuid
			+ " "
			+ getName();
	}
}
