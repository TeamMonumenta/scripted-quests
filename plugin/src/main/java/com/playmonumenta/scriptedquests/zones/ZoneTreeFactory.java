package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.util.Vector;

public class ZoneTreeFactory {
	// Fragments of a particular zone
	private static class ZoneFragments {
		private final Zone mZone;
		private List<ZoneFragment> mFragments;

		protected Zone getZone() {
			return mZone;
		}

		protected ZoneFragments(Zone from) {
			mZone = from;
			mFragments = List.of(new ZoneFragment(from));
		}

		/*
		 * Split all fragments of this zone by an overlapping zone,
		 * marking otherZone as the parent of the exact overlap fragment if
		 * it exists. Otherwise, the exact overlap fragment is discarded.
		 */
		protected void splitByOverlap(ZoneBase overlap, ZoneFragments otherZone, boolean includeOther) {
			List<ZoneFragment> newFragments = new ArrayList<>();
			for (ZoneFragment fragment : mFragments) {
				@Nullable ZoneBase subOverlap = fragment.overlappingZone(overlap);

				if (subOverlap == null) {
					newFragments.add(fragment);
					continue;
				}

				newFragments.addAll(fragment.splitByOverlap(subOverlap, otherZone.mZone, includeOther));
				fragment.invalidate();
			}
			mFragments = newFragments;
		}

		public List<ZoneFragment> getZoneFragments() {
			return new ArrayList<>(mFragments);
		}
	}

	private static class NamespaceFragments {
		private final Audience mAudience;
		private final List<ZoneFragments> mZones = new ArrayList<>();

		protected NamespaceFragments(Audience audience, ZoneNamespace namespace) {
			mAudience = audience;
			for (Zone zone : namespace.getZones()) {
				mZones.add(new ZoneFragments(zone));
			}

			// Split the zones into non-overlapping fragments
			handleOverlaps();

			// Check for total eclipses
			totalEclipseCheck();
		}

		protected List<ZoneFragments> getZones() {
			return mZones;
		}

		private void handleOverlaps() {
			int lastElement = mZones.size() - 1;
			for (int i = 0; i < lastElement; i++) {
				ZoneFragments outer = mZones.get(i);
				for (ZoneFragments inner : mZones.subList(i + 1, mZones.size())) {
					@Nullable ZoneBase overlap = outer.mZone.overlappingZone(inner.mZone);
					if (overlap == null) {
						continue;
					}
					inner.splitByOverlap(overlap, outer, false);
					outer.splitByOverlap(overlap, inner, true);
				}
			}
		}

		private void totalEclipseCheck() {
			// Zones that are not totally eclipsed and can be ignored
			Set<Zone> zonesInEffect = new HashSet<>();
			// Maps a zone to the zones partially eclipsing it (entries removed if they're in effect)
			Map<Zone, Set<String>> zoneTotalEclipses = new HashMap<>();

			// Figure out which zones are in effect, and which are eclipsed by which
			for (ZoneFragments zoneFragments : mZones) {
				for (ZoneFragment zoneFragment : zoneFragments.mFragments) {
					for (Map.Entry<String, List<Zone>> fragmentZoneEntry : zoneFragment.getParentsAndEclipsed().entrySet()) {
						// {world: zone}
						Map<String, Zone> activeZoneMap = new HashMap<>();

						for (Zone zone : fragmentZoneEntry.getValue()) {
							Zone activeZone = activeZoneMap.putIfAbsent(zone.getWorldRegex(), zone);
							if (activeZone == null) {
								// zone is active!
								zonesInEffect.add(zone);
								zoneTotalEclipses.remove(zone);
								continue;
							}

							// Ignore zones that are in effect in other areas
							if (zonesInEffect.contains(zone)) {
								continue;
							}

							// Zone is not in effect, mark it as eclipsed by activeZone
							zoneTotalEclipses
								.computeIfAbsent(zone, k -> new TreeSet<>())
								.add(activeZone.getName());
						}
					}
				}
			}

			// Tell eclipsed zones
			for (Map.Entry<Zone, Set<String>> totalEclipseEntry : zoneTotalEclipses.entrySet()) {
				String eclipsed = totalEclipseEntry.getKey().getName();
				Component eclipsingComponent = getEclipsingComponent(totalEclipseEntry);

				mAudience.sendMessage(Component.text("Total eclipse of zone ", NamedTextColor.RED)
					.append(Component.text(eclipsed)
						.decorate(TextDecoration.BOLD))
					.append(Component.text(" by "))
					.append(eclipsingComponent));
			}
		}

