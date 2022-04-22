package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

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

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;

public class TranslationsManager implements Listener {
	private static final int WRITE_DELAY_TICKS = 100;

	private static TranslationsManager INSTANCE = null;

	private final Plugin mPlugin;
	private final TreeMap<UUID, String> mPlayerLanguageMap;
	private TreeMap<String, TreeMap<String, String>> mTranslationsMap;
	private ConfigurationSection mGSheetConfig = null;

	private boolean mWriting = false;
	private boolean mReading = false;
	private BukkitRunnable mWriteAndReloadRunnable = null;
	private int mWriteDelayTicks = 0;

	public TranslationsManager(Plugin plugin, ConfigurationSection translationsConfig) {
		mPlugin = plugin;
		mPlayerLanguageMap = new TreeMap<>();
		mTranslationsMap = new TreeMap<>();

		if (!translationsConfig.contains("enabled") || !translationsConfig.getBoolean("enabled", false)) {
			return;
		}

		INSTANCE = this;

		/* Register the main two commands */
		new CommandAPICommand("reloadtranslations")
			.withPermission(CommandPermission.fromString("scriptedquests.translations.reload"))
			.executes((sender, args) -> {
				reload(sender);
			}).register();

		new CommandAPICommand("changelanguage")
			.withPermission(CommandPermission.fromString("scriptedquests.translations.changelanguage"))
			.withAliases("cl")
			.withArguments(new StringArgument("language").replaceSuggestions(info -> {
				return getListOfAvailableLanguages(true).values().toArray(new String[0]);
			}))
			.executes((sender, args) -> {
				changeLanguage(sender, (String)args[0]);
			}).register();

		/* Check if the gsheet config is enabled - if it is, register the gsheet sync command */
		if (translationsConfig.contains("gsheet_sync")) {
			mGSheetConfig = translationsConfig.getConfigurationSection("gsheet_sync");
			if (mGSheetConfig != null && mGSheetConfig.contains("enabled") && mGSheetConfig.getBoolean("enabled", false)) {
				/* Register the gsheet sync command */
				new CommandAPICommand("synctranslationsheet")
					.withPermission(CommandPermission.fromString("scriptedquests.translations.sync"))
					.executes((sender, args) -> {
						syncTranslationSheet(sender);
					}).register();
			}
		}
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
			if (entry.getValue().equalsIgnoreCase(arg)) {
				wantedLang = entry.getKey();
				break;
			}
		}

		// remove old language tags from the player
		removeLanguageOfPlayer(player);

		if ("English".equalsIgnoreCase(arg)) {
			playerJoin(player, true);
			return;
		}
		if (wantedLang == null) {
			sender.sendMessage("Could not find an existing language corresponding to the value " + arg);
			sender.sendMessage("defaulting to english.");
			playerJoin(player, true);
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

				return messageAmount + " messages loaded into " + translationAmount + " translated messages";
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

				/* Serialize the content on the main thread so there's no risk that a translation added at the wrong time will cause corruption */
				String content = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(mTranslationsMap);

				Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
					// write the map into the file
					String filename = Paths.get(mPlugin.getDataFolder().toString(), "translations", "translations.json").toString();
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
		};

