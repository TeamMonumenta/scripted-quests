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

	@Override
	public String getName() {
		return mSender.getName();
	}

	@Override
	public boolean isLoaded() {
		return true;
	}

	@Override
	public @Nullable CommandSender callee() {
		return mSender;
	}

	@Override
	public SenderId calleeId() {
		return this;
	}

	@Override
	public @Nullable CommandSender caller() {
		return mSender;
	}

	@Override
	public SenderId callerId() {
		return this;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UnknownSenderId;
	}

	@Override
	public String toString() {
		return "UnknownSenderId " + getName();
	}
}
