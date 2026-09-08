package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ActionQuestGuide implements ActionBase {
	public static final String CHECK = "✔";
	public static final int NO_WAYPOINT = Integer.MAX_VALUE;
	public static final Component NOT_MET = Component.text("* Requirements not yet met *", NamedTextColor.DARK_RED);

	private final List<QuestGuideEntry> mEntries = new ArrayList<>();

	public ActionQuestGuide(JsonElement element) {
		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new IllegalArgumentException("quest_guide action must be an array");
		}
		for (JsonElement element2 : array.asList()) {
			JsonObject entry = element2.getAsJsonObject();
			if (entry == null) {
				throw new IllegalArgumentException("entry in quest_guide action array must be an object");
			}
			List<QuestGuideEntryRequirement> prereqs = new ArrayList<>();
			JsonElement reqsJson = entry.get("requirements");
			if (reqsJson != null) {
				JsonArray reqsJson2 = reqsJson.getAsJsonArray();
				if (reqsJson2 == null) {
					throw new IllegalArgumentException("quest_guide action prereqs must be an array");
				}
				for (JsonElement obj : reqsJson2.asList()) {
					prereqs.add(new QuestGuideEntryRequirement(
						obj.getAsJsonObject().get("name").getAsString(),
						obj.getAsJsonObject().get("scoreboard").getAsString(),
						obj.getAsJsonObject().get("min_score_for_completion").getAsInt()
					));
				}
			}
			mEntries.add(new QuestGuideEntry(
				stringOrDefault(entry, "prefix", null),
				entry.get("name").getAsString(),
				entry.get("scoreboard").getAsString(),
				intOrDefault(entry, "min_score_for_started", 1),
				entry.get("min_score_for_completion").getAsInt(),
				stringOrDefault(entry, "difficulty", null),
				prereqs,
				stringOrDefault(entry, "started_at", null),
				intOrDefault(entry, "x", NO_WAYPOINT),
				intOrDefault(entry, "y", NO_WAYPOINT),
				intOrDefault(entry, "z", NO_WAYPOINT)
			));
		}
	}

	@Override
	public void doActions(QuestContext context) {
		Player player = context.getPlayer();
		for (QuestGuideEntry entry : mEntries) {
			Component message;
			int scoreboard = ScoreboardUtils.getScoreboardValue(player, entry.scoreboard);
			if (scoreboard >= entry.minScoreForCompletion) {
				message = entry.completed();
			} else if (scoreboard >= entry.minScoreForStarted) {
				message = entry.started();
			} else {
				message = entry.unstarted(player);
			}
			player.sendMessage(message);
		}
		player.sendMessage(Component.text("[Quest Guide] ", NamedTextColor.GOLD)
			.append(Component.text("Seems like that's it! Hover over a specific quest for more information on it, " +
				"and click on a ", NamedTextColor.WHITE))
			.append(Component.text("red ", NamedTextColor.RED))
			.append(Component.text("entry to have its starting NPC location added to your compass.", NamedTextColor.WHITE))
		);
	}

	private record QuestGuideEntry(@Nullable String prefix, String name, String scoreboard,
								   int minScoreForStarted, int minScoreForCompletion,
								   @Nullable String difficulty, List<QuestGuideEntryRequirement> reqs,
								   @Nullable String startedAt, int x, int y, int z) {
		public boolean hasWaypoint() {
			return x != NO_WAYPOINT && y != NO_WAYPOINT && z != NO_WAYPOINT;
		}

		private Component unstarted(Player player) {
			// create hover text

			// name + check if prereqs met
			boolean prereqsMet = reqs == null || reqs.isEmpty() || reqs.stream().allMatch(req -> req.check(player));
			TextColor nameColor = prereqsMet ? NamedTextColor.RED : NamedTextColor.DARK_RED;
			Component hoverText = Component.text(name, nameColor);

			// difficulty
			if (difficulty != null) {
				hoverText = hoverText.appendNewline();
				hoverText = hoverText.append(Component.text("Difficulty : ", NamedTextColor.GRAY));
				if (difficulty.matches("<.*>")) { // has minimessage formatting
					hoverText = hoverText.append(MessagingUtils.fromMiniMessage(difficulty));
				} else { // no formatting, guess color from text
					hoverText = hoverText.append(Component.text(difficulty, difficultyColor(difficulty)));
				}
			}

			// requirements
			hoverText = hoverText.appendNewline()
				.append(Component.text("Requirements : ", NamedTextColor.GRAY));
			if (reqs == null || reqs.isEmpty()) {
				hoverText = hoverText.append(Component.text("None", NamedTextColor.GREEN));
			} else {
				for (int i = 0; i < reqs.size(); i++) {
					QuestGuideEntryRequirement req = reqs.get(i);
					hoverText = hoverText.append(req.get(player, i == reqs.size() - 1));
				}
			}

			// start
			if (startedAt != null) {
				hoverText = hoverText.appendNewline()
					.append(Component.text("Start : ", NamedTextColor.GRAY))
					.append(Component.text(startedAt, NamedTextColor.DARK_GRAY));
			}

			// "requirements not met"
			if (!prereqsMet) {
				hoverText = hoverText.appendNewline().append(NOT_MET);
			}

			// construct prefix component
			Component prefixC = Component.empty();
			if (prefix != null) {
				prefixC = MessagingUtils.fromMiniMessage(prefix).append(Component.text(" "));
			}

			// construct main component
			Component main = Component.text(name, nameColor).hoverEvent(hoverText);
			if (startedAt != null && hasWaypoint() && prereqsMet) {
				main = main.clickEvent(ClickEvent.runCommand("/waypoint set @s \"&a&lQuest Guide\" \"&aFinding the starting NPC for %s.\" %d %d %d"
					.formatted(name, x, y, z)));
			}

			return prefixC.append(main);
		}

		private Component started() {
			Component hoverText = Component.text(name, NamedTextColor.AQUA);
			if (startedAt != null) {
				hoverText = hoverText.appendNewline()
					.append(Component.text("Started at : ", NamedTextColor.GRAY))
					.append(Component.text(startedAt, NamedTextColor.DARK_GRAY));
			}
			return Component.text(name, NamedTextColor.AQUA).hoverEvent(hoverText);
		}

		private Component completed() {
			Component hoverText = Component.text(name, NamedTextColor.GREEN)
				.appendNewline()
				.append(Component.text("Quest Completed!", NamedTextColor.GRAY));
			return Component.text(CHECK + " " + name, NamedTextColor.GREEN).hoverEvent(hoverText);
		}
	}

	private record QuestGuideEntryRequirement(String name, String scoreboard, int minValue) {
		public Component get(Player player, boolean last) {
			return Component.text(name + (last ? "" : ", "), check(player) ? NamedTextColor.GREEN : NamedTextColor.RED);
		}

		public boolean check(Player player) {
			return ScoreboardUtils.getScoreboardValue(player, scoreboard) >= minValue;
		}
	}

	private static TextColor difficultyColor(String difficulty) {
		if (difficulty.matches("Hard")) {
			return NamedTextColor.RED;
		} else if (difficulty.matches("Easy")) {
			return NamedTextColor.GREEN;
		} else {
			return NamedTextColor.YELLOW;
		}
	}

	private static int intOrDefault(JsonObject object, String property, int defaultValue) {
		JsonElement e = object.get(property);
		return e == null ? defaultValue : e.getAsInt();
	}

	private static @Nullable String stringOrDefault(JsonObject object, String property, @Nullable String defaultValue) {
		JsonElement e = object.get(property);
		return e == null ? defaultValue : e.getAsString();
	}
}
