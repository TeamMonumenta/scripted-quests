package com.playmonumenta.scriptedquests.senderid;

import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;

/*
 * NOTE: This is always loaded, so holding it in memory is a non-issue.
 */
public class RemoteConsoleSenderId extends SenderId {
	private final RemoteConsoleCommandSender mSender;

	public RemoteConsoleSenderId(RemoteConsoleCommandSender sender) {
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
		return other instanceof RemoteConsoleSenderId;
	}

	@Override
	public String toString() {
		return "RemoteConsoleSenderId";
	}
}
