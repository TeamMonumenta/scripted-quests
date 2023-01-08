package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.SoundArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Music {
	private static class Song {
		public String mSongPath;
		public SoundCategory mCategory;
		public long mSongDuration;
		public boolean mIsLoop;
		public float mVolume;
		public float mPitch;

		public Song(String path, SoundCategory category, double durationSeconds, boolean isLoop, float volume, float pitch) {
			mSongPath = path;
			mCategory = category;
			mSongDuration = (long) (1000.0f * durationSeconds);
			mIsLoop = isLoop;
			mVolume = volume;
			mPitch = pitch;
		}

		public boolean equalsSong(Song song) {
			return mIsLoop == song.mIsLoop
				&& mSongDuration == song.mSongDuration
				&& mVolume == song.mVolume
				&& mPitch == song.mPitch
				&& mCategory.equals(song.mCategory)
				&& mSongPath.equals(song.mSongPath);
		}
	}

	private static class PlayerState implements Runnable {
		private static final ZoneId TIMEZONE = ZoneOffset.UTC;

		private final UUID mPlayerId;
		private @Nullable Song mNow = null;
		private @Nullable Song mNext = null;
		private LocalDateTime mNextTime = LocalDateTime.MAX;

		public PlayerState(UUID playerId) {
			mPlayerId = playerId;
		}

		public void playNow() {
			Player player = Bukkit.getPlayer(mPlayerId);
			if (player == null) {
				cancelNext();
				cancelNow();
				return;
			}

			if (millisToRefresh() <= 0) {
				cancelNow();
			}

			if (mNow == null) {
				if (mNext == null) {
					return;
				}
				mNow = mNext;
				if (!mNext.mIsLoop) {
					mNext = null;
				}
			}

			mNextTime = LocalDateTime.now(TIMEZONE).plus(mNow.mSongDuration, ChronoUnit.MILLIS);
			player.playSound(player.getLocation(), mNow.mSongPath, mNow.mCategory, mNow.mVolume, mNow.mPitch);
			mRealTimePool.schedule(this, millisToRefresh(), TimeUnit.of(ChronoUnit.MILLIS));
		}

		public void playNow(Song song) {
			cancelNext();
			if (mNow != null && !mNow.equalsSong(song)) {
				cancelNow();
			}
			playNext(song);
		}

		public void cancelNow() {
			if (mNow == null) {
				return;
			}

			Player player = Bukkit.getPlayer(mPlayerId);
			if (player != null) {
				player.stopSound(mNow.mSongPath, mNow.mCategory);
			}
			mNow = null;
			mNextTime = LocalDateTime.MAX;
		}

		public void playNext(Song song) {
			mNext = song;
			if (mNow == null || millisToRefresh() <= 0) {
				playNow();
			}
		}

		public void cancelNext() {
			mNext = null;
		}

		public long millisToRefresh() {
			try {
				return LocalDateTime.now(TIMEZONE).until(mNextTime, ChronoUnit.MILLIS);
			} catch (ArithmeticException ignored) {
				return Long.MAX_VALUE;
			}
		}

		@Override
		public void run() {
			if (millisToRefresh() <= 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						playNow();
					}
				}.runTask(Plugin.getInstance());
			}
		}
	}

	private static final ScheduledThreadPoolExecutor mRealTimePool = new ScheduledThreadPoolExecutor(1);
	private static final ConcurrentMap<UUID, PlayerState> mPlayerStates = new ConcurrentHashMap<>();

	public static void register() {
		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new MultiLiteralArgument("music"));
		arguments.add(new MultiLiteralArgument("play"));
		arguments.add(new MultiLiteralArgument("now", "next"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		arguments.add(new SoundArgument.NamespacedKey("music path"));
		arguments.add(new DoubleArgument("duration in seconds", 0.001));

		List<Argument<?>> optionalArguments = new ArrayList<>();
		optionalArguments.add(new BooleanArgument("is loop"));
		optionalArguments.add(new MultiLiteralArgument("master", "music", "record", "weather", "block", "hostile", "neutral", "player", "ambient", "voice"));
		optionalArguments.add(new FloatArgument("volume", 0.0f, 1.0f));
		optionalArguments.add(new FloatArgument("pitch", 0.5f, 2.0f));

		new CommandAPICommand("monumenta")
			.withPermission(CommandPermission.fromString("scriptedquests.music.play"))
			.withArguments(arguments)
			.executes(Music::runPlay)
			.register();

		for (Argument<?> argument : optionalArguments) {
			arguments.add(argument);
			new CommandAPICommand("monumenta")
				.withPermission(CommandPermission.fromString("scriptedquests.music.play"))
				.withArguments(arguments)
				.executes(Music::runPlay)
				.register();
		}

		new CommandAPICommand("monumenta")
			.withPermission(CommandPermission.fromString("scriptedquests.music.cancel"))
			.withArguments(new MultiLiteralArgument("music"))
			.withArguments(new MultiLiteralArgument("cancel"))
			.withArguments(new MultiLiteralArgument("now", "next"))
			.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
			.executes(Music::runStop)
			.register();
	}

	@SuppressWarnings("unchecked")
	public static int runPlay(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		String when = (String) args[2];
		boolean isNow = "now".equals(when);
		Collection<Player> players = (Collection<Player>) args[3];

		if (sender instanceof Player player) {
			if (!player.hasPermission("scriptedquests.music.play.others") && (players.size() > 1 || !players.contains(player))) {
				throw CommandAPI.failWithString("You do not have permission to run this as another player.");
			}
		}

		NamespacedKey musicPath = (NamespacedKey) args[4];
		double duration = (Double) args[5];
		boolean isLoop;
		if (args.length > 6) {
			isLoop = (Boolean) args[6];
		} else {
			isLoop = false;
		}
		SoundCategory category;
		if (args.length > 7) {
			String categoryStr = (String) args[7];
			category = switch (categoryStr) {
				case "master" -> SoundCategory.MASTER;
				case "music" -> SoundCategory.MUSIC;
				case "weather" -> SoundCategory.WEATHER;
				case "block" -> SoundCategory.BLOCKS;
				case "hostile" -> SoundCategory.HOSTILE;
				case "neutral" -> SoundCategory.NEUTRAL;
				case "players" -> SoundCategory.PLAYERS;
				case "ambient" -> SoundCategory.AMBIENT;
				case "voice" -> SoundCategory.VOICE;
				default -> SoundCategory.RECORDS;
			};
		} else {
			category = SoundCategory.RECORDS;
		}
		float volume;
		if (args.length > 8) {
			volume = (Float) args[8];
		} else {
			volume = 1.0f;
		}
		float pitch;
		if (args.length > 9) {
			pitch = (Float) args[9];
		} else {
			pitch = 1.0f;
		}
		Song song = new Song(musicPath.asString(), category, duration, isLoop, volume, pitch);

		for (Player player : players) {
			UUID playerId = player.getUniqueId();
			PlayerState state = mPlayerStates.computeIfAbsent(playerId, PlayerState::new);
			if (isNow) {
				state.playNow(song);
			} else {
				state.playNext(song);
			}
		}

		return players.size();
	}


	@SuppressWarnings("unchecked")
	public static int runStop(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		String when = (String) args[2];
		boolean isNow = "now".equals(when);
		Collection<Player> players = (Collection<Player>) args[3];

		if (sender instanceof Player player) {
			if (!player.hasPermission("scriptedquests.music.cancel.others") && (players.size() > 1 || !players.contains(player))) {
				throw CommandAPI.failWithString("You do not have permission to run this as another player.");
			}
		}

		int count = 0;
		for (Player player : players) {
			PlayerState state = mPlayerStates.get(player.getUniqueId());
			if (state == null) {
				continue;
			}
			state.cancelNext();
			if (isNow) {
				state.cancelNow();
			}
		}

		return count;
	}

	public static void onLogout(Player player) {
		PlayerState state = mPlayerStates.remove(player.getUniqueId());
		if (state != null) {
			state.cancelNext();
			state.cancelNow();
		}
	}
}
