package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.zone.BaseZone;
import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.zonetree.BaseZoneTree;

public abstract class ZoneManager {
	private Plugin mPlugin;
	private BaseZoneTree mZoneTree;

	public ZoneManager(Plugin plugin) {
		mPlugin = plugin;
	}
}
