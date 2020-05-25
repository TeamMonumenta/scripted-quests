package com.playmonumenta.scriptedquests.plots;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.playmonumenta.scriptedquests.zones.Zone;

public class PlotManager {
	protected static final String ZONE_LAYER_NAME = "SQPlots";
	private static Plugin mPlugin;
	private static PlotManager mPlotManager = null;
	private static ZoneLayer mZoneLayer;
	private static Map<String, PlotStyle> mPlotStyles = new HashMap<String, PlotStyle>();
	private static Map<Zone, Plot> mPlotByZone = new HashMap<Zone, Plot>();
	private static Map<UUID, Map<PlotStyle, Plot>> mPlotsByUUID = new HashMap<UUID, Map<PlotStyle, Plot>>();

	private PlotManager(Plugin plugin) {
		mPlugin = plugin;
		mZoneLayer = new ZoneLayer(ZONE_LAYER_NAME, true);
		mPlugin.mZoneManager.registerPluginZoneLayer(mZoneLayer);
	}

	public static PlotManager getPlotManager(Plugin plugin) {
		if (mPlotManager == null) {
			mPlotManager = new PlotManager(plugin);
		}

		return mPlotManager;
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public static void reload(Plugin plugin, CommandSender sender) {
		ZoneLayer oldZoneLayer = mZoneLayer;
		mZoneLayer = new ZoneLayer(ZONE_LAYER_NAME, true);
		mPlotByZone.clear();
		mPlotsByUUID.clear();
		mPlotStyles.clear();

		QuestUtils.loadScriptedQuests(plugin, "plot_styles", sender, (object) -> {
			// Load this file into a PlotStyle object
			PlotStyle style = new PlotStyle(sender, object);
			String styleName = style.getName();
			mPlotStyles.put(styleName, style);
			return styleName + ":" + Integer.toString(style.getNumVariants());
		});

		QuestUtils.loadScriptedQuests(plugin, "plots", sender, (object) -> {
			// Load this file into a Plot object
			Plot plot = new Plot(sender, object);
			String debugPlotName = plot.getPlotStyle().getName() + "@" + plot.getInsideLoc().toVector().toString();

			Zone plotZone = new Zone(mZoneLayer, plot.getMin(), plot.getMax(), debugPlotName, new HashSet<String>());
			mPlotByZone.put(plotZone, plot);
			mZoneLayer.addZone(plotZone);

			if (plot.getOwnerId() != null) {
				Map<PlotStyle, Plot> playerPlots = mPlotsByUUID.get(plot.getOwnerId());
				if (playerPlots == null) {
					playerPlots = new HashMap<PlotStyle, Plot>();
					mPlotsByUUID.put(plot.getOwnerId(), playerPlots);
				}

				playerPlots.put(plot.getPlotStyle(), plot);
			}

			return debugPlotName;
		});

		mPlugin.mZoneManager.replacePluginZoneLayer(mZoneLayer);
		oldZoneLayer.invalidate();
	}

	public static void newUnclaimedPlot(String styleName, String styleVariant, Vector insideLoc) {
		PlotStyleVariant variant = getVariant(styleName, styleVariant);
		Plot plot = new Plot(variant, null, insideLoc);
		String debugPlotName = plot.getPlotStyle().getName() + "@" + plot.getInsideLoc().toVector().toString();

		Zone plotZone = new Zone(mZoneLayer, plot.getMin(), plot.getMax(), debugPlotName, new HashSet<String>());
		mPlotByZone.put(plotZone, plot);
		mZoneLayer.addZone(plotZone);

		mZoneLayer.invalidate();
		mPlugin.mZoneManager.replacePluginZoneLayer(mZoneLayer);
	}

	public static Plot getPlayerPlot(OfflinePlayer player, String styleName) {
		Map<PlotStyle, Plot> playerPlots = mPlotsByUUID.get(player.getUniqueId());
		if (playerPlots == null) {
			return null;
		}
		return playerPlots.get(getStyle(styleName));
	}

	public static PlotStyle getStyle(String styleName) {
		return mPlotStyles.get(styleName);
	}

	public static PlotStyleVariant getVariant(String styleName, String variantName) {
		PlotStyle style = getStyle(styleName);
		if (style == null) {
			return null;
		}
		return style.getVariant(variantName);
	}
}
