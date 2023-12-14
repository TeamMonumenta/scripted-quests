package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ShowZones {
	enum FragmentFace {
		X_MIN,
		Y_MIN,
		Z_MIN,
		X_MAX,
		Y_MAX,
		Z_MAX
	}

	enum FragmentEdge {
		X_YMIN_ZMIN,
		X_YMIN_ZMAX,
		X_YMAX_ZMIN,
		X_YMAX_ZMAX,
		Y_XMIN_ZMIN,
		Y_XMIN_ZMAX,
		Y_XMAX_ZMIN,
		Y_XMAX_ZMAX,
		Z_XMIN_YMIN,
		Z_XMIN_YMAX,
		Z_XMAX_YMIN,
		Z_XMAX_YMAX
	}

	static class ShownInfo extends BukkitRunnable {
		private static final XoRoShiRo128PlusRandom RANDOM = new XoRoShiRo128PlusRandom();
		private static final double DELTA_POS = 0.000001;
		private static final double RENDER_DISTANCE = 16.0; // Really this goes up to 32 blocks, but that's hard to see.

		Plugin mPlugin;
		UUID mPlayerUuid;
		String mNamespaceName;
		@Nullable String mPropertyName;

		public ShownInfo(Plugin plugin, UUID playerUuid, String namespaceName, @Nullable String propertyName) {
			mPlugin = plugin;
			mPlayerUuid = playerUuid;
			mNamespaceName = namespaceName;
			mPropertyName = propertyName;
		}

		public String namespaceName() {
			return mNamespaceName;
		}

		public void namespaceName(String namespaceName) {
			mNamespaceName = namespaceName;
		}

		public @Nullable String propertyName() {
			return mPropertyName;
		}

		public void propertyName(@Nullable String propertyName) {
			mPropertyName = propertyName;
		}

		private static double randRange(double min, double max) {
			return (RANDOM.nextDouble() * (max - min)) + min;
		}

		@Override
		@SuppressWarnings("ReferenceEquality")
		public void run() {
			@Nullable Player player = Bukkit.getPlayer(mPlayerUuid);
			if (player == null) {
				cancel();
				mShownInfo.remove(mPlayerUuid);
				return;
			}
			Vector loc = player.getLocation().toVector();

			// Initialized here to avoid warnings about them being uninitialized
			Vector testPointOffset1 = new Vector(0.0, 0.0, 0.0);
			Vector testPointOffset2 = new Vector(0.0, 0.0, 0.0);

			for (double shownBlockDistance = 8.0; shownBlockDistance <= RENDER_DISTANCE; shownBlockDistance *= 2.0) {
				double minX = loc.getX() - shownBlockDistance;
				double minY = loc.getY() - shownBlockDistance;
				double minZ = loc.getZ() - shownBlockDistance;
				double maxX = loc.getX() + shownBlockDistance;
				double maxY = loc.getY() + shownBlockDistance;
				double maxZ = loc.getZ() + shownBlockDistance;
				BoundingBox bbVisible = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

				double x;
				double y;
				double z;

				// Simplest case, volumetric particle cloud
				for (int i = 2; i != 0; --i) {
					x = randRange(minX, maxX);
					y = randRange(minY, maxY);
					z = randRange(minZ, maxZ);

					@Nullable Zone zone = ZoneManager.getInstance().getZoneLegacy(new Vector(x, y, z), mNamespaceName);
					if (zone == null) {
						continue;
					}
					if (mPropertyName != null && !zone.hasProperty(mPropertyName)) {
						continue;
					}
					String zoneName = zone.getName();
					Color color = ZoneUtils.getBukkitColor(mNamespaceName, zoneName);
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

				/*
				 * The number of particles sent to each client can vary wildly;
				 * estimate the space those particles are spread over to make
				 * the number of particles sent more consistent.
				 */
				Set<ZoneFragment> fragments = ZoneManager.getInstance().getZoneFragments(bbVisible);
				Iterator<ZoneFragment> it = fragments.iterator();
				double maxPossibleTotalArea = 0.0;
				double maxPossibleTotalLength = 0.0;
				while (it.hasNext()) {
					ZoneFragment fragment = it.next();
					@Nullable Zone targetZone = fragment.getParentLegacy(mNamespaceName);
					if (targetZone == null) {
						it.remove();
						continue;
					}
					if (mPropertyName != null && !targetZone.hasProperty(mPropertyName)) {
						it.remove();
						continue;
					}

					Vector minCorner = fragment.minCorner();
					Vector maxCorner = fragment.maxCornerExclusive();

					double fragMinXShown = Double.max(minX, minCorner.getX());
					double fragMinYShown = Double.max(minY, minCorner.getY());
					double fragMinZShown = Double.max(minZ, minCorner.getZ());
					double fragMaxXShown = Double.min(maxX, maxCorner.getX());
					double fragMaxYShown = Double.min(maxY, maxCorner.getY());
					double fragMaxZShown = Double.min(maxZ, maxCorner.getZ());
					double fragSizeXShown = fragMaxXShown - fragMinXShown;
					double fragSizeYShown = fragMaxYShown - fragMinYShown;
					double fragSizeZShown = fragMaxZShown - fragMinZShown;

					// Save *2 part for outside the loop
					maxPossibleTotalArea += ((fragSizeXShown * fragSizeYShown)
					                       + (fragSizeXShown * fragSizeZShown)
					                       + (fragSizeYShown * fragSizeZShown));
					// Save *4 part for outside the loop
					maxPossibleTotalLength += fragSizeXShown + fragSizeYShown + fragSizeZShown;
				}
				maxPossibleTotalArea *= 2;
				maxPossibleTotalLength *= 4;

				NavigableMap<Double, FragmentEdge> edgeWeights = new TreeMap<>();
				for (ZoneFragment fragment : fragments) {
					@Nullable Zone targetZone = fragment.getParentLegacy(mNamespaceName);
					if (targetZone == null) {
						continue;
					}
					if (mPropertyName != null && !targetZone.hasProperty(mPropertyName)) {
						continue;
					}
					Color color = ZoneUtils.getBukkitColor(mNamespaceName, targetZone.getName());

					Vector minCorner = fragment.minCorner();
					Vector maxCorner = fragment.maxCornerExclusive();
					@Nullable Zone testZone;

					double fragMinXShown = Double.max(minX, minCorner.getX());
					double fragMinYShown = Double.max(minY, minCorner.getY());
					double fragMinZShown = Double.max(minZ, minCorner.getZ());
					double fragMaxXShown = Double.min(maxX, maxCorner.getX());
					double fragMaxYShown = Double.min(maxY, maxCorner.getY());
					double fragMaxZShown = Double.min(maxZ, maxCorner.getZ());
					double fragSizeXShown = fragMaxXShown - fragMinXShown;
					double fragSizeYShown = fragMaxYShown - fragMinYShown;
					double fragSizeZShown = fragMaxZShown - fragMinZShown;

					double partMinXShown;
					double partMinYShown;
					double partMinZShown;
					double partMaxXShown;
					double partMaxYShown;
					double partMaxZShown;

					// Trickier, show faces of fragments *unless* they're next to another fragment of the same zone
					Particle.DustOptions dustOptions = new Particle.DustOptions(color, 0.75f);
					for (FragmentFace face : FragmentFace.values()) {
						partMinXShown = fragMinXShown;
						partMinYShown = fragMinYShown;
						partMinZShown = fragMinZShown;
						partMaxXShown = fragMaxXShown;
						partMaxYShown = fragMaxYShown;
						partMaxZShown = fragMaxZShown;

						double area;
						switch (face) {
							case X_MIN, X_MAX -> {
								if (face == FragmentFace.X_MIN) {
									partMaxXShown = fragMinXShown;
								} else {
									partMinXShown = fragMaxXShown;
								}
								area = fragSizeYShown * fragSizeZShown;
							}
							case Y_MIN, Y_MAX -> {
								if (face == FragmentFace.Y_MIN) {
									partMaxYShown = fragMinYShown;
								} else {
									partMinYShown = fragMaxYShown;
								}
								area = fragSizeXShown * fragSizeZShown;
							}
							default -> {
								if (face == FragmentFace.Z_MIN) {
									partMaxZShown = fragMinZShown;
								} else {
									partMinZShown = fragMaxZShown;
								}
								area = fragSizeXShown * fragSizeYShown;
							}
						}

						double faceParticleCount = 1.0 + 25.0 * area / maxPossibleTotalArea;
						for (int i = (int) faceParticleCount; i != 0; --i) {
							x = randRange(partMinXShown, partMaxXShown);
							y = randRange(partMinYShown, partMaxYShown);
							z = randRange(partMinZShown, partMaxZShown);

							testZone = switch (face) {
								case X_MIN -> ZoneManager.getInstance().getZoneLegacy(new Vector(x - DELTA_POS, y, z), mNamespaceName);
								case Y_MIN -> ZoneManager.getInstance().getZoneLegacy(new Vector(x, y - DELTA_POS, z), mNamespaceName);
								case Z_MIN -> ZoneManager.getInstance().getZoneLegacy(new Vector(x, y, z - DELTA_POS), mNamespaceName);
								default -> ZoneManager.getInstance().getZoneLegacy(new Vector(x, y, z), mNamespaceName);
							};
							// Intentionally testing if these are the same object
							if (targetZone == testZone) {
								continue;
							}

							player.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0.0, 0.0, 0.0, dustOptions);
						}
					}

					// Trickiest, show edges
					//dustOptions = new Particle.DustOptions(color, 0.25f);
					edgeWeights.clear();
					double maxWeight = 0.0;
					for (FragmentEdge edge : FragmentEdge.values()) {
						switch (edge) {
							case X_YMIN_ZMIN, X_YMIN_ZMAX, X_YMAX_ZMIN, X_YMAX_ZMAX ->
								maxWeight += DELTA_POS + fragSizeXShown;
							case Y_XMIN_ZMIN, Y_XMIN_ZMAX, Y_XMAX_ZMIN, Y_XMAX_ZMAX ->
								maxWeight += DELTA_POS + fragSizeYShown;
							default -> maxWeight += DELTA_POS + fragSizeZShown;
						}
						edgeWeights.put(maxWeight, edge);
					}
					double edgeParticleCount = 1.0 + 50.0 * maxWeight / maxPossibleTotalLength;
					for (int i = (int) edgeParticleCount; i != 0; --i) {
						@Nullable Entry<Double, FragmentEdge> edgeEntry = edgeWeights.higherEntry(maxWeight * RANDOM.nextDouble());
						if (edgeEntry == null) {
							continue;
						}
						FragmentEdge edge = edgeEntry.getValue();

						switch (edge) {
							case X_YMIN_ZMIN, X_YMIN_ZMAX, X_YMAX_ZMIN, X_YMAX_ZMAX -> {
								x = randRange(fragMinXShown, fragMaxXShown);
								testPointOffset1 = new Vector(0.0, DELTA_POS, 0.0);
								testPointOffset2 = new Vector(0.0, 0.0, DELTA_POS);
							}
							case Y_XMIN_ZMIN, Y_XMIN_ZMAX, Z_XMIN_YMIN, Z_XMIN_YMAX -> x = fragMinXShown;
							default -> x = fragMaxXShown;
						}

						switch (edge) {
							case Y_XMIN_ZMIN, Y_XMIN_ZMAX, Y_XMAX_ZMIN, Y_XMAX_ZMAX -> {
								y = randRange(fragMinYShown, fragMaxYShown);
								testPointOffset1 = new Vector(DELTA_POS, 0.0, 0.0);
								testPointOffset2 = new Vector(0.0, 0.0, DELTA_POS);
							}
							case X_YMIN_ZMIN, X_YMIN_ZMAX, Z_XMIN_YMIN, Z_XMAX_YMIN -> y = fragMinYShown;
							default -> y = fragMaxYShown;
						}

						switch (edge) {
							case Z_XMIN_YMIN, Z_XMIN_YMAX, Z_XMAX_YMIN, Z_XMAX_YMAX -> {
								z = randRange(fragMinZShown, fragMaxZShown);
								testPointOffset1 = new Vector(DELTA_POS, 0.0, 0.0);
								testPointOffset2 = new Vector(0.0, DELTA_POS, 0.0);
							}
							case X_YMIN_ZMIN, X_YMAX_ZMIN, Y_XMIN_ZMIN, Y_XMAX_ZMIN -> z = fragMinZShown;
							default -> z = fragMaxZShown;
						}

						/*
						 * Determining if a point of an edge is visible or not is...complicated.
						 *
						 * Fragments are used to mark rectangular prisms where zone priority
						 * is consistent, even across namespaces. Zones can be split into many
						 * fragments, with some fragments combining to form seamless
						 * planes or internal volumes.
						 *
						 * The trick becomes how to tell if an edge is actually an edge,
						 * if it's a seamless joint in a plane, or if it's in an internal
						 * volume of a zone with fragments on each side. There's further
						 * complications in that there may be internal corners, or an edge
						 * may be seamless in some parts but not in others. Further still,
						 * the positive axes of a fragment are exclusive.
						 *
						 * The best way I can come up with to deal with this is to test
						 * a 3x3 grid of points, mapping out if they're the same zone
						 * or not, and testing if the test points indicate a real seam.
						 */
						Vector particlePosition = new Vector(x, y, z);
						int[] testAxis1 = {0, 0, 0};
						int[] testAxis2 = {0, 0, 0};
						for (int offset1 = -1; offset1 <= 1; ++offset1) {
							Vector testPoint1 = particlePosition.clone().add(testPointOffset1.clone().multiply(offset1));
							for (int offset2 = -1; offset2 <= 1; ++offset2) {
								testZone = ZoneManager.getInstance().getZoneLegacy(testPoint1.clone().add(testPointOffset2.clone().multiply(offset2)), mNamespaceName);
								// Intentionally testing if these are not the same object
								if (targetZone != testZone) {
									testAxis1[1 + offset2] |= 1 << (1 + offset1);
									testAxis2[1 + offset1] |= 1 << (1 + offset2);
								}
							}
						}
						boolean showParticle = false;
						if (testAxis1[0] == testAxis1[1] && testAxis1[1] == testAxis1[2]) {
							for (int testValue : testAxis2) {
								if (testValue != 0 && testValue != 7) {
									showParticle = true;
									break;
								}
							}
						} else if (testAxis2[0] == testAxis2[1] && testAxis2[1] == testAxis2[2]) {
							for (int testValue : testAxis1) {
								if (testValue != 0 && testValue != 7) {
									showParticle = true;
									break;
								}
							}
						} else {
							showParticle = true;
						}
						if (showParticle) {
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
			.withArguments(new TextArgument("namespace").replaceSuggestions(ZoneManager.getNamespaceArgumentSuggestions()))
			.executes((sender, args) -> {
				String namespaceName = (String) args[1];
				return show(plugin, sender, namespaceName, null);
			})
			.register();

		new CommandAPICommand("showzones")
			.withPermission(CommandPermission.fromString("scriptedquests.showzones"))
			.withArguments(new MultiLiteralArgument("show"))
			.withArguments(new TextArgument("namespace").replaceSuggestions(ZoneManager.getNamespaceArgumentSuggestions()))
			.withArguments(new TextArgument("property").replaceSuggestions(ZoneManager.getLoadedPropertyArgumentSuggestions(1)))
			.executes((sender, args) -> {
				String namespaceName = (String) args[1];
				String propertyName = (String) args[2];
				return show(plugin, sender, namespaceName, propertyName);
			})
			.register();
	}

	private static int hide(CommandSender sender) throws WrapperCommandSyntaxException {
		CommandSender callee = sender;
		if (sender instanceof ProxiedCommandSender proxiedCommandSender) {
			callee = proxiedCommandSender.getCallee();
		}
		if (!(callee instanceof Player player)) {
			throw CommandAPI.failWithString("This command can only be run as a player.");
		} else {
			UUID playerUuid = player.getUniqueId();
			@Nullable ShownInfo shownInfo = mShownInfo.get(playerUuid);
			if (shownInfo == null) {
				sender.sendMessage(Component.text("Zones already hidden.", NamedTextColor.AQUA));
				return 1;
			}
			shownInfo.cancel();
			mShownInfo.remove(playerUuid);
			sender.sendMessage(Component.text("Zones hidden.", NamedTextColor.AQUA));
			return 1;
		}
	}

	private static int show(Plugin plugin, CommandSender sender, String namespaceName, @Nullable String propertyName) throws WrapperCommandSyntaxException {
		CommandSender callee = sender;
		if (sender instanceof ProxiedCommandSender proxiedCommandSender) {
			callee = proxiedCommandSender.getCallee();
		}
		if (!(callee instanceof Player player)) {
			throw CommandAPI.failWithString("This command can only be run as a player.");
		} else {
			UUID playerUuid = player.getUniqueId();
			@Nullable ShownInfo shownInfo = mShownInfo.get(playerUuid);
			if (shownInfo == null) {
				shownInfo = new ShownInfo(plugin, playerUuid, namespaceName, propertyName);
				mShownInfo.put(playerUuid, shownInfo);
				shownInfo.runTaskTimer(plugin, 0, 1);
			} else {
				shownInfo.namespaceName(namespaceName);
				shownInfo.propertyName(propertyName);
			}
			if (propertyName == null) {
				sender.sendMessage(Component.text("Showing all " + namespaceName + " zones.", NamedTextColor.AQUA));
			} else {
				sender.sendMessage(Component.text("Showing all " + namespaceName + " zones with property " + propertyName + ".", NamedTextColor.AQUA));
			}
			return 1;
		}
	}
}
