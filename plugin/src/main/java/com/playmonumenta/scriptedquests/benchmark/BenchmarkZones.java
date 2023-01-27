package com.playmonumenta.scriptedquests.benchmark;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class BenchmarkZones extends Benchmark {
	static final Random mRandom = new XoRoShiRo128PlusRandom();

	public static void register() {
		new CommandAPICommand("benchmark")
			.withPermission(CommandPermission.fromString("scriptedquests.benchmark"))
			.withArguments(new MultiLiteralArgument("ScriptedQuests"))
			.withArguments(new MultiLiteralArgument("Zones"))
			.withArguments(new MultiLiteralArgument("Random"))
			.executes((sender, args) -> {
				runBenchmarkRandom(sender);
				return 0;
			})
			.register();

		new CommandAPICommand("benchmark")
			.withPermission(CommandPermission.fromString("scriptedquests.benchmark"))
			.withArguments(new MultiLiteralArgument("ScriptedQuests"))
			.withArguments(new MultiLiteralArgument("Zones"))
			.withArguments(new MultiLiteralArgument("Random"))
			.withArguments(new LocationArgument("from"))
			.withArguments(new LocationArgument("to"))
			.executes((sender, args) -> {
				Location from = (Location) args[3];
				Location to = (Location) args[4];
				runBenchmarkRandom(sender, from, to);
				return 0;
			})
			.register();
	}

	public static void runBenchmarkRandom(CommandSender sender) {
		new BukkitRunnable() {
			@Override
			public void run() {
				benchmarkRandom(sender);
			}
		}.runTaskAsynchronously(Plugin.getInstance());
	}

	public static void runBenchmarkRandom(CommandSender sender, Location from, Location to) {
		new BukkitRunnable() {
			@Override
			public void run() {
				benchmarkRandom(sender, from, to);
			}
		}.runTaskAsynchronously(Plugin.getInstance());
	}

	private static void benchmarkRandom(CommandSender sender) {
		ZoneManager zoneManager = ZoneManager.getInstance();
		BoundingBox randomBoundingBox = zoneManager.getAllZoneBoundingBox();
		if (randomBoundingBox == null) {
			randomBoundingBox = new BoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		}
		randomBoundingBox.expand(0.05 * randomBoundingBox.getWidthX(),
			0.05 * randomBoundingBox.getHeight(),
			0.05 * randomBoundingBox.getWidthZ());

		benchmarkRandom(sender, zoneManager, randomBoundingBox);
	}

	private static void benchmarkRandom(CommandSender sender, Location from, Location to) {
		benchmarkRandom(sender, ZoneManager.getInstance(), BoundingBox.of(from, to));
	}

	private static void benchmarkRandom(CommandSender sender, ZoneManager zoneManager, BoundingBox randomBoundingBox) {
		double minX = randomBoundingBox.getMinX();
		double minY = randomBoundingBox.getMinY();
		double minZ = randomBoundingBox.getMinZ();
		double deltaX = randomBoundingBox.getWidthX();
		double deltaY = randomBoundingBox.getHeight();
		double deltaZ = randomBoundingBox.getWidthZ();

		Supplier<Vector> vectorSupplier = () -> new Vector(minX + deltaX * mRandom.nextDouble(),
			minY + deltaY * mRandom.nextDouble(),
			minZ + deltaZ * mRandom.nextDouble());

		benchmark(sender, "Benchmark: SQ Zone: Random", getZoneTestWrapper(zoneManager, vectorSupplier));
	}

	private static BooleanSupplier getZoneTestWrapper(ZoneManager zoneManager, Supplier<Vector> vectorSupplier) {
		return () -> zoneManager.getZoneFragment(vectorSupplier.get()) != null;
	}
}
