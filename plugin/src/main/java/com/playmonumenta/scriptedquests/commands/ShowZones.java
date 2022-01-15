package com.playmonumenta.scriptedquests.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneFragment;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ShowZones {
	static enum FragmentFace {
		X_MIN,
		Y_MIN,
		Z_MIN,
		X_MAX,
		Y_MAX,
		Z_MAX
	}

	static class ShownInfo extends BukkitRunnable {
		private static final XoRoShiRo128PlusRandom RANDOM = new XoRoShiRo128PlusRandom();
		private static final double DELTA_POS = 0.000001;

		Plugin mPlugin;
		UUID mPlayerUuid;
		String mLayerName = null;
		@Nullable String mPropertyName = null;

		public ShownInfo(Plugin plugin, UUID playerUuid, String layerName, @Nullable String propertyName) {
			mPlugin = plugin;
			mPlayerUuid = playerUuid;
			mLayerName = layerName;
			mPropertyName = propertyName;
		}

		public String layerName() {
			return mLayerName;
		}

		public void layerName(String layerName) {
			mLayerName = layerName;
		}

		public @Nullable String propertyName() {
			return mPropertyName;
		}

		public void propertyName(@Nullable String propertyName) {
			mPropertyName = propertyName;
		}

		private static final double randRange(double min, double max) {
			return (RANDOM.nextDouble() * (max - min)) + min;
		}

		@Override
		public void run() {
			@Nullable Player player = Bukkit.getPlayer(mPlayerUuid);
			if (player == null) {
				cancel();
				mShownInfo.remove(mPlayerUuid);
				return;
			}
			Vector loc = player.getLocation().toVector();

			for (double shownBlockDistance = 8.0; shownBlockDistance <= 32.0; shownBlockDistance *= 2.0) {
				double minX = loc.getX() - shownBlockDistance;
				double minY = loc.getY() - shownBlockDistance;
				double minZ = loc.getZ() - shownBlockDistance;
				double maxX = loc.getX() + shownBlockDistance;
				double maxY = loc.getY() + shownBlockDistance;
				double maxZ = loc.getZ() + shownBlockDistance;
				BoundingBox bbVisible = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

				double x = 0.0;
				double y = 0.0;
				double z = 0.0;

				// Simplest case, volumetric particle cloud
				for (int i = 2; i != 0; --i) {
					x = randRange(minX, maxX);
					y = randRange(minY, maxY);
					z = randRange(minZ, maxZ);

					@Nullable Zone zone = mPlugin.mZoneManager.getZone(new Vector(x, y, z), mLayerName);
					if (zone == null) {
						continue;
					}
					if (mPropertyName != null && !zone.hasProperty(mPropertyName)) {
						continue;
					}
					String zoneName = zone.getName();
					Color color = ZoneUtils.getBukkitColor(mLayerName, zoneName);
					double r = (1.0 + color.getRed()) / 256.0;
					double g = (1.0 + color.getGreen()) / 256.0;
					double b = (1.0 + color.getBlue()) / 256.0;
					player.spawnParticle(Particle.SPELL_MOB,
						                 x,
						                 y,
						                 z,
						                 0,
						                 r,
						                 g,
						                 b);
				}

				double fragMinXShown;
				double fragMinYShown;
				double fragMinZShown;
				double fragMaxXShown;
				double fragMaxYShown;
				double fragMaxZShown;
				double area = 0.0;
				Vector minCorner;
				Vector maxCorner;
				Double faceParticleCountDouble;
				int faceParticleCount;
				@Nullable Zone targetZone;
				@Nullable Zone testZone;
				Color color;

				for (ZoneFragment fragment : mPlugin.mZoneManager.getZoneFragments(bbVisible)) {
					targetZone = fragment.getParent(mLayerName);
					if (targetZone == null) {
						continue;
					}
					if (mPropertyName != null && !targetZone.hasProperty(mPropertyName)) {
						continue;
					}
					color = ZoneUtils.getBukkitColor(mLayerName, targetZone.getName());

					minCorner = fragment.minCorner();
					maxCorner = fragment.maxCornerExclusive();

					// Trickier, show faces of fragments *unless* they're next to another fragment of the same zone
					for (FragmentFace face : FragmentFace.values()) {
						fragMinXShown = Double.max(minX, minCorner.getX());
						fragMinYShown = Double.max(minY, minCorner.getY());
						fragMinZShown = Double.max(minZ, minCorner.getZ());
						fragMaxXShown = Double.min(maxX, maxCorner.getX());
						fragMaxYShown = Double.min(maxY, maxCorner.getY());
						fragMaxZShown = Double.min(maxZ, maxCorner.getZ());
						switch (face) {
						case X_MIN:
						case X_MAX:
							if (face == FragmentFace.X_MIN) {
								fragMaxXShown = fragMinXShown;
							} else {
								fragMinXShown = fragMaxXShown;
							}
							area = (fragMaxYShown - fragMinYShown) * (fragMaxZShown - fragMinZShown);
							break;
						case Y_MIN:
						case Y_MAX:
							if (face == FragmentFace.Y_MIN) {
								fragMaxYShown = fragMinYShown;
							} else {
								fragMinYShown = fragMaxYShown;
							}
							area = (fragMaxXShown - fragMinXShown) * (fragMaxZShown - fragMinZShown);
							break;
						default:
							if (face == FragmentFace.Z_MIN) {
								fragMaxZShown = fragMinZShown;
							} else {
								fragMinZShown = fragMaxZShown;
							}
							area = (fragMaxXShown - fragMinXShown) * (fragMaxYShown - fragMinYShown);
						}

						faceParticleCountDouble = 1.0 + 0.01 * area;
						faceParticleCount = faceParticleCountDouble.intValue();
						for (int i = faceParticleCount; i != 0; --i) {
							x = randRange(fragMinXShown, fragMaxXShown);
							y = randRange(fragMinYShown, fragMaxYShown);
							z = randRange(fragMinZShown, fragMaxZShown);

							switch (face) {
							case X_MIN:
								testZone = mPlugin.mZoneManager.getZone(new Vector(x - DELTA_POS, y, z), mLayerName);
								break;
							case Y_MIN:
								testZone = mPlugin.mZoneManager.getZone(new Vector(x, y - DELTA_POS, z), mLayerName);
								break;
							case Z_MIN:
								testZone = mPlugin.mZoneManager.getZone(new Vector(x, y, z - DELTA_POS), mLayerName);
								break;
							default:
								testZone = mPlugin.mZoneManager.getZone(new Vector(x, y, z), mLayerName);
							}
							// Intentionally testing if these are the same object
							if (targetZone == testZone) {
								continue;
							}

							Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
							player.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0.0, 0.0, 0.0, dustOptions);
						}
					}
				}
			}
		}
	}

	protected static Map<UUID, ShownInfo> mShownInfo = new HashMap<>();

	public static void register(Plugin plugin) {
		new CommandAPICommand("showzones")
			.withPermission(CommandPermission.fromString("scriptedquests.showzones"))
			.withArguments(new MultiLiteralArgument("hide"))
			.executes((sender, args) -> {
				return hide(sender);
			})
			.register();

		new CommandAPICommand("showzones")
			.withPermission(CommandPermission.fromString("scriptedquests.showzones"))
			.withArguments(new MultiLiteralArgument("show"))
			.withArguments(new TextArgument("layer").replaceSuggestions(info -> {
				return plugin.mZoneManager.getLayerNameSuggestions();
			}))
			.executes((sender, args) -> {
				String layerName = (String) args[1];
				return show(plugin, sender, layerName, null);
			})
			.register();

		new CommandAPICommand("showzones")
			.withPermission(CommandPermission.fromString("scriptedquests.showzones"))
			.withArguments(new MultiLiteralArgument("show"))
			.withArguments(new TextArgument("layer").replaceSuggestions(info -> {
				return plugin.mZoneManager.getLayerNameSuggestions();
			}))
			.withArguments(new TextArgument("property").replaceSuggestions(info -> {
				return plugin.mZoneManager.getLoadedPropertySuggestions((String) info.previousArgs()[1]);
			}))
			.executes((sender, args) -> {
				String layerName = (String) args[1];
				String propertyName = (String) args[2];
				return show(plugin, sender, layerName, propertyName);
			})
			.register();
	}

	private static int hide(CommandSender sender) throws WrapperCommandSyntaxException {
		if (!(sender instanceof Player)) {
			CommandAPI.fail("This command can only be run as a player.");
		}
		Player player = (Player)sender;
		UUID playerUuid = player.getUniqueId();
		@Nullable ShownInfo shownInfo = mShownInfo.get(playerUuid);
		if (shownInfo == null) {
			player.sendMessage(Component.text("Zones already hidden.", NamedTextColor.AQUA));
			return 1;
		}
		shownInfo.cancel();
		mShownInfo.remove(playerUuid);
		player.sendMessage(Component.text("Zones hidden.", NamedTextColor.AQUA));
		return 1;
	}

	private static int show(Plugin plugin, CommandSender sender, String layerName, @Nullable String propertyName) throws WrapperCommandSyntaxException {
		if (!(sender instanceof Player)) {
			CommandAPI.fail("This command can only be run as a player.");
		}
		Player player = (Player)sender;

		BoundingBox bbVisible = BoundingBox.of(player.getLocation(), 32.0, 32.0, 32.0);

		UUID playerUuid = player.getUniqueId();
		@Nullable ShownInfo shownInfo = mShownInfo.get(playerUuid);
		if (shownInfo == null) {
			shownInfo = new ShownInfo(plugin, playerUuid, layerName, propertyName);
			mShownInfo.put(playerUuid, shownInfo);
			shownInfo.runTaskTimer(plugin, 0, 1);
		} else {
			shownInfo.layerName(layerName);
			shownInfo.propertyName(propertyName);
		}
		if (propertyName == null) {
			player.sendMessage(Component.text("Showing all " + layerName + " zones.", NamedTextColor.AQUA));
		} else {
			player.sendMessage(Component.text("Showing all " + layerName + " zones with property " + propertyName + ".", NamedTextColor.AQUA));
		}
		return 1;
	}
}
