package com.playmonumenta.scriptedquests.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class TranslationsManager implements Listener {

	private static TranslationsManager INSTANCE = null;

	private final Plugin mPlugin;

	private TreeMap<UUID, String> mPlayerLanguageMap;
	private TreeMap<String, TreeMap<String, String>> mTranslationsMap;
	private boolean mWriting = false;
	private boolean mReading = false;

	public TranslationsManager(Plugin mPlugin) {
		INSTANCE = this;
		this.mPlugin = mPlugin;

		mPlayerLanguageMap = new TreeMap<>();
		mTranslationsMap = new TreeMap<>();
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
	}

	public static void reload(CommandSender sender) {
		if (INSTANCE == null) {
			return;
		}

		if (INSTANCE.mWriting || INSTANCE.mReading) {
			/* Only allow one read/write task at a time. Better to lose translations than cause problems here */
			return;
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
		if (mWriting || mReading) {
			/* Only allow one read/write task at a time. Better to lose translations than cause problems here */
			return;
		}

		mWriting = true;

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
}
