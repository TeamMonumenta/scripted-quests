package com.playmonumenta.scriptedquests.commands;

import com.google.common.base.Preconditions;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.handbook.Category;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HandbookCommand {

	public static void register(Plugin plugin) {

		new CommandAPICommand("handbook")
			.withPermission(CommandPermission.fromString("scriptedquests.handbook"))
			.withSubcommand(new CommandAPICommand("opencategory")
				.withPermission(CommandPermission.fromString("scriptedquests.handbook.opencategory"))
				.withArguments(new PlayerArgument("who"),
					new StringArgument("name of the category"))
				.executes((sender, args) -> {

					final Player who = args.getUnchecked("who");

					final String categoryId = args.getUnchecked("name of the category");

					Preconditions.checkNotNull(who);
					Preconditions.checkNotNull(categoryId);
					final var cat = plugin.getHandbookManager().categoryByName(categoryId);
					plugin.getHandbookManager().openCategoryForPlayer(cat, who);
				}))
			.withSubcommand(new CommandAPICommand("createcategory")
				.withPermission(CommandPermission.fromString("scriptedquests.handbook.createcategory"))
				.withArguments(new StringArgument("name"), new StringArgument("parent category name"))
				.executes((sender, args) -> {
						if (!(sender instanceof Player p)) return;
						if (p.getInventory().getSize() == Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).count()) {
							p.sendMessage("Your inventory is full.");
							return;
						}
						final String name = args.getUnchecked("name");
						final String parent = args.getUnchecked("parent category name");
						final @Nullable Category parentCategory = plugin.getHandbookManager().categoryByName(parent);
						if (parentCategory == null) {
							p.sendMessage(Component.text("(!) That parent category doesn't exist.", NamedTextColor.RED));
							return;
						}
						p.sendMessage(Component.text("(!) Given you a book and quill. Please write the main page of the category and then ", NamedTextColor.GREEN).append(Component.text("SIGN IT", NamedTextColor.DARK_GREEN)).append(Component.text(" when you are finished.", NamedTextColor.GREEN)));
						p.getInventory().addItem(new ItemStack(Material.WRITABLE_BOOK));
						plugin.getBookEditor().addListener(p, bookMeta -> {
							final var pages = bookMeta.pages();
							if (pages.size() > 1) {
								p.sendMessage("This is a main page; not plural."); // todo change the string to be more better
								return;
							}
							final var page = pages.get(0);
							final var cat = new Category(name, page, parentCategory, new HashSet<>());
							plugin.getHandbookManager().createCategory(cat);
							p.sendMessage(Component.text("(!) Created category " + name + " at " + cat.toDirectoryPath(), NamedTextColor.GREEN));
							p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
							p.spawnParticle(Particle.VILLAGER_HAPPY, p.getEyeLocation(), 20);
						});
					}
				)
			)
			.withSubcommand(new CommandAPICommand("listcategories")
				.withPermission(CommandPermission.fromString("scriptedquests.handbook.listcategories"))
				.executes((commandSender, commandArguments) -> {
					plugin.getHandbookManager().getCats().forEach(x -> commandSender.sendMessage(x.name() + " at " + x.toDirectoryPath()));
				}))
			.withSubcommand(new CommandAPICommand("deletecategory")
				.withPermission(CommandPermission.fromString("scriptedquests.handbook.deletecategory"))
				.withArguments(new StringArgument("name of said category"))
				.executes((commandSender, commandArguments) -> {
					final String sacrifice = commandArguments.getUnchecked("name of said category");
					try {
						Files.walkFileTree(plugin.getHandbookManager().getRealFileLocation(plugin.getHandbookManager().categoryByName(sacrifice)), new SimpleFileVisitor<Path>() {
							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								commandSender.sendMessage("deleting file " + file);
								Files.delete(file);
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
								commandSender.sendMessage("deleting directory " + dir);
								Files.delete(dir);
								return FileVisitResult.CONTINUE;
							}
						});
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					commandSender.sendMessage("(!) It is done.");
					plugin.getHandbookManager().reload(plugin, commandSender);
				}))
			.withSubcommand(new CommandAPICommand("unlock")
				.withPermission(CommandPermission.fromString("scriptedquests.handbook.unlock"))
				.withArguments(new PlayerArgument("who"), new StringArgument("path to the entry or the category"))
				.executes((commandSender, commandArguments) -> {
						final Player who = (Player) commandArguments.get("who");
						final String path = (String) commandArguments.get("path to the entry or the category");
						Preconditions.checkNotNull(who);
						Preconditions.checkNotNull(path);
						Optional.ofNullable(plugin.getHandbookManager().categoryByPath(path)).ifPresentOrElse(x -> plugin.getHandbookManager().unlockCategory(who, x),
							() -> { // not a category, let's try entry
							Optional.ofNullable(plugin.getHandbookManager().entryByPath(path)).ifPresentOrElse(x -> plugin.getHandbookManager().unlockHandbookEntry(who, x),
								() -> { // not an entry either: invalid.
								commandSender.sendMessage("(!) That is not a valid HandbookEntry path, nor is it a valid Category path.");
							});
						});


					}

				))
			.register(plugin);
	}
}
