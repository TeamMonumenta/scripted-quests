package com.playmonumenta.scriptedquests.utils;

import org.bukkit.entity.Player;

public class TranslationUtils {

	public static String getLanguageOfPlayer(Player player) {

		for (String s : player.getScoreboardTags()) {
			if (s.startsWith("language_")) {
				return s.split("_")[1];
			}
		}
		return "en-US";

	}
}
