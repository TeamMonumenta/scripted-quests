package com.playmonumenta.scriptedquests.managers;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class TranslationsManager implements Listener {
	private static final int WRITE_DELAY_TICKS = 100;

	private static TranslationsManager INSTANCE = null;

	private final Plugin mPlugin;

	private final TreeMap<UUID, String> mPlayerLanguageMap;
	private TreeMap<String, TreeMap<String, String>> mTranslationsMap;
	private boolean mWriting = false;
	private boolean mReading = false;
	private BukkitRunnable mWriteAndReloadRunnable = null;
	private int mWriteDelayTicks = 0;

	public TranslationsManager(Plugin mPlugin) {
		INSTANCE = this;
		this.mPlugin = mPlugin;

		mPlayerLanguageMap = new TreeMap<>();
		mTranslationsMap = new TreeMap<>();
		mGSheet = new TranslationGSheet(mPlugin);

		// registercommands crashes because overridesuggestion launches a method that produces nullpointer
		// because the mTranslationsMap is not filled since the filling is done after init.
		// manually editing the map fixes it
		TreeMap<String, String> tmp = new TreeMap<>();
		tmp.put("en_US", "English");
		tmp.put("de", "German");
		tmp.put("es", "Spanish");
		tmp.put("fr", "French");
		tmp.put("ru", "Russian");
		tmp.put("zhs", "Chinese_Simplified");
		tmp.put("zht", "Chinese_Traditional");
		mTranslationsMap.put(" @ @ LANGUAGES @ @ ", tmp);

		registerCommands();
	}

	public static String translate(Player player, String message) {
		if (INSTANCE == null) {
			return message;
		}
		return INSTANCE.translatePriv(message, player);
	}

	public static String getLanguageOfPlayer(Player player) {
		for (String s : player.getScoreboardTags()) {
			if (s.startsWith("language_") && s.length() > "language_".length()) {
				return s.split("_")[1];
			}
		}
		return "en-US";
	}

	private String translatePriv(String message, Player player) {

		String lang = mPlayerLanguageMap.get(player.getUniqueId());

		if (lang == null || lang.equals("en.US")) {
			// base language, no need to translate
			return message;
		}

		TreeMap<String, String> translations = mTranslationsMap.get(message);
		if (translations == null) {
			// no translations for this message. means its new in the system. needs to be added.
			// do not attempt to translate afterwards, since there will be no translation for that new string anyway
			addNewEntry(message);
			return message;
		}

		// translate the message. if there is no entry for the player language, default fallback to the base message
		String translated = translations.get(lang);
		if (translated == null || translated.isEmpty()) {
			translated = message;
		}

		return translated;
	}

	private void addNewEntry(String message) {

		// update the loaded translation map
		mTranslationsMap.put(message, new TreeMap<>());

		mPlugin.getLogger().info("Added new entry for translations: " + message);

		writeTranslationFileAndReloadShards();
	}

	private void changeLanguage(CommandSender sender, String arg) {
		if (sender instanceof ProxiedCommandSender) {
			ProxiedCommandSender target = (ProxiedCommandSender)sender;
			sender = target.getCallee();
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by players");
			return;
		}
		Player player = (Player)sender;

		String wantedLang = null;
		// go through the language list for a matching argument
		for (Map.Entry<String, String> entry : getListOfAvailableLanguages().entrySet()) {
			if (entry.getValue().toLowerCase().equals(arg.toLowerCase())) {
				wantedLang = entry.getKey();
				break;
			}
		}

		// remove old language tags from the player
		removeLanguageOfPlayer(player);

		if (wantedLang == null) {
			sender.sendMessage("Could not find an existing language corresponding to the value " + arg);
			sender.sendMessage("defaulting to english.");
			return;
		}

		// add the new language tag
		player.addScoreboardTag("language_" + wantedLang);

		// refresh
		playerJoin(player, true);

	}

	public static void removeLanguageOfPlayer(Player player) {
		Set<String> toDelete = new HashSet<>();
		for (String s : player.getScoreboardTags()) {
			if (s.startsWith("language_")) {
				toDelete.add(s);
			}
		}
		for (String s : toDelete) {
			player.removeScoreboardTag(s);
		}
	}

	public void playerJoin(Player player, boolean display) {
		String lang = getLanguageOfPlayer(player);
		mPlayerLanguageMap.put(player.getUniqueId(), lang);

		if (display && !lang.equals("en-US")) {
			player.sendMessage("Language set to: " + lang);
		}
	}

	/**
	 *
	 * COMMANDS
	 *
	 */

	private void registerCommands() {
		CommandPermission perm = CommandPermission.fromString("monumenta.translations");

		// reloadtranslations
		new CommandAPICommand("reloadtranslations")
			.withPermission(perm)
			.executes((sender, args) -> {
				reload(sender);
			}).register();

		// synctranslationsheet
		new CommandAPICommand("synctranslationsheet")
			.withPermission(perm)
			.executes((sender, args) -> {
				syncTranslationSheet(sender, null);
			}).register();

		new CommandAPICommand("synctranslationsheet")
			.withPermission(perm)
			.withArguments(new TextArgument("callbackKey"))
			.executes((sender, args) -> {
				syncTranslationSheet(sender, (String)args[0]);
			}).register();

		new CommandAPICommand("changelanguage")
			.withPermission(CommandPermission.fromString("monumenta.changelanguage"))
			.withAliases("cl")
			.withArguments(new StringArgument("language").overrideSuggestions(getListOfAvailableLanguages().values()))
			.executes((sender, args) -> {
				changeLanguage(sender, (String)args[0]);
			}).register();
	}

	/**
	 *
	 * FILE RELOAD/WRITING/READING
	 *
	 */

	public static void reload(CommandSender sender) {
		if (INSTANCE == null) {
			return;
		}

		if (INSTANCE.mWriting || INSTANCE.mReading) {
			/* Only allow one read/write task at a time. Better to lose translations than cause problems here */
			INSTANCE.mPlugin.getLogger().info("Read was cancelled by existing read/write task");
			return;
		}

		if (INSTANCE.mWriteAndReloadRunnable != null && !INSTANCE.mWriteAndReloadRunnable.isCancelled()) {
			/* This will overwrite the data that would be written - so cancel the pending write task */
			INSTANCE.mPlugin.getLogger().info("Write was cancelled by new reload");
			INSTANCE.mWriteAndReloadRunnable.cancel();
			INSTANCE.mWriteAndReloadRunnable = null;
		}

		INSTANCE.mReading = true;
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {

			TreeMap<String, TreeMap<String, String>> newTranslations = new TreeMap<>();

			QuestUtils.loadScriptedQuests(Plugin.getInstance(), "translations", sender, (object) -> {

				int messageAmount = 0;
				int translationAmount = 0;

				for (Map.Entry<String, JsonElement> entry : object.entrySet()) {

					String message = entry.getKey();

					TreeMap<String, String> langMap = new TreeMap<>();

					for (Map.Entry<String, JsonElement> entry2 : entry.getValue().getAsJsonObject().entrySet()) {

						String lang = entry2.getKey();
						String translated = entry2.getValue().getAsString();

						langMap.put(lang, translated);
						translationAmount++;
					}

					newTranslations.put(message, langMap);
					messageAmount++;
				}

				return messageAmount + " messages loaded into " + translationAmount + " translations from";
			});

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				INSTANCE.mTranslationsMap.clear();
				INSTANCE.mTranslationsMap = newTranslations;
				INSTANCE.mReading = false;
			});
		});

		for (Player p : Bukkit.getOnlinePlayers()) {
			INSTANCE.playerJoin(p, false);
		}
	}

	private void writeTranslationFileAndReloadShards() {
		/* Refresh the write delay since a new write request came in */
		mWriteDelayTicks = WRITE_DELAY_TICKS;

		if (mWriteAndReloadRunnable != null && !mWriteAndReloadRunnable.isCancelled()) {
			/* A write is already scheduled, don't need to do anything */
			return;
		}

		mWriteAndReloadRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mWriteDelayTicks > 0) {
					/* Still waiting for time to elapse before writing */
					mWriteDelayTicks--;
					return;
				}

				if (mReading) {
					/* Whatever was going to be written is being overwritten by read anyway, so cancel the write */
					mPlugin.getLogger().info("Write was cancelled by pending read");
					this.cancel();
					mWriteAndReloadRunnable = null;
					return;
				}

				if (mWriting) {
					/*
					 * Only allow one write task at a time. Better to lose translations than cause problems here.
					 * This will try again next time it ticks until writing is complete
					 */
					mPlugin.getLogger().info("Write was blocked by existing ongoing write, waiting a tick");
					return;
				}

				mWriting = true;

				/* Start writing. There's no longer a need for this runnable */
				this.cancel();
				mWriteAndReloadRunnable = null;

				writeAndReloadNow();

			}
		};

		mWriteAndReloadRunnable.runTaskTimer(mPlugin, 1, 1);
	}

	private void writeAndReloadNow() {
		/* Serialize the content on the main thread so there's no risk that a translation added at the wrong time will cause corruption */
		String content = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(mTranslationsMap);

		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
			// write the map into the file
			String filename = mPlugin.getDataFolder() + File.separator + "translations" + File.separator + "translations.json";
			try {
				FileUtils.writeFile(filename, content);
			} catch (IOException e) {
				mPlugin.getLogger().severe("Caught error writing translations: " + e.getMessage());
				e.printStackTrace();
			}

			// reload the translations on all shards
			// TODO: If the network relay isn't present, this won't do anything and will print an error in the logs
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "broadcastcommand reloadtranslations");
				mWriting = false;
			});
		});
	}


	/**
	 *
	 * EVENTS STUFF
	 *
	 */


	@EventHandler(priority = EventPriority.LOWEST)
	public void playerJoinEvent(PlayerJoinEvent event) {
		playerJoin(event.getPlayer(), true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mPlayerLanguageMap.remove(event.getPlayer().getUniqueId());
	}

	/**
	 *
	 * GSHEET STUFF
	 *
	 */

	public static class TranslationGSheet {

		Sheets mSheets;
		final String mSpreadsheetId = "1w7KZZOa8I9J8e6FeHFjTR7u7A35EbpVF11KqMwvfFdM";
		final List<String> mScopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);
		InputStream mRawCredsInputStream;
		Plugin mPlugin;

		TranslationGSheet(Plugin plugin) {
			mPlugin = plugin;
		}

		public List<List<Object>> readSheet(String sheetName) throws IOException {
			ValueRange result = mSheets.spreadsheets().values().get(mSpreadsheetId, sheetName + "!A1:Z9999").execute();
			return result.getValues();
		}

		public UpdateValuesResponse writeSheet(String sheetName, List<List<Object>> data) throws IOException {
			ValueRange values = new ValueRange();
			values.setValues(data);
			return mSheets.spreadsheets().values().update(mSpreadsheetId, sheetName + "!A1", values)
				.setValueInputOption("RAW").execute();
		}

		boolean init(CommandSender sender) {

			try {
				String filename = mPlugin.getDataFolder() + File.separator + "translations" + File.separator + "credentials.raw";

				// load the config file for raw creds inputstream
				mRawCredsInputStream = new FileInputStream(new File(filename));

				GoogleCredentials googleCredentials = ServiceAccountCredentials.fromStream(mRawCredsInputStream).createScoped(mScopes);

				HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);

				mSheets = new Sheets.Builder(new NetHttpTransport(), new JacksonFactory(), requestInitializer).build();

			} catch (IOException e) {
				e.printStackTrace();
				sender.sendMessage("error while connecting to she sheet: " + e.getMessage());
				return true;
			}

			return false;
		}

	}

	TranslationGSheet mGSheet;

	public void syncTranslationSheet(CommandSender sender, String key) {

		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {

			if (mGSheet.init(sender)) {
				return;
			}

			try {
				sender.sendMessage("Reading values");
				List<List<Object>> rows = mGSheet.readSheet("Translations");
				readSheetValues(rows);
			} catch (IOException e) {
				sender.sendMessage("Failed to read values from sheet. Abort. error: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			sender.sendMessage("Reloading shards");
			writeAndReloadNow();

			try {
				sender.sendMessage("Writing values");
				List<List<Object>> data = convertDataToSheetList();
				UpdateValuesResponse response = mGSheet.writeSheet("Translations", data);
				sender.sendMessage("Done! written " + response.getUpdatedRows() + " rows");
			} catch (IOException e) {
				sender.sendMessage("Failed to write values into sheet. error: " + e.getMessage());
				e.printStackTrace();
				return;
			}

		});
	}

	private List<List<Object>> convertDataToSheetList() {
		List<List<Object>> rows = new ArrayList<>();

		TreeMap<String, String> languages = getListOfAvailableLanguages();
		Set<String> langSet = languages.keySet();
		// it is expected that the languages map contains all languages that have at least 1 translation

		// create and populate the first row
		List<Object> row = new ArrayList<>();
		row.add("English");
		row.add("S | Status");
		for (String s : langSet) {
			row.add(s + " | " + languages.get(s));
		}
		rows.add(row);

		// for every message
		for (String message : mTranslationsMap.keySet()) {
			Map<String, String> translations = mTranslationsMap.get(message);
			row = new ArrayList<>();
			// first column, the english message
			row.add(message);
			// second column, its translation status
			row.add(translations.getOrDefault("S", "WIP"));
			// following colums, translations
			for (String s : langSet) {
				row.add(translations.getOrDefault(s, ""));
			}

			rows.add(row);
		}

		return rows;
	}

	public TreeMap<String, String> getListOfAvailableLanguages() {
		// if null, a lot of things will break. its good that nullpointers will show up then.
		TreeMap<String, String> out = new TreeMap<>();

		for (Map.Entry<String, String> entry : mTranslationsMap.get(" @ @ LANGUAGES @ @ ").entrySet()) {
			if (!entry.getKey().equals("S")) {
				out.put(entry.getKey(), entry.getValue());
			}
		}
		return out;
	}

	private void readSheetValues(List<List<Object>> rows) {
		HashMap<Integer, String> indexToLanguageMap = new HashMap<>();

		// for every row
		for (List<Object> row : rows) {
			// if language index map is empty, then its first row.
			// parse language map form it
			if (indexToLanguageMap.isEmpty()) {
				readLanguageRow(row, indexToLanguageMap);
				continue;
			}

			readDataRow(row, indexToLanguageMap);
		}
	}


	private void readDataRow(List<Object> row, HashMap<Integer, String> indexToLanguageMap) {

		String message = (String)row.get(0);
		String status = (String)row.get(1);

		if (status.equals("DEL")) {
			// line is notified as to be deleted from the system.
			mPlugin.getLogger().info("removing entry :" + message);
			mTranslationsMap.remove(message);
			return;
		}

		TreeMap<String, String> map = mTranslationsMap.getOrDefault(message, new TreeMap<>());

		for (int i = 1; i < row.size(); i++) {
			String translation = (String)row.get(i);
			if (translation == null || translation.equals("")) {
				continue;
			}
			map.put(indexToLanguageMap.get(i), translation);
		}

		mTranslationsMap.put(message, map);

	}

	private void readLanguageRow(List<Object> row, HashMap<Integer, String> indexToLanguageMap) {
		// first cell is english. ignore
		for (int i = 1; i < row.size(); i++) {
			indexToLanguageMap.put(i, ((String)row.get(i)).split(" \\| ")[0]);
		}
	}
}
