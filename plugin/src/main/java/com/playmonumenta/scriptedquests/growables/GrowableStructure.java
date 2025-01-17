package com.playmonumenta.scriptedquests.growables;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

/**
 * Encapsulates a growable structure that could be grown later.
 */
public class GrowableStructure {

	private static class GrowableElement {
		private final int mDX;
		private final int mDY;
		private final int mDZ;
		private final int mDepth;
		private final BlockData mData;
		private final List<BlockData> mExclude;
		private final List<BlockData> mInclude;

		private GrowableElement(int dx, int dy, int dz, int depth, BlockData data) {
			mDX = dx;
			mDY = dy;
			mDZ = dz;
			mDepth = depth;
			mData = data;
			mExclude = new ArrayList<>();
			mInclude = new ArrayList<>();
		}

		private GrowableElement(JsonObject obj) throws Exception {
			mDX = obj.get("dx").getAsInt();
			mDY = obj.get("dy").getAsInt();
			mDZ = obj.get("dz").getAsInt();
			mDepth = obj.get("depth").getAsInt();
			mData = Bukkit.getServer().createBlockData(obj.get("data").getAsString());
			mExclude = new ArrayList<>();
			mInclude = new ArrayList<>();
			if (obj.get("excludes") != null) {
				JsonArray blocks = obj.get("excludes").getAsJsonArray();
				for (JsonElement block : blocks) {
					BlockData type = Bukkit.getServer().createBlockData(block.getAsString());
					mExclude.add(type);
				}
			} else if (obj.get("includes") != null) {
				JsonArray blocks = obj.get("includes").getAsJsonArray();
				for (JsonElement block : blocks) {
					BlockData type = Bukkit.getServer().createBlockData(block.getAsString());
					mInclude.add(type);
				}
			}
		}

		private Block getBlock(Location origin) {
			return origin.clone().add(mDX, mDY, mDZ).getBlock();
		}

		private List<BlockData> getExclusionList() { return mExclude; }

		private List<BlockData> getInclusionList() { return mInclude; }

		private BlockState getBlockState(Location origin) {
			BlockState state = getBlock(origin).getState();
			state.setType(mData.getMaterial());
			state.setBlockData(mData);
			return state;
		}

		protected JsonObject getAsJsonObject() {
			JsonArray array = new JsonArray();
			String type = null;
			if(!mExclude.isEmpty()) {
				type = "excludes";
				for (BlockData item : mExclude) {
					array.add(item.getAsString());
				}
			} else if (!mInclude.isEmpty()) {
				type = "includes";
				for (BlockData item : mInclude) {
					array.add(item.getAsString());
				}
			}

			JsonObject obj = new JsonObject();
			obj.addProperty("dx", mDX);
			obj.addProperty("dy", mDY);
			obj.addProperty("dz", mDZ);
			obj.addProperty("depth", mDepth);
			obj.addProperty("data", mData.getAsString());
			if (!array.isEmpty() && type != null) {
				obj.add(type, array);
			}
			return obj;
		}
	}

	// Convenience list of offsets to get adjacent blocks
	private static final List<Vector> ADJACENT_OFFSETS = Arrays.asList(
	                                                         new Vector(0, 0, 1),
	                                                         new Vector(0, 0, -1),
	                                                         new Vector(0, 1, 0),
	                                                         new Vector(0, 1, 1),
	                                                         new Vector(0, 1, -1),
	                                                         new Vector(0, -1, 0),
	                                                         new Vector(0, -1, 1),
	                                                         new Vector(0, -1, -1),
	                                                         new Vector(1, 0, 0),
	                                                         new Vector(1, 0, 1),
	                                                         new Vector(1, 0, -1),
	                                                         new Vector(1, 1, 0),
	                                                         new Vector(1, 1, 1),
	                                                         new Vector(1, 1, -1),
	                                                         new Vector(1, -1, 0),
	                                                         new Vector(1, -1, 1),
	                                                         new Vector(1, -1, -1),
	                                                         new Vector(-1, 0, 0),
	                                                         new Vector(-1, 0, 1),
	                                                         new Vector(-1, 0, -1),
	                                                         new Vector(-1, 1, 0),
	                                                         new Vector(-1, 1, 1),
	                                                         new Vector(-1, 1, -1),
	                                                         new Vector(-1, -1, 0),
	                                                         new Vector(-1, -1, 1),
	                                                         new Vector(-1, -1, -1)
	                                                     );
	private static final String METAKEY = "GrowableMetakey";

	private final String mPath;
	private final String mLabel;
	private final List<GrowableElement> mElements;

