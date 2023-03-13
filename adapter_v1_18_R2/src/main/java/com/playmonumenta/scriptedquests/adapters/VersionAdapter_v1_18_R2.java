package com.playmonumenta.scriptedquests.adapters;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.adventure.AdventureComponent;
import io.papermc.paper.adventure.PaperAdventure;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftCommandBlock;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VersionAdapter_v1_18_R2 implements VersionAdapter {

	@Override
	public void setAutoState(CommandBlock state, boolean auto) {
		((CraftCommandBlock) state).getTileEntity().setAutomatic(auto);
	}

	@Override
	public void removeAllMetadata(Plugin plugin) {
		CraftServer server = (CraftServer) plugin.getServer();
		server.getEntityMetadata().removeAll(plugin);
		server.getPlayerMetadata().removeAll(plugin);
		server.getWorldMetadata().removeAll(plugin);
		for (World world : Bukkit.getWorlds()) {
			((CraftWorld) world).getBlockMetadata().removeAll(plugin);
		}
	}

	@Override
	public @Nullable ParseResults<?> parseCommand(String command) {
		try {
			String testCommandStr = command.replaceAll("@S", "testuser").replaceAll("@N", "testnpc").replaceAll("@U", UUID.randomUUID().toString().toLowerCase());
			return ((CraftServer) Bukkit.getServer()).getServer().vanillaCommandDispatcher.getDispatcher().parse(testCommandStr, ((CraftServer) Bukkit.getServer()).getServer().rconConsoleSource.createCommandSourceStack());
		} catch (Exception e) {
			// Failed to test the command - ignore it and print a log message
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Component resolveComponents(Component component, Player player) {
		try {
			return PaperAdventure.asAdventure(ComponentUtils.updateForEntity(
				((CraftPlayer) player).getHandle().createCommandSourceStack().withSource(MinecraftServer.getServer()).withPermission(4),
				new AdventureComponent(component).deepConverted(), ((CraftPlayer) player).getHandle(), 0));
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return component;
		}
	}

	@Override
	public void executeCommandAsBlock(Block block, String command) {
		CommandBlockEntity tileEntity = new CommandBlockEntity(((CraftBlock) block).getPosition(), ((CraftBlockState) block.getState()).getHandle());
		Bukkit.dispatchCommand(tileEntity.getCommandBlock().getBukkitSender(tileEntity.getCommandBlock().createCommandSourceStack()), command);
	}

}
