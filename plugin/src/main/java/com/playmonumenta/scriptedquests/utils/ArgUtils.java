package com.playmonumenta.scriptedquests.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public class ArgUtils {
	private static final Pattern RE_ALLOWED_WITHOUT_QUOTES = Pattern.compile("[0-9A-Za-z_.+-]+");

	public static boolean requiresQuotes(String arg) {
		if (arg == null) {
			return true;
		}
		return !RE_ALLOWED_WITHOUT_QUOTES.matcher(arg).matches();
	}

	public static @Nullable String quoteIfNeeded(@Nullable String arg) {
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
