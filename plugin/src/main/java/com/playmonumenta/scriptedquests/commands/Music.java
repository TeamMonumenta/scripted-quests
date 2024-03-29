package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.SongManager;
import com.playmonumenta.scriptedquests.managers.SongManager.Song;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Music {
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
		optionalArguments.add(new MultiLiteralArgument(Constants.SOUND_CATEGORY_BY_NAME.keySet().toArray(String[]::new)));
		optionalArguments.add(new FloatArgument("volume", 0.0f));
		optionalArguments.add(new FloatArgument("pitch", 0.5f, 2.0f));
		optionalArguments.add(new BooleanArgument("stop on death"));

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

		arguments.clear();
		arguments.add(new MultiLiteralArgument("music"));
		arguments.add(new MultiLiteralArgument("cancel"));
		arguments.add(new MultiLiteralArgument("now", "next"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		new CommandAPICommand("monumenta")
			.withPermission(CommandPermission.fromString("scriptedquests.music.cancel"))
			.withArguments(arguments)
			.executes(Music::runStop)
			.register();

		arguments.add(new MultiLiteralArgument("if", "unless"));
		arguments.add(new SoundArgument.NamespacedKey("music path"));
		new CommandAPICommand("monumenta")
			.withPermission(CommandPermission.fromString("scriptedquests.music.cancel"))
			.withArguments(arguments)
			.executes(Music::runStop)
			.register();

		new CommandAPICommand("monumenta")
			.withPermission(CommandPermission.fromString("scriptedquests.music.isplaying"))
			.withArguments(new MultiLiteralArgument("music"))
			.withArguments(new MultiLiteralArgument("isplaying"))
			.withArguments(new MultiLiteralArgument("now", "next"))
			.withArguments(new EntitySelectorArgument.OnePlayer("players"))
			.withArguments(new SoundArgument.NamespacedKey("music path"))
			.executes(Music::runIsPlaying)
			.register();
	}

	@SuppressWarnings("unchecked")
	public static int runPlay(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		String when = (String) args[2];
		boolean playNow = "now".equals(when);
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
			category = Constants.SOUND_CATEGORY_BY_NAME.getOrDefault(categoryStr,
				Plugin.getInstance().getDefaultMusicSoundCategory());
		} else {
			category = Plugin.getInstance().getDefaultMusicSoundCategory();
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
		boolean stopOnDeath = false;
		if (args.length > 10) {
			stopOnDeath = (boolean) args[10];
		}
		Song song = new Song(musicPath.asString(), category, duration, isLoop, volume, pitch, stopOnDeath);
		return SongManager.playSong(players, song, playNow);
	}

	@SuppressWarnings("unchecked")
	public static int runStop(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		String when = (String) args[2];
		boolean cancelNow = "now".equals(when);
		Collection<Player> players = (Collection<Player>) args[3];

		if (sender instanceof Player player) {
			if (!player.hasPermission("scriptedquests.music.cancel.others") && (players.size() > 1 || !players.contains(player))) {
				throw CommandAPI.failWithString("You do not have permission to run this as another player.");
			}
		}

		if (args.length > 5) {
			String conditionType = (String) args[2];
			boolean conditionIsUnless = "unless".equals(conditionType);
			String musicPath = ((NamespacedKey) args[5]).asString();
			int result = 0;
			for (Player player : players) {
				boolean wasCancelled = false;

				Song nextSong = SongManager.getNextSong(player);
				boolean isPlayingNext = nextSong != null && nextSong.songPath().equals(musicPath);
				if (conditionIsUnless ^ isPlayingNext) {
					SongManager.stopSong(player, false);
					wasCancelled = true;
				}

				if (cancelNow) {
					Song currentSong = SongManager.getCurrentSong(player);
					boolean isPlayingNow = currentSong != null && currentSong.songPath().equals(musicPath);
					if (conditionIsUnless ^ isPlayingNow) {
						SongManager.stopSong(player, true);
						wasCancelled = true;
					}
				}

				if (wasCancelled) {
					++result;
				}
			}
			return result;
		}

		return SongManager.stopSong(players, cancelNow);
	}

	public static int runIsPlaying(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
		String when = (String) args[2];
		boolean isPlayingNow = "now".equals(when);
		Player player = (Player) args[3];

		if (sender instanceof Player playerSender) {
			if (!playerSender.hasPermission("scriptedquests.music.isplaying.others") && !playerSender.equals(player)) {
				throw CommandAPI.failWithString("You do not have permission to run this as another player.");
			}
		}

		String musicPath = ((NamespacedKey) args[4]).asString();

		Song song;
		if (isPlayingNow) {
			song = SongManager.getCurrentSong(player);
		} else {
			song = SongManager.getNextSong(player);
		}

		return song != null && song.songPath().equals(musicPath) ? 1 : 0;
	}
}
