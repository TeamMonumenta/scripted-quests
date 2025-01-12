package com.playmonumenta.scriptedquests.adapters;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.adventure.AdventureComponent;
import io.papermc.paper.adventure.PaperAdventure;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftCommandBlock;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("unchecked")
public class VersionAdapter_v1_20_R3 implements VersionAdapter {
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
			return MinecraftServer.getServer().getCommands().getDispatcher().parse(testCommandStr, MinecraftServer.getServer().createCommandSourceStack().withSuppressedOutput());
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

	// https://linkie.shedaniel.dev/mappings?namespace=mojang_raw&version=1.20.4&search=CURRENT_EXECUTION_CONTEXT&translateMode=none
	private static ThreadLocal<ExecutionContext<CommandSourceStack>> CURRENT_EXECUTION_CONTEXT;

	static {
		try {
			final var field = Commands.class.getDeclaredField("f");
			field.setAccessible(true);
			CURRENT_EXECUTION_CONTEXT = (ThreadLocal<ExecutionContext<CommandSourceStack>>) field.get(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void dispatchCommandInNewContext(Runnable exec) {
		// We can't actually use Commands.performCommand directly here, since that would break if
		// runConsoleCommandSilently is ran *while* handling another command. This is because minecraft will enqueue
		// the command we want to run into the current active ExecutionContext, rather than immediately performing the
		// command. Thus, the command we intend to run will be executed after the current command has completed, rather
		// than while the current command is running. We therefore need to modify the behavior here.
		final var currContext = CURRENT_EXECUTION_CONTEXT.get();

		// The goal of this is to trick minecraft into thinking there isn't an active command context.
		//noinspection ThreadLocalSetWithNull
		CURRENT_EXECUTION_CONTEXT.set(null);
		exec.run();
		CURRENT_EXECUTION_CONTEXT.set(currContext);
	}

	@Override
	public void executeCommandAsBlock(Block block, String command) {
		CommandBlockEntity tileEntity = new CommandBlockEntity(
			((CraftBlock) block).getPosition(),
			((CraftBlockState) block.getState()).getHandle()
		);

		dispatchCommandInNewContext(() -> {
			final var source = tileEntity.getCommandBlock().createCommandSourceStack();
			final var sender = tileEntity.getCommandBlock().getBukkitSender(source);
			Bukkit.dispatchCommand(sender, command);
		});
	}

	@Override
	public void runConsoleCommandSilently(String command) {
		dispatchCommandInNewContext(() -> {
			final var source = MinecraftServer.getServer().createCommandSourceStack().withSuppressedOutput();
			final var parseResults = MinecraftServer.getServer().getCommands().getDispatcher().parse(command, source);
			MinecraftServer.getServer().getCommands().performCommand(parseResults, command);
		});
	}
}
