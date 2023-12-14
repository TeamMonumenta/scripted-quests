package com.playmonumenta.scriptedquests.zones;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;
import net.kyori.adventure.audience.Audience;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ZoneTreeFactoryTest {

	@BeforeAll
	public static void setUp() throws Exception {
		Field instance = Plugin.class.getDeclaredField("INSTANCE");
		instance.setAccessible(true);
		Plugin plugin = Mockito.mock(Plugin.class);
		instance.set(null, plugin);
		plugin.mZonePropertyGroupManager = Mockito.mock(ZonePropertyGroupManager.class);
	}

	// test that the zone tree that we build is actually properly representing the real zones
	@Test
	void testZoneTree() throws Exception {

		JsonObject zonesFile = new Gson().fromJson(new InputStreamReader(
			getClass().getClassLoader().getResourceAsStream("zones.json"), StandardCharsets.UTF_8), JsonElement.class).getAsJsonObject();
		ZoneNamespace namespace = new ZoneNamespace("default", false,
			StreamSupport.stream(zonesFile.get("zones").getAsJsonArray().spliterator(), false).map(JsonElement::getAsJsonObject).toList());
		ZoneTreeFactory factory = new ZoneTreeFactory(Mockito.mock(Audience.class), List.of(namespace));

		ZoneTreeBase tree = factory.build();
		tree.print(System.out);

		Random rand = new Random();
		// For each zone in the test file, test a few random points within and some slightly outside
		for (Zone testZone : namespace.getZones()) {
			for (int r = 0; r < 1000; r++) {
				Vector test = new Vector(
					rand.nextDouble(testZone.minCorner().getX() - 5, testZone.maxCorner().getX() + 5),
					rand.nextDouble(testZone.minCorner().getY() - 5, testZone.maxCorner().getY() + 5),
					rand.nextDouble(testZone.minCorner().getZ() - 5, testZone.maxCorner().getZ() + 5));
				Zone treeZone = tree.getZonesLegacy(test).get("default");
				Zone fallbackZone = namespace.fallbackGetZoneLegacy(test);
				Assertions.assertSame(fallbackZone, treeZone, "Zone mismatch at " + test);
			}
		}

	}

}
