package com.playmonumenta.scriptedquests.quests.points;

import com.playmonumenta.scriptedquests.models.ModelInstance;
import me.Novalescent.utils.quadtree.reworked.QuadTreeValue;

public class QuestPointTreeNode extends QuadTreeValue {

	private QuestPoint mPoint;
	public QuestPointTreeNode(QuestPoint point) {
		super(point.getLocation());
		mPoint = point;
	}

	public QuestPoint getPoint() {
		return mPoint;
	}

	@Override
	public void destroy() {

	}

	@Override
	public QuestPointTreeNode clone() {
		QuestPointTreeNode clone = new QuestPointTreeNode(mPoint.clone());

		return clone;
	}
}
