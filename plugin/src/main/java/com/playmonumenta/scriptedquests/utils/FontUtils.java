package com.playmonumenta.scriptedquests.utils;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

public class FontUtils {
	private static final int DEFAULT_WIDTH = 6;
	private static final int[] CHAR_WIDTHS = new int[128];

	static {
		Arrays.fill(CHAR_WIDTHS, DEFAULT_WIDTH);
		CHAR_WIDTHS[' '] = 4;
		CHAR_WIDTHS['!'] = 2;
		CHAR_WIDTHS['"'] = 4;
		CHAR_WIDTHS['('] = 4;
		CHAR_WIDTHS[')'] = 4;
		CHAR_WIDTHS['*'] = 4;
		CHAR_WIDTHS[','] = 2;
		CHAR_WIDTHS['.'] = 2;
		CHAR_WIDTHS[':'] = 2;
		CHAR_WIDTHS[';'] = 2;
		CHAR_WIDTHS['<'] = 5;
		CHAR_WIDTHS['>'] = 5;
		CHAR_WIDTHS['@'] = 7;
		CHAR_WIDTHS['I'] = 4;
		CHAR_WIDTHS['['] = 4;
		CHAR_WIDTHS['\''] = 2;
		CHAR_WIDTHS[']'] = 4;
		CHAR_WIDTHS['`'] = 3;
		CHAR_WIDTHS['f'] = 5;
		CHAR_WIDTHS['i'] = 2;
		CHAR_WIDTHS['k'] = 5;
		CHAR_WIDTHS['l'] = 3;
		CHAR_WIDTHS['t'] = 4;
		CHAR_WIDTHS['{'] = 4;
		CHAR_WIDTHS['|'] = 2;
		CHAR_WIDTHS['}'] = 4;
		CHAR_WIDTHS['~'] = 7;
	}

	private static int charWidth(char c) {
		return (c < CHAR_WIDTHS.length) ? CHAR_WIDTHS[c] : DEFAULT_WIDTH;
	}

	/** Returns the rendered pixel width of a plain string in the default Minecraft font. */
	public static int getRenderedPixelWidth(String s) {
		int width = 0;
		for (int i = 0; i < s.length(); i++) {
			width += charWidth(s.charAt(i));
		}
		return width;
	}

	/**
	 * Returns the rendered pixel width of a Component, recursing into children.
	 * Bold text is 1px wider per character; bold state inherits from parent nodes.
	 */
	public static int getRenderedPixelWidth(Component component) {
		return getRenderedPixelWidthHelper(component, false);
	}

	/** Pads {@code s} with trailing spaces to reach at least {@code targetWidth} px. */
	public static String padWithSpaces(String s, int targetWidth) {
		return pad(s, targetWidth, ' ');
	}

	/**
	 * Pads {@code component} with trailing spaces to reach at least {@code targetWidth} px.
	 * Uses pixel-perfect space filling (see {@link #pad(Component, int, Component)}).
	 */
	public static Component padWithSpaces(Component component, int targetWidth) {
		return pad(component, targetWidth, Component.text(" "));
	}

	/** Pads {@code s} with trailing {@code padChar} characters to reach at least {@code targetWidth} px. */
	public static String pad(String s, int targetWidth, char padChar) {
		int currentWidth = getRenderedPixelWidth(s);
		if (currentWidth >= targetWidth) {
			return s;
		}
		int padWidth = charWidth(padChar);
		StringBuilder sb = new StringBuilder(s);
		while (currentWidth < targetWidth) {
			sb.append(padChar);
			currentWidth += padWidth;
		}
		return sb.toString();
	}

	/**
	 * Appends copies of {@code padWith} to {@code component} to reach at least {@code targetWidth} px.
	 * If {@code padWith} is a plain-text Component containing only space characters, mixes non-bold
	 * (4px) and bold (5px) spaces for pixel-perfect fills. Otherwise repeats {@code padWith} as-is,
	 * reaching the closest multiple of its width without overshoot.
	 */
	public static Component pad(Component component, int targetWidth, Component padWith) {
		int currentWidth = getRenderedPixelWidth(component);
		if (currentWidth >= targetWidth) {
			return component;
		}
		return component.append(buildPadding(targetWidth - currentWidth, padWith));
	}

