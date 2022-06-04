package com.playmonumenta.scriptedquests.senderid;

import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;

public class ProxiedSenderId extends SenderId {
	private final SenderId mCalleeId;
	private final SenderId mCallerId;
	private final String mName;

	public ProxiedSenderId(ProxiedCommandSender sender) {
		mCalleeId = SenderId.of(sender.getCallee());
		mCallerId = SenderId.of(sender.getCaller());
		mName = sender.getName();
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public boolean isLoaded() {
		return mCalleeId.isLoaded() && mCallerId.isLoaded();
	}

	@Override
	public @Nullable CommandSender callee() {
		return mCalleeId.callee();
	}

	@Override
	public SenderId calleeId() {
		return mCalleeId;
	}

	@Override
	public @Nullable CommandSender caller() {
		return mCallerId.caller();
	}

	@Override
	public SenderId callerId() {
		return mCallerId;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ProxiedSenderId otherProxiedSenderId) {
			return mCalleeId.equals(otherProxiedSenderId.mCalleeId)
				&& mCallerId.equals(otherProxiedSenderId.mCallerId);
		}
		return false;
	}

	@Override
	public String toString() {
		return "ProxiedSenderId "
			+ getName()
			+ " caller: ("
			+ callerId().toString()
			+ " ), callee: ("
			+ calleeId().toString()
			+ ")";
	}
}
