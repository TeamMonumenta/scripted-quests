package com.playmonumenta.scriptedquests.utils;

import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class MMLog {
	private static @Nullable com.playmonumenta.common.MMLog INSTANCE = null;

	public static void init(JavaPlugin plugin) {
		if (INSTANCE == null) {
			INSTANCE = new com.playmonumenta.common.MMLog(plugin, "scriptedquests");
		}
	}

	public static void init(com.playmonumenta.common.MMLog log) {
		INSTANCE = log;
	}

	private static com.playmonumenta.common.MMLog getLogOrThrow() {
		if (INSTANCE == null) {
			throw new RuntimeException("ScriptedQuests logger invoked before being initialized!");
		}
		return INSTANCE;
	}

	public static void setLevel(Level level) {
		getLogOrThrow().setLevel(level);
	}

	public static boolean isLevelEnabled(Level level) {
		return getLogOrThrow().isLevelEnabled(level);
	}

	public static void trace(Supplier<String> msg) {
		getLogOrThrow().trace(msg);
	}

	public static void trace(String msg) {
		getLogOrThrow().trace(msg);
	}

	public static void trace(String msg, Throwable throwable) {
		getLogOrThrow().trace(msg, throwable);
	}

	public static void debug(Supplier<String> msg) {
		getLogOrThrow().debug(msg);
	}

	public static void debug(String msg) {
		getLogOrThrow().debug(msg);
	}

	public static void debug(String msg, Throwable throwable) {
		getLogOrThrow().debug(msg, throwable);
	}

	public static void info(String msg) {
		getLogOrThrow().info(msg);
	}

	public static void info(Supplier<String> msg) {
		getLogOrThrow().info(msg);
	}

	public static void warning(Supplier<String> msg) {
		getLogOrThrow().warning(msg);
	}

	public static void warning(String msg) {
		getLogOrThrow().warning(msg);
	}

	public static void warning(String msg, Throwable throwable) {
		getLogOrThrow().warning(msg, throwable);
	}

	public static void severe(Supplier<String> msg) {
		getLogOrThrow().severe(msg);
	}

	public static void severe(String msg) {
		getLogOrThrow().severe(msg);
	}

	public static void severe(String msg, Throwable throwable) {
		getLogOrThrow().severe(msg, throwable);
	}
}
