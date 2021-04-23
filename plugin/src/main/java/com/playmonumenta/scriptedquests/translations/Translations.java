package com.playmonumenta.scriptedquests.translations;

import com.google.gson.GsonBuilder;
import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class Translations {

	TranslationsManager mManager;
	TranslationGSheet mGSheet;

	// stats for sync
	private int mMessageRows;
	private int mDeletedRows;
	private int mLoadedRows;
	private int mLoadedTranslations;


	public Translations(TranslationsManager manager) {
		mManager = manager;
		mGSheet = new TranslationGSheet();
	}

	public void syncTranslationSheet(CommandSender sender, String key) {

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
			mManager.mTranslationsMap.remove(message);
			mDeletedRows++;
			return;
		}

		TreeMap<String, String> map =  mManager.mTranslationsMap.get(message);

		for (int i = 1; i < row.size(); i++) {
			String translation = (String)row.get(i);
			if (translation == null || translation.equals("")) {
				continue;
			}

			map.put(indexToLanguageMap.get(i), translation);
		}

	}

	private void readLanguageRow(List<Object> row, HashMap<Integer, String> indexToLanguageMap) {

		// first cell is english. ignore
		for (int i = 1; i < row.size(); i++) {
			indexToLanguageMap.put(i, ((String)row.get(i)).split(" \\| ")[0]);
		}

		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(indexToLanguageMap));
	}


}
