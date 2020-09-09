package com.playmonumenta.scriptedquests.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
/*
ChatMessageType contains the following:
ACTION_BAR
CHAT
SYSTEM

https://ci.md-5.net/job/BungeeCord/ws/chat/target/apidocs/net/md_5/bungee/api/ChatMessageType.html
*/
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
// https://www.spigotmc.org/wiki/the-chat-component-api/

public class MessagingUtils {
	public static String translatePlayerName(Player player, String message) {
		return message.replaceAll("@S", player.getName());
	}

	public static void sendActionBarMessage(Player player, String message) {
		sendActionBarMessage(player, ChatColor.YELLOW, false, message);
	}

	public static void sendActionBarMessage(Player player, ChatColor color, boolean bold, String message) {
		message = translatePlayerName(player, message);
		TextComponent formattedMessage = new TextComponent(TextComponent.fromLegacyText(message));
		formattedMessage.setColor(color);
		formattedMessage.setBold(bold);
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, formattedMessage);
	}

	public static void sendNPCMessage(Player player, String displayName, String message) {
		message = ChatColor.translateAlternateColorCodes('&',translatePlayerName(player, message));
		TextComponent formattedMessage = new TextComponent(TextComponent.fromLegacyText(displayName));
		formattedMessage.setColor(ChatColor.GOLD);
		TextComponent semiColon = new TextComponent(TextComponent.fromLegacyText(": "));
		semiColon.setColor(ChatColor.YELLOW);
		formattedMessage.addExtra(semiColon);
		TextComponent tempText = new TextComponent(TextComponent.fromLegacyText(message));
		tempText.setColor(ChatColor.WHITE);
		formattedMessage.addExtra(tempText);

		BaseComponent[] toDisplay = new BaseComponent[1];
		toDisplay[0] = formattedMessage;

		player.spigot().sendMessage(ChatMessageType.SYSTEM,toDisplay);
	}

	public static void sendRawMessage(Player player, String message) {
		message = translatePlayerName(player, message);
		String noAlternateColorCodes = ChatColor.translateAlternateColorCodes('&',message);
		player.spigot().sendMessage(ChatMessageType.SYSTEM, TextComponent.fromLegacyText(noAlternateColorCodes));
	}

	public static void sendClickableNPCMessage(Plugin plugin, Player player, String message,
	                                           String commandStr) {
		message = translatePlayerName(player, message);
		TextComponent formattedMessage = new TextComponent(TextComponent.fromLegacyText(">> " + message));
		formattedMessage.setColor(ChatColor.of("#6cc0eb"));
		formattedMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandStr));

		BaseComponent[] toDisplay = new BaseComponent[1];
		toDisplay[0] = formattedMessage;

		player.spigot().sendMessage(ChatMessageType.SYSTEM,toDisplay);
	}

	public static void sendStackTrace(CommandSender sender, Exception e) {
		TextComponent formattedMessage;
		String errorMessage = e.getLocalizedMessage();
		if (errorMessage != null) {
			formattedMessage = new TextComponent(TextComponent.fromLegacyText(errorMessage));
		} else {
			formattedMessage = new
			TextComponent("An error occured without a set message. Hover for stack trace.");
		}
		formattedMessage.setColor(ChatColor.RED);

		// Get the first 300 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		sStackTrace = sStackTrace.substring(0, Math.min(sStackTrace.length(), 300));

		BaseComponent[] textStackTrace = new ComponentBuilder(sStackTrace.replace("\t",
		                                                      "  ")).color(ChatColor.RED).create();
		formattedMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, textStackTrace));

		BaseComponent[] toDisplay = new BaseComponent[1];
		toDisplay[0] = formattedMessage;

		sender.spigot().sendMessage(toDisplay);
	}
}
