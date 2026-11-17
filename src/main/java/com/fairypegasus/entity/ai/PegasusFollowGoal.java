package com.fairypegasus.entity.ai;

import com.fairypegasus.entity.FairyPegasusEntity;
import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

/** While the pegasus is in FOLLOW mode it walks after its owner and teleports to them if they get far away. */
public class PegasusFollowGoal extends Goal {
	private final FairyPegasusEntity pegasus;
	private final double speed;
	private PlayerEntity owner;
	private int recalcTimer;

	public PegasusFollowGoal(FairyPegasusEntity pegasus, double speed) {
		this.pegasus = pegasus;
		this.speed = speed;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (pegasus.getMode() != FairyPegasusEntity.MODE_FOLLOW || pegasus.hasPassengers()) {
			return false;
		}
		PlayerEntity player = pegasus.getOwnerPlayer();
		if (player == null || player.isSpectator()) {
			return false;
		}
		if (pegasus.squaredDistanceTo(player) < 16.0) {
			return false;
		}
		this.owner = player;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return pegasus.getMode() == FairyPegasusEntity.MODE_FOLLOW
				&& !pegasus.hasPassengers()
				&& owner != null
				&& owner.isAlive()
				&& pegasus.squaredDistanceTo(owner) > 6.25;
	}

	@Override
	public void start() {
		recalcTimer = 0;
	}

	@Override
	public void stop() {
		owner = null;
		pegasus.getNavigation().stop();
	}

	@Override
	public void tick() {
		pegasus.getLookControl().lookAt(owner, 10.0f, 40.0f);
		if (--recalcTimer <= 0) {
			recalcTimer = 10;
			if (pegasus.squaredDistanceTo(owner) > 400.0) {
				pegasus.refreshPositionAfterTeleport(owner.getX(), owner.getY(), owner.getZ());
				pegasus.getNavigation().stop();
			} else {
				pegasus.getNavigation().startMovingTo(owner, speed);
			}
		}
	}
}