		mWriteAndReloadRunnable.runTaskTimer(mPlugin, 1, 1);
	}

	/**
	 *
	 * EVENTS STUFF
	 *
	 */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerJoinEvent(PlayerJoinEvent event) {
		playerJoin(event.getPlayer(), true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mPlayerLanguageMap.remove(event.getPlayer().getUniqueId());
	}

	/**
	 *
	 * GSHEET STUFF
	 *
	 */

	private static class TranslationGSheet {
		private final Sheets mSheets;
		private final String mSheetId;
		private final String mSheetName;
		private final List<String> mScopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);

		private TranslationGSheet(ConfigurationSection gsheetSettings) throws Exception {
			if (!gsheetSettings.contains("sheet_id")) {
				throw new Exception("Google Sheets configuration missing 'sheet_id'");
			}
			mSheetId = gsheetSettings.getString("sheet_id");

			if (!gsheetSettings.contains("sheet_name")) {
				throw new Exception("Google Sheets configuration missing 'sheet_name'");
			}
			mSheetName = gsheetSettings.getString("sheet_name");

			File credentialsFile = Paths.get(Plugin.getInstance().getDataFolder().toString(), "gsheet_credentials.json").toFile();
			if (!credentialsFile.isFile()) {
				throw new Exception("Google Sheets requires a gsheet_credentials.json file in the plugin data folder");
			}
			InputStream credentialsStream = new FileInputStream(credentialsFile);
			GoogleCredentials googleCredentials = ServiceAccountCredentials.fromStream(credentialsStream).createScoped(mScopes);

			HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);

			mSheets = new Sheets.Builder(new NetHttpTransport(), new JacksonFactory(), requestInitializer).build();
		}

		private List<List<Object>> readSheet() throws IOException {
			ValueRange result = mSheets.spreadsheets().values().get(mSheetId, mSheetName + "!A1:Z99999").execute();
			return result.getValues();
		}

		private UpdateValuesResponse writeSheet(List<List<Object>> data) throws IOException {
			ValueRange values = new ValueRange();
			values.setValues(data);
			return mSheets.spreadsheets().values().update(mSheetId, mSheetName + "!A1", values)
				.setValueInputOption("RAW").execute();
		}
	}

	private void syncTranslationSheet(CommandSender sender) {

		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
			/* First make a backup copy of the translations database */
			try {
				sender.sendMessage("Making backup copy of the translations.json database");
				Date date = Calendar.getInstance().getTime();
				String filename = Paths.get(mPlugin.getDataFolder().toString(), "translations", "translations.json").toString();
				String toFilename = filename + new SimpleDateFormat(".yyyy-MM-dd_hh:mm:ss").format(date);
				Files.copy(new File(filename).toPath(),
				           new File(toFilename).toPath(),
				           StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				sender.sendMessage("Failed to backup translations.json database: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			TranslationGSheet gSheet;
			try {
				sender.sendMessage("Initializing Google Sheets");
				gSheet = new TranslationGSheet(mGSheetConfig);
			} catch (Exception e) {
				sender.sendMessage("Failed initialize Google Sheets: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			List<List<Object>> rows;
			try {
				sender.sendMessage("Reading values");
				rows = gSheet.readSheet();
				sender.sendMessage("Recieved " + rows.size() + " rows from the GSheet");
			} catch (IOException e) {
				sender.sendMessage("Failed to read values from sheet. Abort. error: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			Bukkit.getScheduler().runTask(mPlugin, () -> {
				sender.sendMessage("old translation map size: " + mTranslationsMap.size());
				readSheetValues(rows);
				sender.sendMessage("new translation map size: " + mTranslationsMap.size());

				sender.sendMessage("Reloading shards");
				writeTranslationFileAndReloadShards();
				sender.sendMessage("translation map size: " + mTranslationsMap.size());

				try {
					sender.sendMessage("Compiling values");
					List<List<Object>> data = convertDataToSheetList();
					sender.sendMessage("Writing " + data.size() + " rows to the GSheet");
					UpdateValuesResponse response = gSheet.writeSheet(data);
					sender.sendMessage("Done! written " + response.getUpdatedRows() + " rows");
				} catch (IOException e) {
					sender.sendMessage("Failed to write values into sheet. error: " + e.getMessage());
					e.printStackTrace();
				}
			});
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
		for (Map.Entry<String, TreeMap<String, String>> entry : mTranslationsMap.entrySet()) {
			String message = entry.getKey();
			Map<String, String> translations = entry.getValue();

			row = new ArrayList<>();
			// first column, the english message
			row.add(message);
			// second column, its translation status
			row.add(translations.getOrDefault("S", "WIP"));
			// following columns, translations
			for (String s : langSet) {
				row.add(translations.getOrDefault(s, ""));
			}

			rows.add(row);
		}

		return rows;
	}

	public TreeMap<String, String> getListOfAvailableLanguages() {
		return getListOfAvailableLanguages(false);
	}

	public TreeMap<String, String> getListOfAvailableLanguages(boolean includeDefault) {
		// if null, a lot of things will break. its good that nullpointers will show up then.
		TreeMap<String, String> out = new TreeMap<>();
		if (includeDefault) {
			out.put("en | English", "English");
		}

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
