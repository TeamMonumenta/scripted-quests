package com.playmonumenta.scriptedquests.managers;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.handbook.Category;
import com.playmonumenta.scriptedquests.handbook.HandbookEntry;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import com.playmonumenta.scriptedquests.utils.MMLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class HandbookManager implements Reloadable, Listener {

	private List<Category> cats; // meow

	private final String dataFolder;

	private static final String HANDBOOK_PERM_REDIS_PATH = "scriptedquests_handbook_unlocks";

	// the unlock map. Consider this a list of permissions.
	private Map<UUID, Set<String>> dataMap;

	/**
	 * @param dataFolder Where the data should go
	 */
	public HandbookManager(String dataFolder) {
		this.dataFolder = dataFolder;
	}


	@Override
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		this.cats = Collections.synchronizedList(new ArrayList<>());


		final var gson = new Gson();
		final var pass = new HashMap<Category, String>();
		this.dataMap = Maps.newConcurrentMap();
//		pass 1, load the things. Currently, their parents aren't assigned; they aren't linked together.
		try (var dirs = Files.walk(Path.of(dataFolder)).filter(Files::isDirectory)) {
			dirs.forEach(dir -> {
				// we know that each dir will have a json file on it. This contains the category information
				final String categoryInfo;
				try {
					categoryInfo = FileUtils.readFile(dir + "/category_info.json");
				} catch (Exception e) {
					MMLog.warning("Exception while loading category info: " + e + ". Aborting.");
					return;
				}
				final JsonObject categoryObj = gson.fromJson(categoryInfo, JsonObject.class);
				final String catName = categoryObj.get("name").getAsString();
				final String page = categoryObj.get("titlePage").getAsString();
				final Category cat = new Category(catName, page, null, new HashSet<>());
				final List<HandbookEntry> entries1 = new ArrayList<>();
				try (DirectoryStream<Path> entries = Files.newDirectoryStream(cat.getEntriesPath())) {
					for (Path path : entries) {
						final var read = gson.fromJson(FileUtils.readFile(path.toString()), JsonObject.class);
						final var entry = HandbookEntry.fromJsonObject(read, cat);
						if (entry.pages().size() > 100) {
							throw new RuntimeException("Handbook entry " + entry.name() + " has too many pages (100). This is unable to be displayed in a book; consider splitting it into multiple entries.");
						}
						entries1.add(entry);
					}
				} catch (Exception e) {
					MMLog.warning("Error loading : " + e);
				}
				if (entries1.size() > 100) {
					throw new RuntimeException("Category " + cat.name() + " has over 100 entries; this will certainly overflow on pages, consider splitting it into two (or more) categories.");
				}
				cat.entries().addAll(entries1);
				pass.put(cat, categoryObj.has("parentPath") ? categoryObj.get("parentPath").getAsString() : null);
			});
		} catch (Exception e) {
			MMLog.warning("Error loading : " + e);
		}
		// now, we assign parents to the categories.
		this.cats = pass.entrySet().stream().map(e -> {
			final var cat = e.getKey();
			final var parentPath = e.getValue();
			for (final var adopter : pass.keySet()) {
				if (parentPath.equals(adopter.toDirectoryPath().toString())) {
					cat.setParent(adopter);
					return cat;
				}
			}
			return cat;
		}).collect(Collectors.toList());
		MMLog.info("Loaded " + cats.size() + " categories");
	}

	/**
	 * Saves everything into files
	 */
	public void save() {
		final var mainDir = new File(dataFolder);
		if (mainDir.mkdirs()) {
			MMLog.info("Created main Handbook directory " + mainDir);
		}
		cats.forEach(cat -> {

			final var catInfoFile = cat.jsonFilePath();
			if (catInfoFile.toFile().mkdirs()) {
				MMLog.info("Created category path for " + cat.name() + " at " + catInfoFile);
			}
			try {
				FileUtils.writeFile(catInfoFile.toString(),
					cat.getAsJsonObject().toString());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (cat.getEntriesPath().toFile().mkdirs()) {
				MMLog.info("Created directories for category " + cat.name() + " at " + cat.getEntriesPath());
			}
			final var meow = cat.entries();
			meow.forEach(entry -> {
				try {
					final var entryPath = entry.toPath();
					if (entryPath.toFile().createNewFile()) {
						MMLog.info("created new entry file " + entryPath);
					}
					FileUtils.writeFile(entryPath.toString(), entry.getAsJsonObject().toString());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

		});
	}


	/**
	 * Unlocks a {@link HandbookEntry} for a Player if they do not have it.
	 * Returns <code>true</code> if we unlocked it (they did not already have it). Otherwise false
	 *
	 * <p>
	 * If the player does not already have the {@link Category} unlocked,
	 * this method will unlock it and its parent categories for them.
	 *
	 * @param who   The player to unlock this entry for
	 * @param entry The entry in question.
	 * @return True if we unlocked it, false otherwise.
	 */
	public boolean unlockHandbookEntry(Player who, HandbookEntry entry) {
		final var didUnlock = dataMap.get(who.getUniqueId()).add(entry.permission());
		if (didUnlock) {
			who.sendMessage("\n\n\n");
			who.sendMessage(Component.text(String.format("%15s", "HANDBOOK ENTRY UNLOCKED!"), NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
			final var mm = MiniMessage.miniMessage().deserialize(entry.unlockDescription());
			who.sendMessage(mm);
			who.sendMessage("\n\n\n");
			MMLog.info(who.getName() + " unlocked handbook entry " + entry.toPath());
		}
		return didUnlock;
	}


	/**
	 * Also unlocks parent categories
	 *
	 * @param who The player
	 * @param cat The category
	 * @return If the player already had that category
	 */
	public boolean unlockCategory(Player who, Category cat) {
		boolean didAdd = dataMap.get(who.getUniqueId()).add(cat.permission());
		if (didAdd) {
			who.sendMessage("\n\n\n");
			who.sendMessage(Component.text(String.format("%15s", "HANDBOOK CATEGORY " + cat.name() + " UNLOCKED!"), NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
			who.sendMessage("\n\n\n");
			MMLog.info(who.getName() + " unlocked handbook category " + cat.toDirectoryPath());
		}
		return didAdd | cat.traceParents().stream().anyMatch($cat -> unlockCategory(who, $cat));
	}

	/**
	 * @param who The player
	 * @param cat The Category
	 * @return If the player has the Category unlocked.
	 */
	public boolean hasCategoryUnlocked(Player who, Category cat) {
		return dataMap.get(who.getUniqueId()).contains(cat.permission());
	}

	/**
	 * @param who   The player
	 * @param entry The entry
	 * @return If the player has the HandbookEntry unlocked. Assumes that they have the category unlocked if they have the entry.
	 */
	public boolean hasEntryUnlocked(Player who, HandbookEntry entry) {
		return dataMap.get(who.getUniqueId()).contains(entry.permission());
	}


	@EventHandler
	public void onPlayerSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		Set<String> playerHandbookUnlockData = dataMap.get(player.getUniqueId());
		final var arr = new JsonArray();
		for (var data : playerHandbookUnlockData) {
			arr.add(data);
		}
		event.setPluginData(HANDBOOK_PERM_REDIS_PATH, new JsonObjectBuilder().add("data", arr).build());
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		JsonObject data = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), HANDBOOK_PERM_REDIS_PATH);
		if (data != null) {
			dataMap.put(player.getUniqueId(), JsonUtils.intoSet(data.get("data").getAsJsonArray(), JsonElement::getAsString));
		} else {
			dataMap.put(player.getUniqueId(), new HashSet<>());
			MMLog.info("Created a default handbook for " + player.getName());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!p.isOnline()) {
				dataMap.remove(p.getUniqueId());
			}
		}, 100);
	}

	/**
	 * Creates a book gui to open to the player.
	 *
	 * @param cat The category
	 * @return A book to be opened to a player.
	 */
	public static BookMeta createBook(Category cat) {
		return null;
	}
}
