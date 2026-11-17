package com.fairypegasus.entity.ai;

import com.fairypegasus.entity.FairyPegasusEntity;
import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;

/** While the pegasus is in STAY mode it does not walk anywhere. */
public class PegasusStayGoal extends Goal {
	private final FairyPegasusEntity pegasus;

	public PegasusStayGoal(FairyPegasusEntity pegasus) {
		this.pegasus = pegasus;
		this.setControls(EnumSet.of(Control.MOVE, Control.JUMP));
	}

	@Override
	public boolean canStart() {
		return pegasus.getMode() == FairyPegasusEntity.MODE_STAY && !pegasus.hasPassengers();
	}

	@Override
	public boolean shouldContinue() {
		return canStart();
	}

	@Override
	public void start() {
		pegasus.getNavigation().stop();
	}

	@Override
	public void tick() {
		pegasus.getNavigation().stop();
	}
}
