package com.playmonumenta.scriptedquests.managers;

import com.google.common.collect.Lists;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

public class HandbookManager implements Reloadable, Listener {

	private List<Category> cats; // meow

	private Map<String, Category> pathsToCats;

	private Map<String, HandbookEntry> pathsToEntries;

	private final String dataFolder;


	private final String CATEGORY_INFO_FILENAME = "category_info.json";

	private static final String HANDBOOK_PERM_REDIS_PATH = "scriptedquests_handbook_unlocks";

	// the unlock map. Consider this a list of permissions.
	private Map<UUID, Set<String>> dataMap;

	/**
	 * @param dataFolder Where the data should go
	 */
	public HandbookManager(String dataFolder) {
		this.dataFolder = dataFolder;
	}

	// the base category that contains no data but acts as a root. Everything is a child of this category.
	private final Category ROOT = new Category("ROOT", null, null, new HashSet<>());


	@Override
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		this.cats = Collections.synchronizedList(Lists.newArrayList(ROOT));
		pathsToCats = new ConcurrentHashMap<>();
		pathsToCats.put("ROOT", ROOT);

		final Path dataFolderPath = Path.of(dataFolder);
		if (dataFolderPath.toFile().mkdirs()) {
			if (sender != null) sender.sendMessage("Created Handbook directory.");
		}

