package com.playmonumenta.scriptedquests.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.playmonumenta.scriptedquests.utils.InventoryUtils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.Material;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.Location2DArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.wrappers.Location2D;

public class GiveMap {
	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("givemap")
			.withPermission(CommandPermission.fromString("scriptedquests.givemap"))
			.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.withArguments(new Location2DArgument("location", LocationType.BLOCK_POSITION))
			.withArguments(new IntegerArgument("scale", 0, 4))
			.executes((sender, args) -> {
				giveMap((Collection<Player>)args[0], (Location2D)args[1], (Integer)args[2]);
			})
			.register();
	}

	private static void giveMap(Collection<Player> players, Location2D location, int scale) throws WrapperCommandSyntaxException {
		int x = (((location.getBlockX() / 128) >> scale) << scale) * 128 - 64;
		int z = (((location.getBlockZ() / 128) >> scale) << scale) * 128 - 64;
		// No, it's not a bunch of magic numbers. No it shouldn't be an enum. Oh my head.
		MapView.Scale arbitraryScaleType;
		switch (scale) {
		case 0:
			arbitraryScaleType = MapView.Scale.CLOSEST;
			break;
		case 1:
			arbitraryScaleType = MapView.Scale.CLOSE;
			break;
		case 2:
			arbitraryScaleType = MapView.Scale.NORMAL;
			break;
		case 3:
			arbitraryScaleType = MapView.Scale.FAR;
			break;
		default:
			arbitraryScaleType = MapView.Scale.FARTHEST;
		}
		ItemStack item = new ItemStack(Material.FILLED_MAP);
		MapMeta meta = (MapMeta) item.getItemMeta();
		if (meta.hasMapView()) {
			MapView mapView = meta.getMapView();
			if (mapView != null) {
				mapView.setWorld(location.getWorld());
				mapView.setCenterX(x);
				mapView.setCenterZ(z);
				mapView.setScale(arbitraryScaleType);
				meta.setMapView(mapView);
			}
		}
		item.setItemMeta(meta);

		List<ItemStack> items = new ArrayList<ItemStack>(1);
		items.add(item);
		for (Player player : players) {
			InventoryUtils.giveItems(player, items, false);
		}
	}
}
