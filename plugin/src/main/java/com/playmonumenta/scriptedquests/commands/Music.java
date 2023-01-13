package com.playmonumenta.scriptedquests.commands;

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
		return SongManager.stopSong(players, cancelNow);
	}
}
