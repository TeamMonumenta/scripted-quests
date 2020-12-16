package com.playmonumenta.scriptedquests.utils;

import java.util.Collection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgUtils {
	private static Pattern ALLOWED_WITHOUT_QUOTES = Pattern.compile("[0-9A-Za-z_.+-]+");

	public static boolean requiresQuotes(String arg) {
		if (arg == null) {
			return true;
		}
		return !ALLOWED_WITHOUT_QUOTES.matcher(arg).matches();
	}

	public static String quoteIfNeeded(String arg) {
		if (arg == null) {
			return null;
		}
		if (requiresQuotes(arg)) {
			return "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		} else {
			return arg;
		}
	}

	public static String[] quoteIfNeeded(Collection<String> args) {
		Collection<String> possiblyQuotedArgs = new ArrayList<String>();
		for (String arg : args) {
			possiblyQuotedArgs.add(quoteIfNeeded(arg));
		}
		return possiblyQuotedArgs.toArray(new String[0]);
	}
}