		private static Component getEclipsingComponent(Map.Entry<Zone, Set<String>> totalEclipseEntry) {
			List<String> eclipsingNames = new ArrayList<>(totalEclipseEntry.getValue());

			Component eclipsingComponent;
			if (eclipsingNames.isEmpty()) {
				eclipsingComponent = Component.text("...nothing? This shouldn't appear...")
					.decorate(TextDecoration.BOLD);
			} else if (eclipsingNames.size() == 1) {
				eclipsingComponent = Component.text(eclipsingNames.get(0))
					.decorate(TextDecoration.BOLD);
			} else if (eclipsingNames.size() == 2) {
				eclipsingComponent = Component.empty()
					.append(Component.text(eclipsingNames.get(0))
						.decorate(TextDecoration.BOLD))
					.append(Component.text(" and "))
					.append(Component.text(eclipsingNames.get(1))
						.decorate(TextDecoration.BOLD));
			} else {
				eclipsingComponent = Component.empty();
				int remaining = eclipsingNames.size();
				for (String eclipsingName : eclipsingNames) {
					remaining--;
					if (remaining > 0) {
						eclipsingComponent = eclipsingComponent
							.append(Component.text(eclipsingName)
								.decorate(TextDecoration.BOLD))
							.append(Component.text(", "));
					} else {
						eclipsingComponent = eclipsingComponent
							.append(Component.text("and "))
							.append(Component.text(eclipsingName)
								.decorate(TextDecoration.BOLD));
					}
				}
			}
			return eclipsingComponent;
		}
	}

	List<ZoneFragment> mFragments = new ArrayList<>();

	public ZoneTreeFactory(Audience audience, Collection<ZoneNamespace> namespaces) {
		// Obtain the fragments within each namespace, processing internal overlaps as we go
		List<NamespaceFragments> processedNamespaces = new ArrayList<>();
		for (ZoneNamespace namespace : namespaces) {
			processedNamespaces.add(new NamespaceFragments(audience, namespace));
		}

		// Merge zone fragments between namespaces to prevent overlaps
		mergeNamespaces(processedNamespaces);

		// Create list of all zone fragments.
		for (NamespaceFragments namespace : processedNamespaces) {
			for (ZoneFragments aZonesFragments : namespace.getZones()) {
				mFragments.addAll(aZonesFragments.getZoneFragments());
			}
		}
	}

	public ZoneTreeBase build() throws Exception {
		return ZoneTreeBase.createZoneTree(mFragments);
	}

	private void mergeNamespaces(List<NamespaceFragments> namespaces) {
		int numNamespaces = namespaces.size();
		for (int i = 0; i < numNamespaces; i++) {
			NamespaceFragments outer = namespaces.get(i);
			for (int j = i + 1; j < numNamespaces; j++) {
				NamespaceFragments inner = namespaces.get(j);
				mergeNamespaces(outer, inner);
			}
		}
	}

	private void mergeNamespaces(NamespaceFragments outerNamespace, NamespaceFragments innerNamespace) {
		for (ZoneFragments outerZone : outerNamespace.getZones()) {
			for (ZoneFragments innerZone : innerNamespace.getZones()) {
				@Nullable ZoneBase overlap = outerZone.getZone().overlappingZone(innerZone.getZone());
				if (overlap == null) {
					continue;
				}
				outerZone.splitByOverlap(overlap, innerZone, true);
				innerZone.splitByOverlap(overlap, outerZone, false);
			}
		}
	}
}
