package com.fairypegasus;

import com.fairypegasus.entity.FairyPegasusEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class FairyPegasusMod implements ModInitializer {
	public static final String MOD_ID = "fairypegasus";

	public static EntityType<FairyPegasusEntity> FAIRY_PEGASUS;
	public static Item FAIRY_PEGASUS_SPAWN_EGG;

	@Override
	public void onInitialize() {
		FAIRY_PEGASUS = Registry.register(
				Registries.ENTITY_TYPE,
				new Identifier(MOD_ID, "fairy_pegasus"),
				FabricEntityTypeBuilder.<FairyPegasusEntity>createMob()
						.spawnGroup(SpawnGroup.CREATURE)
						.entityFactory(FairyPegasusEntity::new)
						.dimensions(EntityDimensions.fixed(1.4f, 1.6f))
						.trackRangeBlocks(10)
						.build());

		FabricDefaultAttributeRegistry.register(FAIRY_PEGASUS, FairyPegasusEntity.createPegasusAttributes());

		FAIRY_PEGASUS_SPAWN_EGG = Registry.register(
				Registries.ITEM,
				new Identifier(MOD_ID, "fairy_pegasus_spawn_egg"),
				new SpawnEggItem(FAIRY_PEGASUS, 0xF3E5F5, 0x9C6BFF, new Item.Settings()));

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(FAIRY_PEGASUS_SPAWN_EGG));

		registerDiamondTransformation();
	}

	/** Right-click any normal horse with a diamond: it becomes a tamed Fairy Pegasus. */
	private void registerDiamondTransformation() {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack stack = player.getStackInHand(hand);
			if (player.isSpectator() || !stack.isOf(Items.DIAMOND)) {
				return ActionResult.PASS;
			}
			if (!(entity instanceof HorseEntity horse) || horse.isBaby()) {
				return ActionResult.PASS;
			}
			if (world.isClient) {
				return ActionResult.SUCCESS;
			}
			if (!(world instanceof ServerWorld serverWorld)) {
				return ActionResult.PASS;
			}

			FairyPegasusEntity pegasus = FAIRY_PEGASUS.create(serverWorld);
			if (pegasus == null) {
				return ActionResult.PASS;
			}

			pegasus.refreshPositionAndAngles(horse.getX(), horse.getY(), horse.getZ(), horse.getYaw(), horse.getPitch());
			if (horse.hasCustomName()) {
				pegasus.setCustomName(horse.getCustomName());
				pegasus.setCustomNameVisible(horse.isCustomNameVisible());
			}
			pegasus.setPersistent();
			pegasus.tameTo(player);
			if (horse.isSaddled()) {
				pegasus.saddle(SoundCategory.NEUTRAL);
			}

			horse.removeAllPassengers();
			serverWorld.spawnEntity(pegasus);
			horse.discard();

			if (!player.getAbilities().creativeMode) {
				stack.decrement(1);
			}

			serverWorld.spawnParticles(ParticleTypes.END_ROD, pegasus.getX(), pegasus.getY() + 1.0, pegasus.getZ(), 40, 0.6, 0.8, 0.6, 0.05);
			serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pegasus.getX(), pegasus.getY() + 1.0, pegasus.getZ(), 15, 0.6, 0.8, 0.6, 0.0);
			serverWorld.playSound(null, pegasus.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 1.0f, 1.2f);
			return ActionResult.SUCCESS;
		});
	}
}