	/** Centers {@code s} within {@code targetWidth} px using space padding. */
	public static String center(String s, int targetWidth) {
		return center(s, targetWidth, ' ');
	}

	/** Centers {@code s} within {@code targetWidth} px using {@code padChar} padding. */
	public static String center(String s, int targetWidth, char padChar) {
		int currentWidth = getRenderedPixelWidth(s);
		if (currentWidth >= targetWidth) {
			return s;
		}
		int totalPads = (targetWidth - currentWidth) / charWidth(padChar);
		int leftPads = totalPads / 2;
		int rightPads = totalPads - leftPads;
		String pad = String.valueOf(padChar);
		return pad.repeat(leftPads) + s + pad.repeat(rightPads);
	}

	/**
	 * Centers {@code component} within {@code targetWidth} px using space padding.
	 * Uses pixel-perfect space filling (see {@link #center(Component, int, Component)}).
	 */
	public static Component center(Component component, int targetWidth) {
		return center(component, targetWidth, Component.text(" "));
	}

	/**
	 * Centers {@code component} within {@code targetWidth} px using {@code padWith} padding.
	 * If {@code padWith} is a plain-text Component containing only space characters, mixes non-bold
	 * (4px) and bold (5px) spaces for pixel-perfect fills. Otherwise repeats {@code padWith} as-is,
	 * reaching the closest multiple of its width without overshoot.
	 */
	public static Component center(Component component, int targetWidth, Component padWith) {
		int currentWidth = getRenderedPixelWidth(component);
		int gap = targetWidth - currentWidth;
		if (gap <= 0) {
			return component;
		}
		int leftGap = gap / 2;
		int rightGap = gap - leftGap;
		return Component.empty()
			.append(buildPadding(leftGap, padWith))
			.append(component)
			.append(buildPadding(rightGap, padWith));
	}

	/** Right-aligns {@code s} within {@code targetWidth} px using space padding. */
	public static String alignRight(String s, int targetWidth) {
		return alignRight(s, targetWidth, ' ');
	}

	/** Right-aligns {@code s} within {@code targetWidth} px using {@code padChar} padding. */
	public static String alignRight(String s, int targetWidth, char padChar) {
		int currentWidth = getRenderedPixelWidth(s);
		if (currentWidth >= targetWidth) {
			return s;
		}
		int pads = (targetWidth - currentWidth) / charWidth(padChar);
		return String.valueOf(padChar).repeat(pads) + s;
	}

	/**
	 * Right-aligns {@code component} within {@code targetWidth} px using space padding.
	 * Uses pixel-perfect space filling (see {@link #alignRight(Component, int, Component)}).
	 */
	public static Component alignRight(Component component, int targetWidth) {
		return alignRight(component, targetWidth, Component.text(" "));
	}

	/**
	 * Right-aligns {@code component} within {@code targetWidth} px using {@code padWith} padding.
	 * If {@code padWith} is a plain-text Component containing only space characters, mixes non-bold
	 * (4px) and bold (5px) spaces for pixel-perfect fills. Otherwise repeats {@code padWith} as-is,
	 * reaching the closest multiple of its width without overshoot.
	 */
	public static Component alignRight(Component component, int targetWidth, Component padWith) {
		int currentWidth = getRenderedPixelWidth(component);
		int gap = targetWidth - currentWidth;
		if (gap <= 0) {
			return component;
		}
		return Component.empty()
			.append(buildPadding(gap, padWith))
			.append(component);
	}

	/**
	 * Formats a row of fixed-width columns: {@code formatRow(cell1, width1, cell2, width2, ..., lastCell)}.
	 * Each cell is padded with trailing spaces to its following pixel width.
	 * The final cell (if unpaired with a width) is appended as-is.
	 * Cells may be {@link Component} or any type (converted via {@code String.valueOf}).
	 */
	public static Component formatRow(Object... args) {
		Component result = Component.empty();
		for (int i = 0; i < args.length; i += 2) {
			Component cell = args[i] instanceof Component c ? c : Component.text(String.valueOf(args[i]));
			if (i + 1 < args.length) {
				result = result.append(padWithSpaces(cell, (Integer) args[i + 1]));
			} else {
				result = result.append(cell);
			}
		}
		return result;
	}

