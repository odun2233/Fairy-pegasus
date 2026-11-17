package com.fairypegasus.client;

import com.fairypegasus.entity.FairyPegasusEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class FairyPegasusRenderer extends GeoEntityRenderer<FairyPegasusEntity> {
	public FairyPegasusRenderer(EntityRendererFactory.Context context) {
		super(context, new FairyPegasusModel());
		this.shadowRadius = 0.8f;
		// Glowing parts come from fairy_pegasus_glowmask.png
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	/** Semi see-through wings need the translucent render type. */
	@Override
	public RenderLayer getRenderType(FairyPegasusEntity animatable, Identifier texture, VertexConsumerProvider bufferSource, float partialTick) {
		return RenderLayer.getEntityTranslucent(texture);
	}

	/** Show the saddle only when the pegasus wears one. */
	@Override
	public void preRender(MatrixStack poseStack, FairyPegasusEntity animatable, BakedGeoModel model,
						  VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
						  float partialTick, int packedLight, int packedOverlay,
						  float red, float green, float blue, float alpha) {
		model.getBone("saddle").ifPresent(bone -> bone.setHidden(!animatable.isSaddled()));
		super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
				partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