	public GrowableStructure(String growablePath, JsonObject object) throws Exception {
		mPath = growablePath;

		JsonElement label = object.get("label");
		if (label == null) {
			throw new Exception("'label' entry is required");
		}
		if (label.getAsString() == null || label.getAsString().isEmpty()) {
			throw new Exception("Failed to parse 'label' as string");
		}
		mLabel = label.getAsString();

		JsonElement elements = object.get("elements");
		if (elements == null) {
			throw new Exception("'elements' entry is required");
		}
		JsonArray array = elements.getAsJsonArray();
		if (array == null) {
			throw new Exception("Failed to parse 'elements' as JSON array");
		}

		mElements = new ArrayList<>(array.size());

		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();
			mElements.add(new GrowableElement(entry.getAsJsonObject()));
		}
	}

	public GrowableStructure(String growablePath, Location origin, String label, int maxDepth) throws Exception {
		mPath = growablePath;
		Plugin plugin = Plugin.getInstance();

		if (origin.getBlock().isEmpty()) {
			throw new Exception("Starting block is empty");
		}

		/*
		 * This implements a breadth-first search to identify the structure.
		 * All connected non-air blocks are added to the resulting list as long as they are >= the origin y height
		 */

		/* The resulting list of locations */
		List<GrowableElement> result = new ArrayList<>();

		/* The list of next locations to visit after the current work list is complete */
		List<Location> pending = new ArrayList<>();

		/* The current list of locations being processed */
		List<Location> worklist = new ArrayList<>();

		/* Add the first block to the list, and don't visit it again */
		worklist.add(origin);
		origin.getBlock().setMetadata(METAKEY, new FixedMetadataValue(plugin, true));

		int depth = 0;
		while (!worklist.isEmpty() || !pending.isEmpty()) {
			if (worklist.isEmpty()) {
				depth += 1;
				if (depth > maxDepth) {
					/* Clean up the left-behind metadata for result blocks */
					for (GrowableElement element : result) {
						element.getBlock(origin).removeMetadata(METAKEY, plugin);
					}
					/* Clean up the left-behind metadata for blocks that would be visited next */
					for (Location pendingLoc : pending) {
						pendingLoc.getBlock().removeMetadata(METAKEY, plugin);
					}

					throw new Exception("Maximum depth " + Integer.toString(maxDepth) + " exceeded");
				}
				worklist.addAll(pending);
				pending.clear();
			}

			Iterator<Location> workIter = worklist.iterator();
			while (workIter.hasNext()) {
				Location workLoc = workIter.next();
				result.add(new GrowableElement(workLoc.getBlockX() - origin.getBlockX(),
				                                workLoc.getBlockY() - origin.getBlockY(),
												workLoc.getBlockZ() - origin.getBlockZ(),
												depth, workLoc.getBlock().getBlockData()));

				/* Add adjacent non-air blocks without metadata to the pending list */
				for (Vector vec : ADJACENT_OFFSETS) {
					Location tmpLoc = workLoc.clone().add(vec);
					Block blk = tmpLoc.getBlock();
					if (!blk.getType().equals(Material.AIR) && !blk.getType().equals(Material.STRUCTURE_VOID) && !blk.hasMetadata(METAKEY)) {
						/* Visit this location on the next iteration */
						pending.add(tmpLoc);
						/* Don't visit this block again later */
						blk.setMetadata(METAKEY, new FixedMetadataValue(plugin, true));
					}
				}

				/* Done with this item */
				workIter.remove();
			}
		}

		for (GrowableElement element : result) {
			/* Clean up the left-behind metadata */
			element.getBlock(origin).removeMetadata(METAKEY, plugin);
		}

		mLabel = label;
		mElements = result;
	}

	public String getLabel() {
		return mLabel;
	}

	public int getSize() {
		return mElements.size();
	}

	public GrowableProgress grow(Location origin, int ticksPerStep, int blocksPerStep, boolean callStructureGrowEvent) {
		List<BlockState> states = new ArrayList<>(mElements.size());

		for (GrowableElement element : mElements) {
			if (!element.getExclusionList().isEmpty()) {
				if (!element.getExclusionList().contains(element.getBlock(origin).getBlockData())) {
					states.add(element.getBlockState(origin)); // not in exclusion list - keep
				}
			} else if (!element.getInclusionList().isEmpty()) {
				if (element.getInclusionList().contains(element.getBlock(origin).getBlockData())) {
					states.add(element.getBlockState(origin)); // in inclusion list - keep
				}
			} else {
				states.add(element.getBlockState(origin));
			}
		}

		return new GrowableProgress(states, origin, ticksPerStep, blocksPerStep, callStructureGrowEvent);
	}

	public JsonElement getAsJsonObject() {
		JsonArray array = new JsonArray();
		for (GrowableElement element : mElements) {
			array.add(element.getAsJsonObject());
		}

		JsonObject obj = new JsonObject();
		obj.addProperty("label", mLabel);
		obj.add("elements", array);

		return obj;
	}

	public String getPath() {
		return mPath;
	}
}
