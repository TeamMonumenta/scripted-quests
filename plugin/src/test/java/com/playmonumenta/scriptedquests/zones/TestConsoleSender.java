package com.playmonumenta.scriptedquests.zones;

import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecated")
public class TestConsoleSender implements ConsoleCommandSender {
	@Override
	public void sendMessage(@NotNull String message) {

	}

	@Override
	public void sendMessage(@NotNull String... messages) {

	}

	@Override
	public void sendMessage(@Nullable UUID sender, @NotNull String message) {

	}

	@Override
	public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {

	}

	@Override
	public @NotNull Server getServer() {
		return null;
	}

	@Override
	public @NotNull String getName() {
		return "SERVER";
	}

	@NotNull
	@Override
	public Spigot spigot() {
		return null;
	}

	@Override
	public @NotNull Component name() {
		return Component.text("SERVER");
	}

	@Override
	public boolean isConversing() {
		return false;
	}

	@Override
	public void acceptConversationInput(@NotNull String input) {

	}

	@Override
	public boolean beginConversation(@NotNull Conversation conversation) {
		return false;
	}

	@Override
	public void abandonConversation(@NotNull Conversation conversation) {

	}

	@Override
	public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details) {

	}

	@Override
	public void sendRawMessage(@NotNull String message) {

	}

	@Override
	public void sendRawMessage(@Nullable UUID sender, @NotNull String message) {

	}

	@Override
	public boolean isPermissionSet(@NotNull String name) {
		return false;
	}

	@Override
	public boolean isPermissionSet(@NotNull Permission perm) {
		return false;
	}

	@Override
	public boolean hasPermission(@NotNull String name) {
		return false;
	}

	@Override
	public boolean hasPermission(@NotNull Permission perm) {
		return false;
	}

	@Override
	public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
		return null;
	}

	@Override
	public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
		return null;
	}

	@Override
	public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
		return null;
	}

	@Override
	public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
		return null;
	}

	@Override
	public void removeAttachment(@NotNull PermissionAttachment attachment) {

	}

	@Override
	public void recalculatePermissions() {

	}

	@Override
	public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return null;
	}

	@Override
	public boolean isOp() {
		return false;
	}

	@Override
	public void setOp(boolean value) {

	}
}
