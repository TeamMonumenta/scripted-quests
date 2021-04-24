package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class TranslationsManager implements Listener {
	private static final int WRITE_DELAY_TICKS = 100;

	private static TranslationsManager INSTANCE = null;

	private final Plugin mPlugin;

	private TreeMap<UUID, String> mPlayerLanguageMap;
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

	public static void registerCommands() {
		CommandPermission perm = CommandPermission.fromString("monumenta.translations");

		// reloadtranslations
		new CommandAPICommand("reloadtranslations")
			.withPermission(perm)
			.executes((sender, args) -> {
				if (INSTANCE != null) {
					reload(sender);
				}
			})
			.register();

		// synctranslationsheet
		new CommandAPICommand("synctranslationsheet")
			.withPermission(perm)
			.executes((sender, args) -> {
				if (INSTANCE != null) {
					INSTANCE.syncTranslationSheet(sender, null);
				}
			})
			.register();

		new CommandAPICommand("synctranslationsheet")
			.withPermission(perm)
			.withArguments(new TextArgument("callbackKey"))
			.executes((sender, args) -> {
				if (INSTANCE != null) {
					INSTANCE.syncTranslationSheet(sender, (String)args[0]);
				}

			}).register();
	}

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

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerJoinEvent(PlayerJoinEvent event) {
		playerJoin(event.getPlayer(), true);
	}

	public void playerJoin(Player player, boolean display) {
		String lang = getLanguageOfPlayer(player);
		mPlayerLanguageMap.put(player.getUniqueId(), lang);

		if (display && !lang.equals("en-US")) {
			player.sendMessage("Language set to: " + lang);
		}
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
		String mLastUsedKey;
		final String mSpreadsheetId = "1w7KZZOa8I9J8e6FeHFjTR7u7A35EbpVF11KqMwvfFdM";
		final String mRedirectURI = "urn:ietf:wg:oauth:2.0:oob";
		final List<String> mScopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);
		Plugin mPlugin;

		public List<List<Object>> readSheet(String sheetName) throws IOException {
			ValueRange result = mSheets.spreadsheets().values().get(mSpreadsheetId, sheetName + "!A1:Z9999").execute();
			return result.getValues();
		}

		TranslationGSheet(Plugin plugin) {
			mPlugin = plugin;
		}

		private String[] getClientKeysFromCredsFile() {
			String[] keys = new String[2];
			// load the config file for client keys
			String filename = mPlugin.getDataFolder() + File.separator + "translations" + File.separator + "credentials.txt";
			String contents = "failed::toload";
			try {
				contents = FileUtils.readFile(filename);
			} catch (Exception e) {
				e.printStackTrace();
			}
			String[] split = contents.split("::");
			keys[0] = split[0];
			keys[1] = split[1].replace("\n", "");
			return keys;
		}

		public void writeSheet(String sheetName, List<List<Object>> data) throws IOException {
			ValueRange values = new ValueRange();
			values.setValues(data);
			mSheets.spreadsheets().values().update(mSpreadsheetId, sheetName + "!A1", values)
				.setValueInputOption("RAW").execute();
		}

		boolean init(CommandSender sender, String key) {

			if (key == null) {
				key = mLastUsedKey;
			} else {
				sender.sendMessage("attempting auth with given key");
			}

			String[] clientKeys = getClientKeysFromCredsFile();
			sender.sendMessage("\"" + clientKeys[0] + "\"");
			sender.sendMessage("\"" + clientKeys[1] + "\"");

			GoogleTokenResponse response = exchangeAuthKeys(key, clientKeys);

			if (response == null) {
				// key exchange failed. ask for a new auth
				sender.sendMessage("Could not create a valid auth with the given token.");
				String authURL = new GoogleAuthorizationCodeRequestUrl(clientKeys[0], mRedirectURI, mScopes).build();
				sender.sendMessage("To create a new one, go to the following link:");
				sender.sendMessage(authURL);
				sender.sendMessage("And relaunch the command with the toekn code as argument");
				return true;
			}

			mLastUsedKey = key;
			sender.sendMessage("Valid token. Auth given. gsheet sync starting");

			// deprecated, but could not find an alternative.
			GoogleCredential credential = new GoogleCredential.Builder()
				.setClientSecrets(clientKeys[0], clientKeys[1])
				.setTransport(new NetHttpTransport())
				.setJsonFactory(new JacksonFactory())
				.build()
				.setAccessToken(response.getAccessToken())
				.setRefreshToken(response.getRefreshToken());

			try {
				mSheets = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), credential.getJsonFactory(), credential)
					.setApplicationName("Monumenta Translations").build();
			} catch (GeneralSecurityException | IOException e) {
				e.printStackTrace();
			}

			return false;
		}

		private GoogleTokenResponse exchangeAuthKeys(String key, String[] clientKeys) {
			if (key == null) {
				return null;
			}
			GoogleTokenResponse response;
			try {
				response = new GoogleAuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(),
					clientKeys[0], clientKeys[1], key, mRedirectURI)
					.execute();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			return response;
		}
	}

	TranslationGSheet mGSheet;

	public void syncTranslationSheet(CommandSender sender, String key) {

		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {

			if (mGSheet.init(sender, key)) {
				//initialisation failed, and a new auth has been asked
				return;
			}

			sender.sendMessage("Reading values");

			try {
				List<List<Object>> rows = mGSheet.readSheet("Translations");
				readSheetValues(rows);
			} catch (IOException e) {
				sender.sendMessage("Failed to read values from sheet. Abort. error: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			sender.sendMessage("Reloading shards");
			writeAndReloadNow();

			sender.sendMessage("Writing values");
			List<List<Object>> data = convertDataToSheetList();
			try {
				mGSheet.writeSheet("Translations", data);
			} catch (IOException e) {
				sender.sendMessage("Failed to write values into sheet. error: " + e.getMessage());
				e.printStackTrace();
				return;
			}
			sender.sendMessage("Done!");
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

	private TreeMap<String, String> getListOfAvailableLanguages() {
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
