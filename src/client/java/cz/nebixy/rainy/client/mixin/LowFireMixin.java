package cz.nebixy.rainy.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import cz.nebixy.rainy.client.module.ModuleManager;

/**
 * Low Fire: oheň před očima, když hoříš, je níž a nezakrývá výhled.
 */
@Mixin(ScreenEffectRenderer.class)
public class LowFireMixin {
	private static final float OFFSET = 0.3f;

	@Inject(method = "submitFire", at = @At("HEAD"))
	private static void rainy$lowFireStart(PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (ModuleManager.lowFire) {
			pose.translate(0f, -OFFSET, 0f);
		}
	}

	@Inject(method = "submitFire", at = @At("RETURN"))
	private static void rainy$lowFireEnd(PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (ModuleManager.lowFire) {
			pose.translate(0f, OFFSET, 0f); // vrátit, ať se neposune nic dalšího
		}
	}
}
