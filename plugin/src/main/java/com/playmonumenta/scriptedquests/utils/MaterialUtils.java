package com.playmonumenta.scriptedquests.utils;

import java.util.EnumSet;
import org.bukkit.Material;

public class MaterialUtils {
	// List of blocks that can be interacted with using right click and generally perform some functionality
	private static final EnumSet<Material> interactableBlocks = EnumSet.of(
		Material.ACACIA_BUTTON,
		Material.ACACIA_DOOR,
		Material.ACACIA_FENCE_GATE,
		Material.ACACIA_TRAPDOOR,
		Material.ANVIL,
		Material.CHIPPED_ANVIL,
		Material.DAMAGED_ANVIL,
		Material.BEACON,
		Material.BIRCH_BUTTON,
		Material.BIRCH_DOOR,
		Material.BIRCH_FENCE_GATE,
		Material.BIRCH_TRAPDOOR,
		Material.BLACK_BED,
		Material.BLACK_SHULKER_BOX,
		Material.BLUE_BED,
		Material.BLUE_SHULKER_BOX,
		Material.BREWING_STAND,
		Material.BROWN_BED,
		Material.BROWN_SHULKER_BOX,
		Material.CAKE,
		Material.CAULDRON,
		Material.CHAIN_COMMAND_BLOCK,
		Material.CHEST,
		Material.CHIPPED_ANVIL,
		Material.COMMAND_BLOCK,
		Material.COMPARATOR,
		Material.CRAFTING_TABLE,
		Material.CYAN_BED,
		Material.CYAN_SHULKER_BOX,
		Material.DAMAGED_ANVIL,
		Material.DARK_OAK_BUTTON,
		Material.DARK_OAK_DOOR,
		Material.DARK_OAK_FENCE_GATE,
		Material.DARK_OAK_TRAPDOOR,
		Material.DISPENSER,
		Material.DRAGON_EGG,
		Material.DROPPER,
		Material.ENCHANTING_TABLE,
		Material.ENDER_CHEST,
		Material.FLOWER_POT,
		Material.FURNACE,
		Material.GRAY_BED,
		Material.GRAY_SHULKER_BOX,
		Material.GREEN_BED,
		Material.GREEN_SHULKER_BOX,
		Material.HOPPER,
		Material.JUKEBOX,
		Material.JUNGLE_BUTTON,
		Material.JUNGLE_DOOR,
		Material.JUNGLE_FENCE_GATE,
		Material.JUNGLE_TRAPDOOR,
		Material.LEVER,
		Material.LIGHT_BLUE_BED,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.LIGHT_GRAY_BED,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.LIME_BED,
		Material.LIME_SHULKER_BOX,
		Material.MAGENTA_BED,
		Material.MAGENTA_SHULKER_BOX,
		Material.NOTE_BLOCK,
		Material.OAK_BUTTON,
		Material.OAK_DOOR,
		Material.OAK_FENCE_GATE,
		Material.OAK_TRAPDOOR,
		Material.ORANGE_BED,
		Material.ORANGE_SHULKER_BOX,
		Material.PINK_BED,
		Material.PINK_SHULKER_BOX,
		Material.POTTED_ACACIA_SAPLING,
		Material.POTTED_ALLIUM,
		Material.POTTED_AZURE_BLUET,
		Material.POTTED_BIRCH_SAPLING,
		Material.POTTED_BLUE_ORCHID,
		Material.POTTED_BROWN_MUSHROOM,
		Material.POTTED_CACTUS,
		Material.POTTED_DANDELION,
		Material.POTTED_DARK_OAK_SAPLING,
		Material.POTTED_DEAD_BUSH,
		Material.POTTED_FERN,
		Material.POTTED_JUNGLE_SAPLING,
		Material.POTTED_OAK_SAPLING,
		Material.POTTED_ORANGE_TULIP,
		Material.POTTED_OXEYE_DAISY,
		Material.POTTED_PINK_TULIP,
		Material.POTTED_POPPY,
		Material.POTTED_RED_MUSHROOM,
		Material.POTTED_RED_TULIP,
		Material.POTTED_SPRUCE_SAPLING,
		Material.POTTED_WHITE_TULIP,
		Material.PUMPKIN,
		Material.PURPLE_BED,
		Material.PURPLE_SHULKER_BOX,
		Material.RED_BED,
		Material.RED_SHULKER_BOX,
		Material.REPEATER,
		Material.REPEATING_COMMAND_BLOCK,
		Material.SHULKER_BOX,
		Material.SPRUCE_BUTTON,
		Material.SPRUCE_DOOR,
		Material.SPRUCE_FENCE_GATE,
		Material.SPRUCE_TRAPDOOR,
		Material.STONE_BUTTON,
		Material.STRUCTURE_BLOCK,
		Material.TNT,
		Material.TRAPPED_CHEST,
		Material.WHITE_BED,
		Material.WHITE_SHULKER_BOX,
		Material.YELLOW_BED,
		Material.YELLOW_SHULKER_BOX,
		Material.LOOM,
		Material.BARREL,
		Material.SMOKER,
		Material.BLAST_FURNACE,
		Material.CARTOGRAPHY_TABLE,
		Material.FLETCHING_TABLE,
		Material.GRINDSTONE,
		Material.SMITHING_TABLE,
		Material.STONECUTTER,
		Material.BELL,
		Material.COMPOSTER
	);

	public static final EnumSet<Material> shulkerTypes = EnumSet.of(
		Material.BLACK_SHULKER_BOX,
		Material.BLUE_SHULKER_BOX,
		Material.BROWN_SHULKER_BOX,
		Material.CYAN_SHULKER_BOX,
		Material.GRAY_SHULKER_BOX,
		Material.GREEN_SHULKER_BOX,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.LIME_SHULKER_BOX,
		Material.MAGENTA_SHULKER_BOX,
		Material.ORANGE_SHULKER_BOX,
		Material.PINK_SHULKER_BOX,
		Material.PURPLE_SHULKER_BOX,
		Material.RED_SHULKER_BOX,
		Material.SHULKER_BOX,
		Material.WHITE_SHULKER_BOX,
		Material.YELLOW_SHULKER_BOX
	);

	public static boolean isInteractableBlock(Material material) {
		return interactableBlocks.contains(material);
	}

	/**
	 * Same as {@link Material#isOccluding()}, but fixes some bugs.
	 */
	public static boolean isOccluding(Material type) {
		return switch (type) {
			case REDSTONE_BLOCK -> true;
			case BARRIER -> false;
			default -> type.isOccluding();
		};
	}

}
