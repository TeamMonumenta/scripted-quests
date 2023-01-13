package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SongManager {
	public static class Song {
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

		public String songPath() {
			return mSongPath;
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

		public @Nullable Song currentSong() {
			return mNow;
		}

		public @Nullable Song nextSong() {
			return mNext;
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

	public static int playSong(Collection<Player> players, Song song, boolean playNow) {
		for (Player player : players) {
			playSong(player, song, playNow);
		}
		return players.size();
	}

	public static void playSong(Player player, Song song, boolean playNow) {
		UUID playerId = player.getUniqueId();
		PlayerState state = mPlayerStates.computeIfAbsent(playerId, PlayerState::new);
		if (playNow) {
			state.playNow(song);
		} else {
			state.playNext(song);
		}
	}

	public static int stopSong(Collection<Player> players, boolean cancelNow) {
		int count = 0;
		for (Player player : players) {
			if (stopSong(player, cancelNow)) {
				++count;
			}
		}
		return count;
	}

	public static boolean stopSong(Player player, boolean cancelNow) {
		PlayerState state = mPlayerStates.get(player.getUniqueId());
		if (state == null) {
			return false;
		}
		state.cancelNext();
		if (cancelNow) {
			state.cancelNow();
		}
		return true;
	}

	public static @Nullable Song getCurrentSong(Player player) {
		PlayerState state = mPlayerStates.get(player.getUniqueId());
		if (state == null) {
			return null;
		}
		return state.currentSong();
	}

	public static @Nullable Song getNextSong(Player player) {
		PlayerState state = mPlayerStates.get(player.getUniqueId());
		if (state == null) {
			return null;
		}
		return state.nextSong();
	}

	public static void onLogout(Player player) {
		PlayerState state = mPlayerStates.remove(player.getUniqueId());
		if (state != null) {
			state.cancelNext();
			state.cancelNow();
		}
	}
}
