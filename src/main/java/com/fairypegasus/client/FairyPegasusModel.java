package com.fairypegasus.client;

import com.fairypegasus.FairyPegasusMod;
import com.fairypegasus.entity.FairyPegasusEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class FairyPegasusModel extends GeoModel<FairyPegasusEntity> {
	@Override
	public Identifier getModelResource(FairyPegasusEntity animatable) {
		return new Identifier(FairyPegasusMod.MOD_ID, "geo/fairy_pegasus.geo.json");
	}

	@Override
	public Identifier getTextureResource(FairyPegasusEntity animatable) {
		return new Identifier(FairyPegasusMod.MOD_ID, "textures/entity/fairy_pegasus.png");
	}

	@Override
	public Identifier getAnimationResource(FairyPegasusEntity animatable) {
		return new Identifier(FairyPegasusMod.MOD_ID, "animations/fairy_pegasus.animation.json");
	}
}
