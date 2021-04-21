package com.playmonumenta.scriptedquests.translations;

import com.google.gson.GsonBuilder;
import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.List;

public class Translations {

	TranslationsManager mManager;


	public Translations(TranslationsManager manager) {
		mManager = manager;

	}

	public void syncTranslationSheet(CommandSender sender) {

		TranslationGSheet sheet = TranslationGSheet.newInstance();

		if (sheet == null) {
			sender.sendMessage("sheet null");
			return;
		}

		try {
			List<List<Object>> rows = sheet.readTranslationsSheet("Instructions");
			System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(rows));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
