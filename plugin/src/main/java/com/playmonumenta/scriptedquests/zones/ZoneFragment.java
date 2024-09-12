package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.scriptedquests.utils.VectorUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Axis;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

/*
 * A fragment of a zone; this is used to find zones quickly, but not hold their properties.
 * Instead, each fragment points to its parent, a zone with properties.
 * Each zone also keeps track of its fragments.
 */
public class ZoneFragment extends ZoneBase {
	// For each namespace, list of zones by priority without duplicate world regex
	private final Map<String, List<Zone>> mParents = new HashMap<>();
	// For each namespace, list all zones by priority, including those that do not have priority for their world
	private final Map<String, List<Zone>> mParentsAndEclipsed = new HashMap<>();
	private boolean mValid;

	protected ZoneFragment(ZoneFragment other) {
		super(other);

		for (Map.Entry<String, List<Zone>> entry : other.mParents.entrySet()) {
			mParents.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}

		for (Map.Entry<String, List<Zone>> entry : other.mParentsAndEclipsed.entrySet()) {
			mParentsAndEclipsed.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}

		mValid = other.mValid;
	}

	protected ZoneFragment(Zone other) {
		super(other);

		List<Zone> zones = new ArrayList<>();
		zones.add(other);

		mParents.put(other.getNamespaceName(), zones);

		mParentsAndEclipsed.put(other.getNamespaceName(), zones);

		mValid = true;
	}

	/*
	 * Returns (lower_zone, upper_zone) for this split along some axis.
	 *
	 * Either zone may have a size of 0, and should be ignored.
	 */
	private ZoneFragment[] splitAxis(Vector pos, Axis axis) {
		ZoneFragment[] result = new ZoneFragment[2];

		ZoneFragment lower = new ZoneFragment(this);
		ZoneFragment upper = new ZoneFragment(this);

		Vector lowerMax = lower.maxCornerExclusive();
		Vector upperMin = upper.minCorner();

		switch (axis) {
			case X -> {
				lowerMax.setX(pos.getX());
				upperMin.setX(pos.getX());
			}
			case Z -> {
				lowerMax.setZ(pos.getZ());
				upperMin.setZ(pos.getZ());
			}
			default -> {
				lowerMax.setY(pos.getY());
				upperMin.setY(pos.getY());
			}
		}

		lower.maxCornerExclusive(lowerMax);
		upper.minCorner(upperMin);

		result[0] = lower;
		result[1] = upper;

		return result;
	}

	/*
	 * Returns a list of fragments of this zone, split by an overlapping zone.
	 * Optionally register a new parent and return the center zone.
	 *
	 * When registering a new parent, only do so for one of the parent zones.
	 * The other parent zone should have the overlap removed as normal to avoid
	 * overlapping fragments.
	 */
	protected List<ZoneFragment> splitByOverlap(ZoneBase overlap, Zone newParent, boolean includeOverlap) {
		ZoneFragment centerZone = new ZoneFragment(this);

		Vector otherMin = overlap.minCorner();
		Vector otherMax = overlap.maxCornerExclusive();

		ZoneFragment[] tempSplitResult;
		List<ZoneFragment> result = new ArrayList<>();

		for (Axis axis : Axis.values()) {
			@Nullable ZoneFragment lower;
			@Nullable ZoneFragment upper;

			// Add zones split from center, but not the center (overlap) itself
			tempSplitResult = centerZone.splitAxis(otherMin, axis);
			lower = tempSplitResult[0];
			centerZone = tempSplitResult[1];

			tempSplitResult = centerZone.splitAxis(otherMax, axis);
			centerZone = tempSplitResult[0];
			upper = tempSplitResult[1];

			if (lower != null && lower.isValid()) {
				result.add(lower);
			}
			if (upper != null && upper.isValid()) {
				result.add(upper);
			}
		}

		// If registering a new parent, it may be added now that the center zone is the size of the overlap.
		if (includeOverlap) {
			String newParentNamespaceName = newParent.getNamespaceName();
			String newParentWorldRegex = newParent.getWorldRegex();

			// If the center fragment is kept, the original parents take priority over the new parent
			// Failing to do this would mean the fragment takes priority from the wrong zone
			boolean addParent = true;
			List<Zone> parentZones
				= centerZone.mParents.computeIfAbsent(newParentNamespaceName, k -> new ArrayList<>());
			for (Zone parentZone : parentZones) {
				if (parentZone.getWorldRegex().equals(newParentWorldRegex)) {
					addParent = false;
					break;
				}
			}
			if (addParent) {
				parentZones.add(newParent);
			}

			// Track the new parent zone of the center fragment, even if it's eclipsed.
			centerZone.mParentsAndEclipsed
				.computeIfAbsent(newParentNamespaceName, k -> new ArrayList<>())
				.add(newParent);

			result.add(centerZone);
		}

		return result;
	}

