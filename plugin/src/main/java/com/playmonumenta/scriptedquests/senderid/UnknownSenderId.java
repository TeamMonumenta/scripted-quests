package com.playmonumenta.scriptedquests.senderid;

import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/*
 * NOTE: Forwards compatibility ID, keeps the sender in memory as knowledge of this type is unknown at the time.
 */
public class UnknownSenderId extends SenderId {
	private final CommandSender mSender;

	public UnknownSenderId(CommandSender sender) {
		mSender = sender;
	}

	public String getName() {
		return mSender.getName();
	}

	public boolean isLoaded() {
		return true;
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
		return other instanceof UnknownSenderId;
	}

	public String toString() {
		return "UnknownSenderId " + getName();
	}
}