	/**
	 * Returns true if {@code pad} is a plain TextComponent whose content consists entirely of
	 * space characters (no children, no other characters). Such components get pixel-perfect
	 * treatment via fillSpaceGap rather than simple repetition.
	 */
	private static boolean isSpacePad(Component pad) {
		return pad instanceof TextComponent tc
			&& tc.children().isEmpty()
			&& !tc.content().isEmpty()
			&& tc.content().chars().allMatch(c -> c == ' ');
	}

	/**
	 * Dispatches to fillSpaceGap for space-only pads (pixel-perfect), or repeats the
	 * pad Component as many times as fits within {@code gap} for all other pads.
	 */
	private static Component buildPadding(int gap, Component padWith) {
		if (isSpacePad(padWith)) {
			return fillSpaceGap(gap);
		}
		int padWidth = getRenderedPixelWidth(padWith);
		if (padWidth <= 0) {
			return Component.empty();
		}
		int count = gap / padWidth;
		Component result = Component.empty();
		for (int i = 0; i < count; i++) {
			result = result.append(padWith);
		}
		return result;
	}

	/**
	 * Fills exactly (or as close as possible to, without exceeding) {@code gap} pixels using
	 * a mix of non-bold spaces (4px each) and bold spaces (5px each).
	 *
	 * Exact fills are always possible for gap >= 20px (the Frobenius number of 4 and 5 is 11),
	 * and for most smaller values too. For the few small values that cannot be filled exactly
	 * (1, 2, 3, 6, 7, 11 px), the method fills as many pixels as possible without overshoot.
	 *
	 * Math: bold width (5) % normal width (4) == 1, so b*5 % 4 == b % 4. To make the total
	 * width (4*a + 5*b) divisible by 4 with remainder gap%4, we need b = gap % 4. If the
	 * resulting a = (gap - 5*b) / 4 is negative (gap is too small), a short brute-force loop
	 * over at most gap/5 values finds the best fit.
	 */
	private static Component fillSpaceGap(int gap) {
		if (gap <= 0) {
			return Component.empty();
		}
		// Try exact solution: b bold spaces + a normal spaces.
		int b = gap % 4;
		int rem = gap - 5 * b;
		int a;
		if (rem >= 0) {
			a = rem / 4;
		} else {
			// gap too small for exact formula; find closest fill by brute force.
			// Only reached when gap < 20 (Frobenius number of 4,5 is 11), so loop is short.
			int bestFill = 0;
			a = 0;
			b = 0;
			for (int bi = gap / 5; bi >= 0; bi--) {
				int ai = (gap - 5 * bi) / 4;
				int fill = 5 * bi + 4 * ai;
				if (fill > bestFill) {
					bestFill = fill;
					a = ai;
					b = bi;
				}
				if (fill == gap) {
					break;
				}
			}
			if (bestFill == 0) {
				return Component.empty();
			}
		}
		Component result = Component.empty();
		if (b > 0) {
			result = result.append(Component.text(" ".repeat(b)).decoration(TextDecoration.BOLD, true));
		}
		if (a > 0) {
			result = result.append(Component.text(" ".repeat(a)).decoration(TextDecoration.BOLD, false));
		}
		return result;
	}

	private static int getRenderedPixelWidthHelper(Component component, boolean parentBold) {
		TextDecoration.State boldState = component.style().decoration(TextDecoration.BOLD);
		boolean bold = boldState == TextDecoration.State.TRUE || (boldState != TextDecoration.State.FALSE && parentBold);
		int width = 0;
		if (component instanceof TextComponent tc) {
			String text = tc.content();
			for (int i = 0; i < text.length(); i++) {
				width += charWidth(text.charAt(i)) + (bold ? 1 : 0);
			}
		}
		for (Component child : component.children()) {
			width += getRenderedPixelWidthHelper(child, bold);
		}
		return width;
	}
}