	public Map<String, List<Zone>> getParents() {
		Map<String, List<Zone>> result = new HashMap<>();

		for (Map.Entry<String, List<Zone>> entry : mParents.entrySet()) {
			result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}

		return result;
	}

	public Map<String, Zone> getParents(World world) {
		return getParents(world, null);
	}

	public Map<String, Zone> getParents(World world, @Nullable CommandSender sender) {
		Map<String, Zone> result = new HashMap<>();

		for (Map.Entry<String, List<Zone>> entry : mParents.entrySet()) {
			String namespaceName = entry.getKey();
			List<Zone> zones = entry.getValue();
			if (sender != null) {
				sender.sendMessage(Component.text(
					"- This fragment has " + zones.size() + " possible matches", NamedTextColor.GREEN));
			}

			for (Zone zone : zones) {
				if (zone.matchesWorld(world)) {
					if (sender != null) {
						sender.sendMessage(Component.text(
							"- `" + zone.getName()
								+ "` matches " + zone.getWorldRegex() + "!",
							NamedTextColor.GREEN));
					}
					result.put(namespaceName, zone);
					break;
				} else if (sender != null) {
					sender.sendMessage(Component.text(
						"- `" + zone.getName()
							+ "` does not match " + zone.getWorldRegex() + "!",
						NamedTextColor.RED));
				}

			}
		}

		return result;
	}

	public @Nullable Zone getParent(World world, String namespaceName) {
		List<Zone> zones = mParents.get(namespaceName);
		if (zones == null) {
			return null;
		}

		for (Zone zone : zones) {
			if (zone.matchesWorld(world)) {
				return zone;
			}
		}

		return null;
	}

	public Map<String, List<Zone>> getParentsAndEclipsed() {
		Map<String, List<Zone>> result = new HashMap<>();
		for (Map.Entry<String, List<Zone>> entry : mParentsAndEclipsed.entrySet()) {
			String namespaceName = entry.getKey();
			List<Zone> zones = entry.getValue();

			result.put(namespaceName, new ArrayList<>(zones));
		}
		return result;
	}

	public Map<String, List<Zone>> getParentsAndEclipsed(World world) {
		Map<String, List<Zone>> result = new HashMap<>();
		for (Map.Entry<String, List<Zone>> entry : mParentsAndEclipsed.entrySet()) {
			String namespaceName = entry.getKey();
			List<Zone> zones = new ArrayList<>();

			for (Zone zone : entry.getValue()) {
				if (zone.matchesWorld(world)) {
					zones.add(zone);
				}
			}

			result.put(namespaceName, zones);
		}
		return result;
	}

	public List<Zone> getParentAndEclipsed(String namespaceName) {
		@Nullable List<Zone> zones = mParentsAndEclipsed.get(namespaceName);
		List<Zone> result = new ArrayList<>();
		if (zones != null) {
			result.addAll(zones);
		}
		return result;
	}

	public List<Zone> getParentAndEclipsed(World world, String namespaceName) {
		@Nullable List<Zone> zones = mParentsAndEclipsed.get(namespaceName);
		List<Zone> result = new ArrayList<>();
		if (zones != null) {
			for (Zone zone : zones) {
				if (zone.matchesWorld(world)) {
					result.add(zone);
				}
			}
		}
		return result;
	}

	public boolean hasProperty(World world, String namespaceName, String propertyName) {
		@Nullable Zone zone = getParent(world, namespaceName);
		return zone != null && zone.hasProperty(propertyName);
	}

	/*
	 * Force all future tests for locations within this zone to return false.
	 *
	 * This means any code tracking previous fragments/zones will be forced to check again when reloading zones.
	 */
	protected void invalidate() {
		mValid = false;
	}

	@Override
	public boolean within(Vector loc) {
		if (loc == null) {
			return false;
		}

		if (!mValid) {
			return false;
		}

		for (Axis axis : Axis.values()) {
			double test = VectorUtils.vectorAxis(loc, axis);
			double min = VectorUtils.vectorAxis(minCorner(), axis);
			double max = VectorUtils.vectorAxis(maxCornerExclusive(), axis);
			if (test < min || test >= max) {
				return false;
			}
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (!(o instanceof ZoneFragment other)) {
			return false;
		}
		return (super.equals(other) &&
		        mParents.equals(other.mParents) &&
		        mParentsAndEclipsed.equals(other.mParentsAndEclipsed));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31*result + mParents.hashCode();
		result = 31*result + mParentsAndEclipsed.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return ("(ZoneFragment(" + mParentsAndEclipsed.toString() + ") from "
		        + minCorner().toString() + " to "
		        + maxCorner().toString() + ")");
	}
}
