package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

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
		 *
		 * Returns true if the zone being overlapped has been completely
		 * eclipsed by the other zone.
		 */
		protected boolean splitByOverlap(ZoneBase overlap, ZoneFragments otherZone, boolean includeOther) {
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
			return newFragments.isEmpty();
		}

		public List<ZoneFragment> getZoneFragments() {
			return new ArrayList<>(mFragments);
		}
	}

	private static class NamespaceFragments {
		private final Set<CommandSender> mSenders;
		private final List<ZoneFragments> mZones = new ArrayList<>();

		protected NamespaceFragments(Set<CommandSender> senders, ZoneNamespace namespace) {
			mSenders = senders;
			for (Zone zone : namespace.getZones()) {
				mZones.add(new ZoneFragments(zone));
			}

			// Split the zones into non-overlapping fragments
			handleOverlaps();
		}

		protected List<ZoneFragments> getZones() {
			return mZones;
		}

		private void handleOverlaps() {
			for (int i = 0; i < mZones.size(); i++) {
				ZoneFragments outer = mZones.get(i);
				for (ZoneFragments inner : mZones.subList(i + 1, mZones.size())) {
					@Nullable ZoneBase overlap = outer.mZone.overlappingZone(inner.mZone);
					if (overlap == null) {
						continue;
					}
					if (inner.splitByOverlap(overlap, outer, false)) {
						for (@Nullable CommandSender sender : mSenders) {
							if (sender != null) {
								sender.sendMessage(Component.text("Total eclipse of zone ", NamedTextColor.RED)
									.append(Component.text(inner.mZone.getName(), NamedTextColor.RED, TextDecoration.BOLD))
									.append(Component.text(" by zone ", NamedTextColor.RED))
									.append(Component.text(outer.mZone.getName(), NamedTextColor.RED, TextDecoration.BOLD)));
							}
						}
					}
					outer.splitByOverlap(overlap, inner, true);
				}
			}
		}
	}

	List<ZoneFragment> mFragments = new ArrayList<>();

	public ZoneTreeFactory(Set<CommandSender> senders, Collection<ZoneNamespace> namespaces) {
		// Obtain the fragments within each namespace, processing internal overlaps as we go
		List<NamespaceFragments> processedNamespaces = new ArrayList<>();
		for (ZoneNamespace namespace : namespaces) {
			processedNamespaces.add(new NamespaceFragments(senders, namespace));
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
