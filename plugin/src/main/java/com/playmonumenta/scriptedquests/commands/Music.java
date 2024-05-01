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
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.SoundArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.naming.Name;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Music {
	private static final MultiLiteralArgument whenArg = new MultiLiteralArgument("when", "now", "next");
	private static final EntitySelectorArgument.ManyPlayers playersArg = new EntitySelectorArgument.ManyPlayers("players");
	private static final SoundArgument.NamespacedKey pathArg = new SoundArgument.NamespacedKey("music path");
	private static final DoubleArgument durationArg = new DoubleArgument("duration in seconds", 0.001);
	private static final BooleanArgument loopArg = new BooleanArgument("is loop");
	private static final MultiLiteralArgument categoryArg = new MultiLiteralArgument("category", Constants.SOUND_CATEGORY_BY_NAME.keySet().toArray(String[]::new));
	private static final FloatArgument volumeArg = new FloatArgument("volume", 0.0f);
	private static final FloatArgument pitchArg = new FloatArgument("pitch", 0.5f, 2.0f);
	private static final BooleanArgument stopOnDeathArg = new BooleanArgument("stop on death");
	private static final MultiLiteralArgument conditionArg = new MultiLiteralArgument("condition", "if", "unless");
	private static final EntitySelectorArgument.OnePlayer onePlayerArg = new EntitySelectorArgument.OnePlayer("player");

	public static void register() {
		new CommandAPICommand("monumenta")
			.withPermission(CommandPermission.fromString("scriptedquests.music.play"))
			.withArguments(new LiteralArgument("music"))
			.withArguments(new LiteralArgument("play"))
			.withArguments(whenArg)
			.withArguments(playersArg)
			.withArguments(pathArg)
			.withArguments(durationArg)
			.withOptionalArguments(loopArg)
			.withOptionalArguments(categoryArg)
			.withOptionalArguments(volumeArg)
			.withOptionalArguments(pitchArg)
			.withOptionalArguments(stopOnDeathArg)
			.executes(Music::runPlay)
			.register();

		new CommandAPICommand("monumenta")
			.withPermission(CommandPermission.fromString("scriptedquests.music.cancel"))
			.withArguments(new LiteralArgument("music"))
			.withArguments(new LiteralArgument("cancel"))
			.withArguments(whenArg)
			.withArguments(playersArg)
			.withOptionalArguments(conditionArg)
			.withOptionalArguments(pathArg)
			.executes(Music::runStop)
			.register();

		new CommandAPICommand("monumenta")
			.withPermission(CommandPermission.fromString("scriptedquests.music.isplaying"))
			.withArguments(new LiteralArgument("music"))
			.withArguments(new LiteralArgument("isplaying"))
			.withArguments(whenArg)
			.withArguments(onePlayerArg)
			.withArguments(pathArg)
			.executes(Music::runIsPlaying)
			.register();
	}

	@SuppressWarnings("unchecked")
	public static int runPlay(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		String when = args.getByArgument(whenArg);
		boolean playNow = "now".equals(when);
		Collection<Player> players = args.getByArgument(playersArg);

		if (sender instanceof Player player) {
			if (!player.hasPermission("scriptedquests.music.play.others") && (players.size() > 1 || !players.contains(player))) {
				throw CommandAPI.failWithString("You do not have permission to run this as another player.");
			}
		}

		NamespacedKey musicPath = args.getByArgument(pathArg);
		double duration = args.getByArgument(durationArg);
		boolean isLoop = args.getByArgumentOrDefault(loopArg, false);

		Optional<String> optionalCategory = args.getOptionalByArgument(categoryArg);
		SoundCategory category;
		if (optionalCategory.isPresent()) {
			category = Constants.SOUND_CATEGORY_BY_NAME.getOrDefault(optionalCategory.get(), Plugin.getInstance().getDefaultMusicSoundCategory());
		} else {
			category = Plugin.getInstance().getDefaultMusicSoundCategory();
		}

		float volume = args.getByArgumentOrDefault(volumeArg, 1.0f);
		float pitch = args.getByArgumentOrDefault(pitchArg, 1.0f);
		boolean stopOnDeath = args.getByArgumentOrDefault(stopOnDeathArg, false);
		Song song = new Song(musicPath.asString(), category, duration, isLoop, volume, pitch, stopOnDeath);
		return SongManager.playSong(players, song, playNow);
	}

	@SuppressWarnings("unchecked")
	public static int runStop(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		String when = args.getByArgument(whenArg);
		boolean cancelNow = "now".equals(when);
		Collection<Player> players = args.getByArgument(playersArg);

		if (sender instanceof Player player) {
			if (!player.hasPermission("scriptedquests.music.cancel.others") && (players.size() > 1 || !players.contains(player))) {
				throw CommandAPI.failWithString("You do not have permission to run this as another player.");
			}
		}

		String conditionType = args.getByArgument(conditionArg);
		NamespacedKey musicPathKey = args.getByArgument(pathArg);
		if (conditionType != null && musicPathKey != null) {
			boolean conditionIsUnless = "unless".equals(conditionType);
			String musicPath = musicPathKey.asString();
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

	public static int runIsPlaying(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		String when = args.getByArgument(whenArg);
		boolean isPlayingNow = "now".equals(when);
		Player player = args.getByArgument(onePlayerArg);

		if (sender instanceof Player playerSender) {
			if (!playerSender.hasPermission("scriptedquests.music.isplaying.others") && !playerSender.equals(player)) {
				throw CommandAPI.failWithString("You do not have permission to run this as another player.");
			}
		}

		String musicPath = args.getByArgument(pathArg).asString();

		Song song;
		if (isPlayingNow) {
			song = SongManager.getCurrentSong(player);
		} else {
			song = SongManager.getNextSong(player);
		}

		return song != null && song.songPath().equals(musicPath) ? 1 : 0;
	}
}
