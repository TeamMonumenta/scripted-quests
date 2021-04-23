package com.playmonumenta.scriptedquests.translations;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

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
			key = "4/1AY0e-g7PKD3ZC_cVTriKaReYYfYYHPgO2n-qU1wGXKEGArf8by3EsAMqQN0";
		} else {
			sender.sendMessage("attempting auth with given key");
		}

		GoogleTokenResponse response = exchangeAuthKeys(key);

		if (response == null) {
			// key exchange failed. ask for a new auth
			sender.sendMessage("Could not use an old auth token.");
			String authURL = new GoogleAuthorizationCodeRequestUrl(mClientID, mRedirectURI, mScopes).build();
			sender.sendMessage("To create a new one, go to the following link:");
			sender.sendMessage(authURL);
			sender.sendMessage("And relaunch the command with the auth code as argument");
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
