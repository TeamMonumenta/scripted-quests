package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.scriptedquests.Plugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ZoneTreeFactoryTest {

	static @Nullable File dataFolder = null;
	static @Nullable MockedStatic<Bukkit> mockedStaticBukkit = null;
	static @Nullable MockedStatic<ZoneNamespace> mockedStaticZoneNamespace = null;

	@BeforeAll
	public static void setUp() throws Exception {
		dataFolder = File.createTempFile("temp", Long.toString(System.nanoTime()));
		if (!dataFolder.delete()) {
			throw new IOException("Unable to create temp directory for test");
		}
		if (!dataFolder.mkdir()) {
			throw new IOException("Unable to create temp directory for test");
		}

		File zoneNamespacesFolder = new File(dataFolder, "zone_namespaces");
		if (!zoneNamespacesFolder.mkdir()) {
			throw new IOException("Unable to create zone_namespaces folder");
		}

		File zoneNamespaceFile = new File(zoneNamespacesFolder, "zones.json");
		try (InputStream zonesFileStream = ZoneTreeFactoryTest.class
			.getClassLoader()
			.getResourceAsStream("zone_namespaces/zones.json")) {
			Files.copy(Objects.requireNonNull(zonesFileStream),
				zoneNamespaceFile.toPath(),
				StandardCopyOption.REPLACE_EXISTING);
		}

		mockedStaticBukkit = Mockito.mockStatic(Bukkit.class);
		Mockito.when(Bukkit.getConsoleSender()).thenReturn(new TestConsoleSender());

		mockedStaticZoneNamespace = Mockito.mockStatic(ZoneNamespace.class);

		Field instance = Plugin.class.getDeclaredField("INSTANCE");
		instance.setAccessible(true);
		Plugin plugin = Mockito.mock(Plugin.class);
		instance.set(null, plugin);
		plugin.mZonePropertyGroupManager = Mockito.mock(ZonePropertyGroupManager.class);

		Field dataFolderField = JavaPlugin.class.getDeclaredField("dataFolder");
		dataFolderField.setAccessible(true);
		dataFolderField.set(plugin, dataFolder);

		plugin.mZoneManager = ZoneManager.createInstance(plugin);

		Field zoneManagerInstance = ZoneManager.class.getDeclaredField("INSTANCE");
		zoneManagerInstance.setAccessible(true);
		zoneManagerInstance.set(null, plugin.mZoneManager);

		Field zoneManagerIsTest = ZoneManager.class.getDeclaredField("IS_TEST_MODE");
		zoneManagerIsTest.setAccessible(true);
		zoneManagerIsTest.set(null, true);

		ZoneManager.ZoneState activeState = new ZoneManager.ZoneState();

		Field zoneManagerActiveState = ZoneManager.class.getDeclaredField("mActiveState");
		zoneManagerActiveState.setAccessible(true);
		zoneManagerActiveState.set(plugin.mZoneManager, activeState);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (dataFolder != null) {
			deleteTree(dataFolder);
			dataFolder = null;
		}

		if (mockedStaticBukkit != null) {
			mockedStaticBukkit.close();
			mockedStaticBukkit = null;
		}

		if (mockedStaticZoneNamespace != null) {
			mockedStaticZoneNamespace.close();
			mockedStaticZoneNamespace = null;
		}
	}

	private static ZoneTreeBase getZoneTree(ZoneManager zoneManager) throws Exception {
		Field zoneManagerActiveState = ZoneManager.class.getDeclaredField("mActiveState");
		zoneManagerActiveState.setAccessible(true);
		return ((ZoneManager.ZoneState) zoneManagerActiveState.get(zoneManager)).mZoneTree;
	}

	private static void deleteTree(File file) throws IOException {
		if (!Files.isSymbolicLink(file.toPath())) {
			File[] contents = file.listFiles();
			if (contents != null) {
				for (File child : contents) {
					deleteTree(child);
				}
			}
		}
		if (!file.delete()) {
			throw new IOException("Failed to delete " + file);
		}
	}

	// test that the zone tree that we build is actually properly representing the real zones
	@Test
	void testZoneTree() throws Exception {

		Plugin plugin = Plugin.getInstance();
		ZoneManager zoneManager = plugin.mZoneManager;
		zoneManager.doReload(plugin, true);

		Random rand = new Random();
		ZoneTreeBase tree = getZoneTree(zoneManager);

		tree.print(System.out);

		for (ZoneNamespace namespace : zoneManager.getNamespaces()) {
			// For each zone in the test file, test a few random points within and some slightly outside
			for (Zone testZone : namespace.getZones()) {
				for (int r = 0; r < 1000; r++) {
					Vector test = new Vector(
						rand.nextDouble(testZone.minCorner().getX() - 5, testZone.maxCorner().getX() + 5),
						rand.nextDouble(testZone.minCorner().getY() - 5, testZone.maxCorner().getY() + 5),
						rand.nextDouble(testZone.minCorner().getZ() - 5, testZone.maxCorner().getZ() + 5));
					Zone treeZone = tree.getZones("test_world", test).get("default");
					Zone fallbackZone = namespace.fallbackGetZone("test_world", test);
					Assertions.assertSame(fallbackZone, treeZone, "Zone mismatch at " + test);
				}
			}
		}

	}

}
