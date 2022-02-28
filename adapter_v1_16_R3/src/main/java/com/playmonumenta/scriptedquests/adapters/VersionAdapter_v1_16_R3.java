package com.playmonumenta.scriptedquests.adapters;

import com.mojang.brigadier.ParseResults;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftCommandBlock;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class VersionAdapter_v1_16_R3 implements VersionAdapter {

	public void setAutoState(CommandBlock state, boolean auto) {
		((CraftCommandBlock) state).getTileEntity().b(auto);
	}

	public void removeAllMetadata(Plugin plugin) {
		CraftServer server = (CraftServer) plugin.getServer();
		server.getEntityMetadata().removeAll(plugin);
		server.getPlayerMetadata().removeAll(plugin);
		server.getWorldMetadata().removeAll(plugin);
		for (World world : Bukkit.getWorlds()) {
			((CraftWorld) world).getBlockMetadata().removeAll(plugin);
		}
	}

	public @Nullable ParseResults<?> parseCommand(String command) {
		try {
			String testCommandStr = command.replaceAll("@S", "testuser").replaceAll("@N", "testnpc").replaceAll("@U", UUID.randomUUID().toString().toLowerCase());
			return ((CraftServer) Bukkit.getServer()).getServer().getCommandDispatcher().dispatcher().parse(testCommandStr, ((CraftServer) Bukkit.getServer()).getServer().getServerCommandListener());
		} catch (Exception e) {
			// Failed to test the command - ignore it and print a log message
			e.printStackTrace();
			return null;
		}
	}

}
