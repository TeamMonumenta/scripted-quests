package com.playmonumenta.scriptedquests.translations;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class TranslationGSheet {

	Sheets mService;
	final String mSpreadsheetId = "1w7KZZOa8I9J8e6FeHFjTR7u7A35EbpVF11KqMwvfFdM";

	public static TranslationGSheet newInstance() {
		TranslationGSheet sheet = new TranslationGSheet();
		if (!sheet.isInitialized()) {
			return null;
		}
		return sheet;
	}

	private boolean isInitialized() {
		return mService != null;
	}

	private TranslationGSheet() {
		mService = null;
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<List<Object>> readTranslationsSheet(String sheetName) throws IOException {
		ValueRange result = mService.spreadsheets().values().get(mSpreadsheetId, sheetName + "!A1:Z9999").execute();
		return result.getValues();
	}

	private void init() throws Exception {
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

		// Load client secrets.
		GoogleClientSecrets.Details installed = new GoogleClientSecrets.Details();
		installed.set("client_id", "994519604494-d4gf87eg5kkltbso11iv44m46bm5tikr.apps.googleusercontent.com");
		installed.set("project_id", "monumentatransla-1619022573363");
		installed.set("auth_uri", "https://accounts.google.com/o/oauth2/auth");
		installed.set("token_uri", "https://oauth2.googleapis.com/token");
		installed.set("auth_provider_x509_cert_url", "https://www.googleapis.com/oauth2/v1/certs");
		installed.set("client_secret", "UsYcF8b1NDA9Vqd94yIhp73T");
		installed.set("redirect_uris", new String[]{"urn:ietf:wg:oauth:2.0:oob","http://localhost"});
		GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setInstalled(installed);

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
			HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(),
			clientSecrets, Collections.singletonList(SheetsScopes.SPREADSHEETS))
			.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
			.setAccessType("offline")
			.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

		// Build a new authorized API client service.
		mService = new Sheets.Builder(HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), cred)
			.setApplicationName("test")
			.build();

		System.out.println("new instance of GSheet initalized");
	}

}
