package com.playmonumenta.scriptedquests.benchmark;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.function.BooleanSupplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class Benchmark {
	private static final long HOT_METHOD_INVOKATION_THRESHOLD = 10_000L;

	protected static void benchmark(CommandSender sender, String name, BooleanSupplier testGenerator) {
		sendMessage(sender, Component.text(name + ": Pre-run", NamedTextColor.GOLD, TextDecoration.BOLD));

		long plannedRuns = HOT_METHOD_INVOKATION_THRESHOLD;
		long hits;
		long startNanos;
		long deltaNanos;
		double runsPerSecond;

		try {
			hits = 0;
			startNanos = System.nanoTime();
			for (long i = 0; i < plannedRuns; i++) {
				if (testGenerator.getAsBoolean()) {
					hits++;
				}
			}
			deltaNanos = System.nanoTime() - startNanos;
		} catch (Exception e) {
			sendMessage(sender, Component.text(name + ": An exception occurred:", NamedTextColor.RED));
			new BukkitRunnable() {
				@Override
				public void run() {
					MessagingUtils.sendStackTrace(sender, e);
				}
			}.runTask(Plugin.getInstance());
			return;
		}
		runsPerSecond = 1_000_000_000.0 // nanoseconds / second
			* plannedRuns               // runs
			/ deltaNanos;               // nanoseconds
		sendMessage(sender, Component.text(name + ": JIT optimization complete with ", NamedTextColor.GOLD)
			.append(Component.text(plannedRuns, NamedTextColor.GREEN))
			.append(Component.text(" iterations in "))
			.append(Component.text(deltaNanos / plannedRuns, NamedTextColor.GREEN))
			.append(Component.text(" nanoseconds each ("))
			.append(Component.text(String.format("%04.2f", runsPerSecond), NamedTextColor.GREEN))
			.append(Component.text(" per second) with "))
			.append(Component.text(String.format("%04.2f%%", 100.0 * hits / plannedRuns), NamedTextColor.GREEN))
			.append(Component.text(" hit rate")));

		sendMessage(sender, Component.text(name + ": Benchmark targeting >= 1 second", NamedTextColor.GOLD, TextDecoration.BOLD));

		plannedRuns = 10_000L;
		deltaNanos = 1L;
		while (deltaNanos < 1_000_000_000L) {
			try {
				hits = 0;
				startNanos = System.nanoTime();
				for (long i = 0; i < plannedRuns; i++) {
					if (testGenerator.getAsBoolean()) {
						hits++;
					}
				}
				deltaNanos = System.nanoTime() - startNanos;
			} catch (Exception e) {
				sendMessage(sender, Component.text(name + ": An exception occurred:", NamedTextColor.RED));
				new BukkitRunnable() {
					@Override
					public void run() {
						MessagingUtils.sendStackTrace(sender, e);
					}
				}.runTask(Plugin.getInstance());
				return;
			}

			if (deltaNanos < 1_000_000_000L) {
				plannedRuns *= 10L;
				continue;
			}

			runsPerSecond = 1_000_000_000.0 // nanoseconds / second
				* plannedRuns               // runs
				/ deltaNanos;               // nanoseconds
			sendMessage(sender, Component.text(name + ": Benchmark complete with ", NamedTextColor.GOLD)
				.append(Component.text(plannedRuns, NamedTextColor.GREEN))
				.append(Component.text(" iterations in "))
				.append(Component.text(deltaNanos / plannedRuns, NamedTextColor.GREEN))
				.append(Component.text(" nanoseconds each ("))
				.append(Component.text(String.format("%04.2f", runsPerSecond), NamedTextColor.GREEN))
				.append(Component.text(" per second) with "))
				.append(Component.text(String.format("%04.2f%%", 100.0 * hits / plannedRuns), NamedTextColor.GREEN))
				.append(Component.text(" hit rate")));
		}
	}

	protected static void sendMessage(CommandSender sender, Component message) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!(sender instanceof ConsoleCommandSender)) {
					Bukkit.getConsoleSender().sendMessage(message);
				}
				if (sender instanceof Entity entity && !entity.isValid()) {
					return;
				}
				sender.sendMessage(message);
			}
		}.runTask(Plugin.getInstance());
	}
}
