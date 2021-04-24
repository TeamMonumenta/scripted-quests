package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
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
import org.bukkit.ChatColor;
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
		mGSheet = new TranslationGSheet();
	}

	public class TranslationGSheet {

		Sheets mSheets;
		final String mSpreadsheetId = "1w7KZZOa8I9J8e6FeHFjTR7u7A35EbpVF11KqMwvfFdM";
		final String mClientID = "136451119023-6mjv73r6047am1kkg86pd06j2621ic6h.apps.googleusercontent.com";
		final String mClientSecret = "q07gjmrfLAJHFN06kPi3wZt9";
		final String mRedirectURI = "urn:ietf:wg:oauth:2.0:oob";
		final List<String> mScopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);

		String mLastUsedKey;

		TranslationGSheet() {
		}

		public List<List<Object>> readTranslationsSheet(String sheetName) throws IOException {
			ValueRange result = mSheets.spreadsheets().values().get(mSpreadsheetId, sheetName + "!A1:Z9999").execute();
			return result.getValues();
		}

		boolean init(CommandSender sender, String key) {

			if (key == null) {
				key = mLastUsedKey;
			} else {
				sender.sendMessage("attempting auth with given key");
			}

			GoogleTokenResponse response = exchangeAuthKeys(key);

			if (response == null) {
				// key exchange failed. ask for a new auth
				sender.sendMessage("Could not create a valid auth with the given token.");
				String authURL = new GoogleAuthorizationCodeRequestUrl(mClientID, mRedirectURI, mScopes).build();
				sender.sendMessage("To create a new one, go to the following link:");
				sender.sendMessage(authURL);
				sender.sendMessage("And relaunch the command with the toekn code as argument");
				return true;
			}

			mLastUsedKey = key;
			sender.sendMessage("Valid token. exprires in " + response.getExpiresInSeconds() + " seconds");

			GoogleCredential credential = new GoogleCredential.Builder()
				.setClientSecrets(mClientID, mClientSecret)
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


		private GoogleTokenResponse exchangeAuthKeys(String key) {
			if (key == null) {
				return null;
			}
			GoogleTokenResponse response;
			try {
				response = new GoogleAuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(),
					mClientID, mClientSecret, key, mRedirectURI)
					.execute();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			return response;
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

		// updatetranslationscsv
		new CommandAPICommand("updatetranslationtsv")
			.withPermission(perm)
			.executes((sender, args) -> {
				if (INSTANCE != null) {
					INSTANCE.loadAndUpdateTSV(sender);
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

	// wont be used once gsheet is up
	public void loadAndUpdateTSV(CommandSender sender) {

		String fileName = mPlugin.getDataFolder() + File.separator + "translations" + File.separator + "common" + File.separator + "translations.tsv";

		// load the values of the csv
		loadTSV(sender, fileName);

		// update the base translation file with the potential new values
		writeTranslationFileAndReloadShards();

		// write the translationMap into the csv (update the csv)
		writeTSV(fileName);

		sender.sendMessage("tsv values loaded into translations. tsv updated.");

	}

	// wont be used once gsheet is up
	private void writeTSV(String fileName) {

		// first, go through the map once to get the list of all languages
		TreeSet<String> languagesSet = new TreeSet<>();
		for (Map.Entry<String, TreeMap<String, String>> entry : mTranslationsMap.entrySet()) {
			languagesSet.addAll(entry.getValue().keySet());
		}
		// from that, create the first line of csv, which will act as index
		int valuesSize = languagesSet.size() + 1;
		String[] languages = new String[valuesSize];
		languages[0] = "en-US (base)";
		int i = 1;
		// make the map to get the index from the language (inverse of array)
		HashMap<String, Integer> langMap = new HashMap<>();
		for (String s : languagesSet) {
			languages[i] = s;
			langMap.put(s, i);
			i++;
		}

		ArrayList<String> out = new ArrayList<>();
		out.add(String.join("\t", languages));

		// go through all lines
		for (Map.Entry<String, TreeMap<String, String>> entry : mTranslationsMap.entrySet()) {
			String[] line = new String[valuesSize];
			Arrays.fill(line, "");
			String baseMessage = entry.getKey().replace("\n", "\\n").replace("\"", "@Q");
			line[0] = baseMessage;

			for (Map.Entry<String, String> entry2 : entry.getValue().entrySet()) {
				String translation = entry2.getValue().replace("\n", "\\n").replace("\"", "@Q");
				line[langMap.get(entry2.getKey())] = translation;
			}
			out.add(String.join("\t", line));
		}

		// join lines together
		String finl = String.join("\n", out.toArray(new String[0]));

		try {
			FileUtils.writeFile(fileName, finl);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// wont be used once gsheet is up
	private void loadTSV(CommandSender sender, String fileName) {
		String content = "";
		try {
			content = FileUtils.readFile(fileName);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		String[] lines = content.split("\n");

		// store first line as list of languages
		String[] languages = lines[0].split("\t");

		// parse each line
		for (int i = 1; i < lines.length; i++) {
			String[] values = lines[i].split("\t");
			// first value is the english version, use this to get the already loaded map
			// the loaded translationMap should always exist.
			String message = values[0].replace("\\n", "\n").replace("@Q", "\"");
			TreeMap<String, String> translationMap = mTranslationsMap.getOrDefault(message, new TreeMap<>());
			// parse and store each value
			for (int j = 1; j < values.length; j++) {
				String translation = values[j];
				try {
					if (!translation.equals("")) {
						translationMap.put(languages[j], translation.replace("\\n", "\n").replace("@Q", "\""));
					}
				} catch (NullPointerException e) {
					String errMessage = ChatColor.GOLD + "parsing error on line: " + ChatColor.RESET +
						lines[i].replace("\t", ChatColor.RED + "TAB" + ChatColor.RESET) +
						ChatColor.GOLD + " Could not match with language list: " + ChatColor.RESET +
						lines[0].replace("\t", ChatColor.RED + "TAB" + ChatColor.RESET) +
						ChatColor.GOLD + " Match sizes: " + languages.length + ":" + values.length + ChatColor.RESET;
					sender.sendMessage(errMessage);
					System.out.println(errMessage);
					throw e;
				}
			}
			mTranslationsMap.put(message, translationMap);
		}
	}

	TranslationGSheet mGSheet;

	// stats for sync
	private int mMessageRows;
	private int mDeletedRows;
	private int mLoadedRows;
	private int mLoadedTranslations;

	public void syncTranslationSheet(CommandSender sender, String key) {

		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {

			if (mGSheet.init(sender, key)) {
				//initialisation failed, and a new auth has been asked
				return;
			}

			mMessageRows = 0;
			mDeletedRows = 0;
			mLoadedRows = 0;
			mLoadedTranslations = 0;

			try {
				List<List<Object>> rows = mGSheet.readTranslationsSheet("TEST_DO_NOT_TOUCH");
				readSheetValues(rows);
			} catch (IOException e) {
				sender.sendMessage("Failed to read values from sheet. Abort. error: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			writeAndReloadNow();

		});

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

		mMessageRows++;

		String message = (String)row.get(0);
		String status = (String)row.get(1);

		if (status.equals("DEL")) {
			// line is notified as to be deleted from the system.
			// do that
			mTranslationsMap.remove(message);
			mDeletedRows++;
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

		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(indexToLanguageMap));
	}
}
