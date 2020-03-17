package com.playmonumenta.scriptedquests.utils;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class LeaderboardUtils {

	public static class LeaderboardEntry implements Comparable<LeaderboardEntry> {
		private String mName;
		private String mColor;
		protected int mValue;
		private String mValueStr;

		public LeaderboardEntry(String name, String color, int value) {
			this(name, color, value, null);
		}

		public LeaderboardEntry(String name, String color, int value, String valueStr) {
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

		public String getColor() {
			return mColor;
		}

		public int getValue() {
			return mValue;
		}

		public String getValueStr() {
			return mValueStr;
		}

		public void setColor(String color) {
			mColor = color;
		}

		@Override
		public int compareTo(LeaderboardEntry other) {
			// compareTo should return < 0 if this is supposed to be
			// less than other, > 0 if this is supposed to be greater than
			// other and 0 if they are supposed to be equal
			return mValue < other.mValue ? -1 : mValue == other.mValue ? 0 : 1;
		}
	}

	public static void sendLeaderboard(Player player, String title, List<LeaderboardEntry> values, int page, String baseCommand) {
		// Page starts at 1
		if (page < 1) {
			page = 1;
		}

		// print header
		player.sendMessage(ChatColor.AQUA + " Leaderboard - " + ChatColor.YELLOW + title);
		player.sendMessage(" ");

		// print leaderboard itself
		//TODO : The alignment here really sucks - improve with https://www.spigotmc.org/threads/free-code-sending-perfectly-centered-chat-message.95872/
		player.sendMessage("" + ChatColor.DARK_GRAY + ChatColor.ITALIC + " Rank  |        Name      |    Score");
		for (int i = (page - 1) * 10; i < Math.min(page * 10, values.size()); i++) {
			LeaderboardEntry entry = values.get(i);

			player.sendMessage(String.format("%s%-3s - %-15s -    %s", entry.getColor(), i + 1, entry.getName(), entry.getValueStr()));
		}
		player.sendMessage(" ");

		int i = 0;
		for (LeaderboardEntry entry : values) {
			i++;
			if (entry.getName().equals(player.getName())) {
				player.sendMessage(String.format("%s%-3s - %-15s -    %s", "" + ChatColor.BLUE + ChatColor.BOLD, i, entry.getName(), entry.getValueStr()));
				break;
			}
		}

		int pageCount = ((values.size() - 1) / 10) + 1;
		//TODO: Send messages directly?
		if (pageCount == 1) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " [\"\",{\"text\":\"--==-- \",\"color\":\"blue\",\"bold\":true},{\"text\":\"[ < ] \",\"color\":\"gray\",\"bold\":false},{\"text\":\"  Page:  \",\"color\":\"yellow\"},{\"text\":\"" + String.format("%4d/%-4d", page, pageCount) + "\",\"color\":\"yellow\",\"bold\":true},{\"text\":\"   [ > ]\",\"color\":\"gray\",\"bold\":false},{\"text\":\" --==--\",\"color\":\"blue\",\"bold\":true}]");
		} else if (page == 1) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " [\"\",{\"text\":\"--==-- \",\"color\":\"blue\",\"bold\":true},{\"text\":\"[ < ] \",\"color\":\"gray\",\"bold\":false},{\"text\":\"  Page:  \",\"color\":\"yellow\"},{\"text\":\"" + String.format("%4d/%-4d", page, pageCount) + "\",\"color\":\"yellow\",\"bold\":true},{\"text\":\"   [ > ]\",\"color\":\"light_purple\",\"bold\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + baseCommand + " " + (page + 1) + "\"}},{\"text\":\" --==--\",\"color\":\"blue\",\"bold\":true}]");
		} else if (page == pageCount) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " [\"\",{\"text\":\"--==-- \",\"color\":\"blue\",\"bold\":true},{\"text\":\"[ < ] \",\"color\":\"light_purple\",\"bold\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + baseCommand + " " + (page - 1) + "\"}},{\"text\":\"  Page:  \",\"color\":\"yellow\",\"bold\":false},{\"text\":\"" + String.format("%4d/%-4d", page, pageCount) + "\",\"color\":\"yellow\",\"bold\":true},{\"text\":\"   [ > ]\",\"color\":\"gray\",\"bold\":false},{\"text\":\" --==--\",\"color\":\"blue\",\"bold\":true}]");
		} else {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " [\"\",{\"text\":\"--==-- \",\"color\":\"blue\",\"bold\":true},{\"text\":\"[ < ] \",\"color\":\"light_purple\",\"bold\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + baseCommand + " " + (page - 1) + "\"}},{\"text\":\"  Page:  \",\"color\":\"yellow\",\"bold\":false},{\"text\":\"" + String.format("%4d/%-4d", page, pageCount) + "\",\"color\":\"yellow\",\"bold\":true},{\"text\":\"   [ > ]\",\"color\":\"light_purple\",\"bold\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + baseCommand + " " + (page + 1) + "\"}},{\"text\":\" --==--\",\"color\":\"blue\",\"bold\":true}]");
		}
		player.sendMessage(" ");
	}
}