		final var gson = new Gson();
		final var pass = new HashMap<Category, String>();
		this.dataMap = Maps.newConcurrentMap();
		// Load the things. Currently, their parents aren't assigned; they aren't linked together.
		try (var dirs = Files.walk(dataFolderPath).skip(1).filter(Files::isDirectory)) {
			dirs.forEach(dir -> {
				// we know that each dir will have a json file on it. This contains the category information
				final String categoryInfo;
				try {
					categoryInfo = FileUtils.readFile(dir + "/" + CATEGORY_INFO_FILENAME);
				} catch (Exception e) {
					if (sender != null) {
						sender.sendMessage("Exception while loading category info for " + dir + ", " + e );
					}
					return;
				}
				final JsonObject categoryObj = gson.fromJson(categoryInfo, JsonObject.class); // parse json from file.
				final String catName = categoryObj.get("name").getAsString();
				final Component page = GsonComponentSerializer.gson().deserializeFromTree(categoryObj.get("titlePage"));
				final Category cat = new Category(catName, page, ROOT, new HashSet<>()); // The default parent of a category is the BASE category.
				final List<HandbookEntry> parsedEntries = new ArrayList<>(); // Holds the parsed entries from the json
				try (DirectoryStream<Path> entries = Files.newDirectoryStream(cat.getEntryFolder())) { // Open the entry folder
					for (Path path : entries) { // and iterate through their files,
						final var read = gson.fromJson(FileUtils.readFile(path.toString()), JsonObject.class); // parsing the json
						final var entry = HandbookEntry.fromJsonObject(read, cat); // creating the object
						if (entry.pages().size() > 100) { // Ensure that we can make a book out of this
							throw new RuntimeException("Handbook entry " + entry.name() + " has too many pages (" + entry.pages().size() + ">100). This is unable to be displayed in a book; consider splitting it into multiple entries.");
						}
						parsedEntries.add(entry);
						pathsToEntries.put(entry.toPath().toString(), entry);
					}
				} catch (Exception e) {
					MMLog.warning("Error loading : " + e);
				}
				if (parsedEntries.size() > 100) {
					if (sender != null) {
						sender.sendMessage("Category " + cat.name() + " has over 100 entries; this will certainly overflow on pages, consider splitting it into two (or more) categories.");
					} else {
						MMLog.warning("Category " + cat.name() + " has over 100 entries; this will certainly overflow on pages, consider splitting it into two (or more) categories.");
					}
				}
				cat.entries().addAll(parsedEntries);
				pass.put(cat, categoryObj.has("parentPath") ? categoryObj.get("parentPath").getAsString() : null);
			});
		} catch (Exception e) {
			if (sender != null) {
				sender.sendMessage("Error loading dirs: " + e);
			}
		}
		// now, we assign parents to the categories and build the fields.
		for (Map.Entry<Category, String> e : pass.entrySet()) {
			final Category cat = e.getKey();
			final String parentPath = e.getValue();
			for (final Category adopter : pass.keySet()) {
				if (parentPath.equals(adopter.toDirectoryPath().toString())) {
					cat.setParent(adopter);
				}
			}
			cats.add(cat);
			pathsToCats.put(cat.toDirectoryPath().toString(), cat);
		}
		sender.sendMessage("Loaded " + cats.size() + " categories");
	}

	/**
	 * Saves everything from {@link HandbookManager#cats} into files
	 */
	public void save() {
		final var mainDir = new File(dataFolder);
		if (mainDir.mkdirs()) {
			MMLog.info("Created main Handbook directory " + mainDir);
		}
		cats.forEach(cat -> {

			final var catInfoFile = resolveToJsonFile(cat);
			if (catInfoFile.toFile().mkdirs()) {
				MMLog.info("Created category path for " + cat.name() + " at " + catInfoFile);
			}
			try {
				FileUtils.writeFile(catInfoFile.toString(),
					cat.getAsJsonObject().toString());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			final var entryDirectory = mainDir.toPath().resolve(cat.getEntryFolder()).toFile();
			if (entryDirectory.mkdirs()) {
				MMLog.info("Created directories for category " + cat.name() + " at " + entryDirectory);
			}
			final var meow = cat.entries();
			meow.forEach(entry -> {
				try {
					final var entryPath = resolveToJsonFile(entry).toFile();
					if (entryPath.mkdirs()) {
						MMLog.info("Created directories for a HandbookEntry at " + entryPath);
					}
					if (entryPath.createNewFile()) {
						MMLog.info("Created new entry file " + entryPath);
					}
					FileUtils.writeFile(entryPath.toPath().toString(), entry.getAsJsonObject().toString());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

		});
	}

	/**
	 *
	 * @param category The category to resolve to.
	 * @return The real path that the category's <b>json file</b> lives at in the server's files.
	 */
	private Path resolveToJsonFile(Category category) {
		return new File(dataFolder).toPath().resolve(category.jsonFilePath());
	}
	/**
	 *
	 * @param entry The category to resolve to.
	 * @return The real path that the entry lives at in the server's files.
	 */
	private Path resolveToJsonFile(HandbookEntry entry) {
		return new File(dataFolder).toPath().resolve(entry.category().getEntryFolder()).resolve(entry.toPath());
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
		final boolean didUnlock = dataMap.get(who.getUniqueId()).add(entry.permission());
		if (didUnlock) {
			who.sendMessage("\n\n\n");
			who.sendMessage(Component.text(String.format("%15s", "HANDBOOK ENTRY UNLOCKED!"), NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
			final var mainMessage = MiniMessage.miniMessage().deserialize(entry.unlockDescription());
			who.sendMessage(mainMessage);
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
		final boolean didAdd = dataMap.get(who.getUniqueId()).add(cat.permission());
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

	public Path getRealFileLocation(Category category) {
		return Path.of(dataFolder).resolve(category.toDirectoryPath());
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
	 * Opens a category into a player.
	 *
	 * @param cat    The category
	 * @param player The player
	 */
	public void openCategoryForPlayer(Category cat, Player player) {
		final var book = Book.builder().pages(cat.intoBookPages()).build();
		player.openBook(book);
	}

	public Map<String, Category> getPathsAndCats() {
		return pathsToCats;
	}

	public List<Category> getCats() {
		return cats;
	}

	/**
	 * Grabs a {@link Category} given a path to it.
	 *
	 * @param path The path to the category as specified by {@link Category#toDirectoryPath()}
	 * @return The Category in question.
	 * @see com.playmonumenta.scriptedquests.commands.HandbookCommand
	 */
	@Nullable
	public Category categoryByPath(String path) {
		return pathsToCats.get(path);
	}

	/**
	 * Grabs a {@link HandbookEntry} given a path to it.
	 *
	 * @param path The path to the category as specified by {@link HandbookEntry#toPath()}
	 * @return The Category in question.
	 * @see com.playmonumenta.scriptedquests.commands.HandbookCommand
	 */
	@Nullable
	public HandbookEntry entryByPath(String path) {
		return pathsToEntries.get(path);
	}
	/**
	 * @param category
	 * @return true if the category was added. false if it already existed.
	 */
	public boolean createCategory(Category category) {
		if (cats.contains(category)) {
			return false;
		}
		cats.add(category);
		save();
		return true;
	}

	/**
	 * @param name The name of the category to look for.
	 * @return The category with the given name.
	 */
	public Category categoryByName(String name) {
		return cats.stream().filter(x -> x.name().equals(name)).findFirst().orElse(null);
	}
}
