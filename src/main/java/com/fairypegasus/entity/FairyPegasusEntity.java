package com.fairypegasus.entity;

import com.fairypegasus.FlightInput;
import com.fairypegasus.entity.ai.PegasusFollowGoal;
import com.fairypegasus.entity.ai.PegasusStayGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FairyPegasusEntity extends AbstractHorseEntity implements GeoEntity {
	public static final int MODE_FREE = 0;
	public static final int MODE_FOLLOW = 1;
	public static final int MODE_STAY = 2;

	private static final TrackedData<Integer> MODE =
			DataTracker.registerData(FairyPegasusEntity.class, TrackedDataHandlerRegistry.INTEGER);

	private static final double FLY_SPEED = 0.45;
	private static final double CLIMB_SPEED = 0.35;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.fairy_pegasus.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.fairy_pegasus.walk");
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.fairy_pegasus.fly");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public FairyPegasusEntity(EntityType<? extends FairyPegasusEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createPegasusAttributes() {
		return createBaseHorseAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
	}

	// ---------- data / saving ----------

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(MODE, MODE_FREE);
	}

	public int getMode() {
		return this.dataTracker.get(MODE);
	}

	public void setMode(int mode) {
		this.dataTracker.set(MODE, mode);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("PegasusMode", getMode());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setMode(nbt.getInt("PegasusMode"));
	}

	// ---------- taming / owner ----------

	public void tameTo(PlayerEntity player) {
		this.setTame(true);
		this.setOwnerUuid(player.getUuid());
		this.setHealth(this.getMaxHealth());
	}

	public PlayerEntity getOwnerPlayer() {
		if (this.getOwnerUuid() == null) {
			return null;
		}
		return this.getWorld().getPlayerByUuid(this.getOwnerUuid());
	}

	// ---------- goals ----------

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new PegasusStayGoal(this));
		this.goalSelector.add(2, new PegasusFollowGoal(this, 1.2));
		this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.7));
		this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
		this.goalSelector.add(7, new LookAroundGoal(this));
	}

	// ---------- interaction ----------

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);

		// Sneak + right-click with an empty hand: switch follow / stay.
		if (player.isSneaking() && stack.isEmpty() && hand == Hand.MAIN_HAND
				&& this.isTame() && player.getUuid().equals(this.getOwnerUuid())) {
			if (!this.getWorld().isClient) {
				if (getMode() == MODE_FOLLOW) {
					setMode(MODE_STAY);
					this.getNavigation().stop();
					player.sendMessage(Text.translatable("message.fairypegasus.stay"), true);
				} else {
					setMode(MODE_FOLLOW);
					player.sendMessage(Text.translatable("message.fairypegasus.follow"), true);
				}
			}
			return ActionResult.success(this.getWorld().isClient);
		}

		// A diamond tames a wild pegasus straight away.
		if (!this.isTame() && stack.isOf(Items.DIAMOND)) {
			if (!this.getWorld().isClient) {
				this.tameTo(player);
				if (!player.getAbilities().creativeMode) {
					stack.decrement(1);
				}
				this.getWorld().sendEntityStatus(this, (byte) 7);
			}
			return ActionResult.success(this.getWorld().isClient);
		}

		return super.interactMob(player, hand);
	}

	// ---------- flying (helicopter style) ----------

	private boolean isFlyingRide() {
		return this.isSaddled() && this.getControllingPassenger() instanceof PlayerEntity;
	}

	@Override
	public void tick() {
		super.tick();

		boolean ridden = isFlyingRide();
		if (this.hasNoGravity() != ridden) {
			this.setNoGravity(ridden);
		}
		if (ridden) {
			this.fallDistance = 0.0f;
		}

		// Sparkles around the wings (client only)
		if (this.getWorld().isClient && this.random.nextInt(6) == 0) {
			this.getWorld().addParticle(ParticleTypes.END_ROD,
					this.getParticleX(1.0), this.getRandomBodyY() + 0.3, this.getParticleZ(1.0),
					0.0, 0.01, 0.0);
		}
	}

	@Override
	public void travel(Vec3d movementInput) {
		if (this.isAlive() && isFlyingRide() && this.getControllingPassenger() instanceof PlayerEntity rider) {
			// Face where the rider looks.
			this.setYaw(rider.getYaw());
			this.prevYaw = this.getYaw();
			this.setPitch(rider.getPitch() * 0.5f);
			this.setRotation(this.getYaw(), this.getPitch());
			this.bodyYaw = this.getYaw();
			this.headYaw = this.bodyYaw;

			// Only the rider's own game computes the movement.
			if (this.isLogicalSideForUpdatingMovement()) {
				float strafe = rider.sidewaysSpeed * 0.5f;
				float forward = rider.forwardSpeed;
				if (forward <= 0.0f) {
					forward *= 0.25f;
				}
				double yawRad = Math.toRadians(this.getYaw());
				double sin = Math.sin(yawRad);
				double cos = Math.cos(yawRad);
				double targetX = (-sin * forward + cos * strafe) * FLY_SPEED;
				double targetZ = (cos * forward + sin * strafe) * FLY_SPEED;
				double targetY = this.getWorld().isClient ? FlightInput.vertical * CLIMB_SPEED : 0.0;

				Vec3d current = this.getVelocity();
				Vec3d target = new Vec3d(targetX, targetY, targetZ);
				this.setVelocity(current.lerp(target, 0.2));
				this.move(MovementType.SELF, this.getVelocity());
			}
			return;
		}
		super.travel(movementInput);
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	public double getMountedHeightOffset() {
		return 1.3;
	}

	// ---------- misc ----------

	@Override
	public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return null;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_HORSE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_HORSE_DEATH;
	}

	// ---------- animations (GeckoLib) ----------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "main", 5, this::animationPredicate));
	}

	private PlayState animationPredicate(AnimationState<FairyPegasusEntity> state) {
		if (this.hasPassengers() && !this.isOnGround()) {
			return state.setAndContinue(FLY);
		}
		if (state.isMoving()) {
			return state.setAndContinue(WALK);
		}
		return state.setAndContinue(IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
