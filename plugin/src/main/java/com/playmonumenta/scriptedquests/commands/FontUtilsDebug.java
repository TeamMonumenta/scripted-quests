package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.FontUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

/**
 * /scriptedquests fontutilsdebug
 *
 * Sends test patterns to verify FontUtils.CHAR_WIDTHS accuracy.
 * Each row pads to TOTAL_WIDTH px using ' (2px) as a ruler.
 * The test character appears mid-row in yellow; the right ruler follows.
 * If the declared width is correct, the right edges (where the gray label
 * begins) will align with the reference line. A short row = declared too wide;
 * a long row = declared too narrow.
 */
public class FontUtilsDebug {
	// Total target row width in pixels. Must be even (ruler char ' = 2px).
	private static final int TOTAL_WIDTH = 100;
	// Pixels of ' ruler before the test character.
	private static final int LEFT_RULER = 30;

	// All chars given explicit widths in FontUtils.CHAR_WIDTHS (non-default).
	private static final char[] NON_DEFAULT_CHARS = {
		' ', '!', '"', '(', ')', '*', ',', '.', ':', ';', '<', '>', '@',
		'I', '[', '\'', ']', '`', 'f', 'i', 'k', 'l', 't', '{', '|', '}', '~'
	};

	public static void register() {
		new CommandAPICommand("scriptedquests")
			.withSubcommand(new CommandAPICommand("fontutilsdebug")
				.withPermission(CommandPermission.fromString("scriptedquests.fontutilsdebug"))
				.executesPlayer((Player player, dev.jorel.commandapi.executors.CommandArguments args) -> {
					sendDebug(player);
				}))
			.register();
	}

