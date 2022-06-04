package com.playmonumenta.scriptedquests.senderid;

import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/*
 * NOTE: This is always loaded, so holding it in memory is a non-issue.
 */
public class ConsoleSenderId extends SenderId {
	private final ConsoleCommandSender mSender;

	public ConsoleSenderId(ConsoleCommandSender sender) {
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
		return other instanceof ConsoleSenderId;
	}

	public String toString() {
		return "ConsoleSenderId";
	}
}
