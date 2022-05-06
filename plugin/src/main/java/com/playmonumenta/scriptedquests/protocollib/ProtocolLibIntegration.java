package com.playmonumenta.scriptedquests.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.playmonumenta.scriptedquests.Plugin;

public class ProtocolLibIntegration {

	private final SelectiveNPCVisibilityHandler mSelectiveNPCVisibilityHandler;

	public ProtocolLibIntegration(Plugin plugin) {
		plugin.getLogger().info("Enabling ProtocolLib integration");

		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

		// this listener registers itself in reload() if necessary
		mSelectiveNPCVisibilityHandler = new SelectiveNPCVisibilityHandler(plugin, protocolManager);

	}

	public void reload() {
		mSelectiveNPCVisibilityHandler.reload();
	}

}