	private static void sendDebug(Player player) {
		player.sendMessage(Component.text("=== FontUtils width debug ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
		player.sendMessage(Component.text(
			"Layout: [" + LEFT_RULER + "px of '] <char> [remaining ' to " + TOTAL_WIDTH + "px]  label",
			NamedTextColor.GRAY));
		player.sendMessage(Component.text(
			"Compare right edges (where gray label starts). Short = declared too wide; long = declared too narrow.",
			NamedTextColor.GRAY));
		player.sendMessage(Component.empty());

		// Reference line — pure ' chars filling TOTAL_WIDTH px; all other rows should match this.
		player.sendMessage(Component.empty()
			.append(Component.text("'".repeat(TOTAL_WIDTH / 2), NamedTextColor.DARK_GRAY))
			.append(Component.text("  <-- reference (" + TOTAL_WIDTH + "px of ')", NamedTextColor.AQUA)));

		player.sendMessage(Component.empty());
		player.sendMessage(Component.text("--- Non-default width characters ---", NamedTextColor.GOLD));
		for (char c : NON_DEFAULT_CHARS) {
			sendCharRow(player, c, false);
		}

		player.sendMessage(Component.empty());
		player.sendMessage(Component.text("--- Default-width characters (all printable ASCII not listed above) ---", NamedTextColor.GOLD));
		Set<Character> nonDefault = new HashSet<>();
		for (char c : NON_DEFAULT_CHARS) {
			nonDefault.add(c);
		}
		for (char c = 32; c <= 126; c++) {
			if (!nonDefault.contains(c)) {
				sendCharRow(player, c, false);
			}
		}

		player.sendMessage(Component.empty());
		player.sendMessage(Component.text("--- Pixel-perfect space padding ---", NamedTextColor.GOLD));
		player.sendMessage(Component.text(
			"All rows padded to " + TOTAL_WIDTH + "px. The '|' marker at right should align on every row.",
			NamedTextColor.GRAY));
		player.sendMessage(Component.text(
			"Gaps that are not a multiple of 4 require mixed bold/non-bold spaces to fill exactly.",
			NamedTextColor.GRAY));
		// Reference: exactly TOTAL_WIDTH px of non-bold spaces (TOTAL_WIDTH must be 4-multiple).
		player.sendMessage(Component.empty()
			.append(Component.text(" ".repeat(TOTAL_WIDTH / 4)).decoration(TextDecoration.BOLD, false))
			.append(Component.text("|", NamedTextColor.GOLD))
			.append(Component.text("  <-- reference (" + TOTAL_WIDTH + "px)", NamedTextColor.AQUA)));
		// Test strings chosen to hit all four gap-mod-4 remainders:
		//   "Hello"     24px -> gap 76, remainder 0 (normals only)
		//   "World"     27px -> gap 73, remainder 1 (1 bold + 18 normal)
		//   "Score"     30px -> gap 70, remainder 2 (2 bold + 15 normal)
		//   "fI"         9px -> gap 91, remainder 3 (3 bold + 19 normal)
		//   "Monumenta" 52px -> gap 48, remainder 0
		//   "leaderboard" 63px -> gap 37, remainder 1
		for (String s : new String[]{"Hello", "World", "Score", "fI", "Monumenta", "leaderboard"}) {
			int w = FontUtils.getRenderedPixelWidth(s);
			int gap = TOTAL_WIDTH - w;
			Component padded = FontUtils.padWithSpaces(Component.text(s, NamedTextColor.YELLOW), TOTAL_WIDTH);
			player.sendMessage(Component.empty()
				.append(padded)
				.append(Component.text("|", NamedTextColor.GOLD))
				.append(Component.text("  " + s + " (" + w + "px, gap=" + gap + ", r=" + (gap % 4) + ")", NamedTextColor.GRAY)));
		}

		player.sendMessage(Component.empty());
		player.sendMessage(Component.text("--- Bold: narrowest characters ---", NamedTextColor.GOLD));
		player.sendMessage(Component.text("(bold ' = 3px; bold adds 1px per glyph)", NamedTextColor.GRAY));
		// Bold reference line using bold ' (3px each)
		int boldRefCount = TOTAL_WIDTH / 3;
		player.sendMessage(Component.empty()
			.append(Component.text("'".repeat(boldRefCount), NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD))
			.append(Component.text("  <-- bold reference (" + boldRefCount * 3 + "px)", NamedTextColor.AQUA)));
		for (char c : new char[]{'\'', '|', '!', ',', '.', ':', ';', 'i', 'j', 'l', 'f', 't'}) {
			sendCharRow(player, c, true);
		}
	}

	private static void sendCharRow(Player player, char c, boolean bold) {
		// Use 2 test chars for non-bold (ruler ' = 2px) and 3 for bold (ruler ' = 3px bold),
		// so the total test width is always an exact multiple of the ruler char width.
		int rulerCharWidth = bold ? 3 : 2;
		int testCount = bold ? 3 : 2;
		int charWidth = FontUtils.getRenderedPixelWidth(String.valueOf(c)) + (bold ? 1 : 0);
		int testWidth = testCount * charWidth;
		int leftCount = LEFT_RULER / rulerCharWidth;
		int rightPx = TOTAL_WIDTH - leftCount * rulerCharWidth - testWidth;
		int rightCount = rightPx > 0 ? rightPx / rulerCharWidth : 0;
		String leftRuler = "'".repeat(leftCount);
		String rightRuler = "'".repeat(rightCount);
		String testStr = String.valueOf(c).repeat(testCount);

		String displayChar = c == '\'' ? "\\'" : String.valueOf(c);
		String label = String.format("'%s' declared=%dpx%s", displayChar, charWidth, bold ? " bold" : "");

		Component row = Component.empty()
			.append(Component.text(leftRuler, NamedTextColor.DARK_GRAY))
			.append(Component.text(testStr, NamedTextColor.YELLOW))
			.append(Component.text(rightRuler, NamedTextColor.DARK_GRAY))
			.append(Component.text("  " + label, NamedTextColor.GRAY));
		if (bold) {
			row = row.decorate(TextDecoration.BOLD);
		}
		player.sendMessage(row);
	}
}
