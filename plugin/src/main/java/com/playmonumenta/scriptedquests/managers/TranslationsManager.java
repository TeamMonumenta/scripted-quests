package com.playmonumenta.scriptedquests.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import com.playmonumenta.scriptedquests.utils.TranslationUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class TranslationsManager {

	private final Plugin mPlugin;

	private TreeMap<UUID, String> mPlayerLanguageMap;
	private TreeMap<String, TreeMap<String, String>> mTranslationsMap;

	public TranslationsManager(Plugin mPlugin) {
		this.mPlugin = mPlugin;

		mPlayerLanguageMap = new TreeMap<>();
		mTranslationsMap = new TreeMap<>();
	}

	public void reload(Plugin plugin, CommandSender sender) {
		mTranslationsMap.clear();

		QuestUtils.loadScriptedQuests(plugin, "translations", sender, (object) -> {

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

				mTranslationsMap.put(message, langMap);
				messageAmount++;

			}

			return messageAmount + " messages loaded into " + translationAmount + " translations";
		});

		for (Player p : Bukkit.getOnlinePlayers()) {
			playerLogin(p, false);
		}

	}

	public String translate(String message, Player player) {

		String lang = mPlayerLanguageMap.get(player.getUniqueId());

		if (lang.equals("en.US")) {
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
		String translatedMessage = translations.getOrDefault(lang, message);

		return translatedMessage;
	}

	private void addNewEntry(String message) {

		// update the loaded translation map
		mTranslationsMap.put(message, new TreeMap<>());

		// write the map into the file
		String content = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(mTranslationsMap);
		System.out.println(content);
		String filename = mPlugin.getDataFolder() + File.separator + "translations" + File.separator + "common" + File.separator + "translations.json";
		try {
			FileUtils.writeFile(filename, content);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			for (StackTraceElement s : e.getStackTrace()) {
				System.out.println(s.toString());
			}
		}

		// reload the translations on all shards
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "broadcastcommand reloadtranslations");
	}

	// launched when a player joins the game
	public void loginEvent(PlayerJoinEvent event) {
		playerLogin(event.getPlayer(), true);
	}

	public void playerLogin(Player player, boolean display) {
		String lang = TranslationUtils.getLanguageOfPlayer(player);
		mPlayerLanguageMap.put(player.getUniqueId(), lang);

		if (display && !lang.equals("en-US")) {
			player.sendMessage("Language set to: " + lang);
		}
	}

	public void quitEvent(PlayerQuitEvent event) {
		mPlayerLanguageMap.remove(event.getPlayer().getUniqueId());
	}
}
