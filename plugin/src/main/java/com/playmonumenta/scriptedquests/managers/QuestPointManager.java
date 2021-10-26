package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.models.Model;
import com.playmonumenta.scriptedquests.models.ModelInstance;
import com.playmonumenta.scriptedquests.models.ModelTreeNode;
import com.playmonumenta.scriptedquests.quests.points.QuestPoint;
import com.playmonumenta.scriptedquests.quests.points.QuestPointTreeNode;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import me.Novalescent.utils.quadtree.reworked.QuadTree;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.*;

public class QuestPointManager {
	private final ArrayList<QuestPoint> mPoints = new ArrayList<>();
	private final Map<UUID, QuadTree<QuestPointTreeNode>> mQuadTrees = new HashMap<>();

	/* If sender is non-null, it will be sent debugging information */
	public void reload(Plugin plugin, CommandSender sender) {
		mPoints.clear();
		destroyPoints();

		QuestUtils.loadScriptedQuests(plugin, "points", sender, (object) -> {

			QuestPoint point = new QuestPoint(plugin, object);
			mPoints.add(point);

			QuestPointTreeNode node = new QuestPointTreeNode(point);
			UUID uuid = node.getLocation().getWorld().getUID();
			QuadTree<QuestPointTreeNode> quadTree = mQuadTrees.get(uuid);
			if (quadTree == null) {
				quadTree = new QuadTree<>();
				mQuadTrees.put(uuid, quadTree);
			}

			quadTree.add(node);
			quadTree.getValues().add(node);
			return null;
		});
	}

	/**
	 * Destroys all QuestPoint QuadTrees
	 * Effect: {@code mQuadTrees} will be cleared and its {@code QuadTree} values will be destroyed
	 */
	private void destroyPoints() {
		for (QuadTree<QuestPointTreeNode> quadTree : mQuadTrees.values()) {
			quadTree.destroy();
		}

		mQuadTrees.clear();
	}

	/**
	 * Gets all points near the specified location in a 32x32 block radius
	 * @param loc The origin to search around
	 * @return The points within radius
	 */
	public List<QuestPoint> getPointsNearby(Location loc) {
		List<QuestPoint> points = new ArrayList<>();

		QuadTree<QuestPointTreeNode> quadTree = mQuadTrees.get(loc.getWorld().getUID());
		if (quadTree != null) {
			for (QuestPointTreeNode node : quadTree.searchNearby(loc, 32, false)) {
				points.add(node.getPoint());
			}
		}

		return points;
	}

}
