package com.playmonumenta.scriptedquests.utils;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class LeaderboardUtils {
	private static final int RANK_COL_WIDTH = 36;
	private static final int NAME_COL_WIDTH = 120;

	public static class LeaderboardEntry implements Comparable<LeaderboardEntry> {
		private String mName;
		private NamedTextColor mColor;
		private boolean mBold = false;
		protected int mValue;
		private String mValueStr;

		public LeaderboardEntry(String name, NamedTextColor color, int value) {
			this(name, color, value, null);
		}

		public LeaderboardEntry(String name, NamedTextColor color, int value, @Nullable String valueStr) {
			mName = name;
			mColor = color;
			mValue = value;
			if (valueStr != null) {
				mValueStr = valueStr;
			} else {
				mValueStr = Integer.toString(value);
			}
		}

		public String getName() {
			return mName;
		}

		public NamedTextColor getColor() {
			return mColor;
		}

		public int getValue() {
			return mValue;
		}

		public String getValueStr() {
			return mValueStr;
		}

		public void setColor(NamedTextColor color) {
			mColor = color;
		}

		public void setBold(boolean bold) {
			mBold = bold;
		}

		public boolean isBold() {
			return mBold;
		}

		@Override
		public int compareTo(LeaderboardEntry other) {
			// compareTo should return < 0 if this is supposed to be
			// less than other, > 0 if this is supposed to be greater than
			// other and 0 if they are supposed to be equal
			return mValue < other.mValue ? -1 : mValue == other.mValue ? 0 : 1;
		}
	}

	public static void sendLeaderboard(Player player, Component title, List<LeaderboardEntry> values, int page, String baseCommand) {
		sendLeaderboard(player, title, values, page, baseCommand, true);
	}

	public static void sendLeaderboard(Player player, Component title, List<LeaderboardEntry> values, int page, String baseCommand, boolean allowChangePages) {
		// Page starts at 1
		if (page < 1) {
			page = 1;
		}

		// print header
		player.sendMessage(Component.text(" Leaderboard - ", NamedTextColor.AQUA).append(title.colorIfAbsent(NamedTextColor.YELLOW)));
		player.sendMessage(" ");

		player.sendMessage(FontUtils.formatRow(
			Component.text("Rank", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC), RANK_COL_WIDTH,
			Component.text("Name", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC), NAME_COL_WIDTH,
			Component.text("Score", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC)));
		for (int i = (page - 1) * 10; i < Math.min(page * 10, values.size()); i++) {
			LeaderboardEntry entry = values.get(i);
			Component rankComp = Component.text(String.valueOf(i + 1), entry.getColor());
			Component nameComp = Component.text(entry.getName(), entry.getColor());
			Component scoreComp = Component.text(entry.getValueStr(), entry.getColor());
			if (entry.isBold()) {
				rankComp = rankComp.decorate(TextDecoration.BOLD);
				nameComp = nameComp.decorate(TextDecoration.BOLD);
				scoreComp = scoreComp.decorate(TextDecoration.BOLD);
			}
			player.sendMessage(FontUtils.formatRow(rankComp, RANK_COL_WIDTH, nameComp, NAME_COL_WIDTH, scoreComp));
		}
		player.sendMessage(" ");

		int rank = 0;
		for (LeaderboardEntry entry : values) {
			rank++;
			if (entry.getName().equals(player.getName())) {
				player.sendMessage(FontUtils.formatRow(
					Component.text(String.valueOf(rank), NamedTextColor.BLUE).decorate(TextDecoration.BOLD), RANK_COL_WIDTH,
					Component.text(entry.getName(), NamedTextColor.BLUE).decorate(TextDecoration.BOLD), NAME_COL_WIDTH,
					Component.text(entry.getValueStr(), NamedTextColor.BLUE).decorate(TextDecoration.BOLD)));
				break;
			}
		}

		if (!allowChangePages) {
			return;
		}

		int pageCount = ((values.size() - 1) / 10) + 1;
		Component prevButton = (page > 1)
			? Component.text("[ < ] ", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)
				.clickEvent(ClickEvent.runCommand(baseCommand + " " + (page - 1)))
			: Component.text("[ < ] ", NamedTextColor.GRAY);
		Component nextButton = (page < pageCount)
			? Component.text("   [ > ]", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)
				.clickEvent(ClickEvent.runCommand(baseCommand + " " + (page + 1)))
			: Component.text("   [ > ]", NamedTextColor.GRAY);
		player.sendMessage(
			Component.text("--==-- ", NamedTextColor.BLUE).decorate(TextDecoration.BOLD)
				.append(prevButton)
				.append(Component.text("  Page:  ", NamedTextColor.YELLOW))
				.append(Component.text(page + "/" + pageCount, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
				.append(nextButton)
				.append(Component.text(" --==--", NamedTextColor.BLUE).decorate(TextDecoration.BOLD)));
		player.sendMessage(" ");
	}
}
