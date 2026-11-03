package cz.nebixy.rainy.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;

import cz.nebixy.rainy.client.module.FullbrightModule;

/**
 * Fullbright: po tom, co Minecraft spočítá světlo, nastavíme efekt nočního vidění na 100 %.
 * Funguje to jako Night Vision (všude světlo, i v jeskyni), ale bez ikonky efektu a bez změny jasu.
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {
	@Inject(method = "extract", at = @At("RETURN"))
	private void rainy$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (FullbrightModule.active) {
			state.nightVisionEffectIntensity = 1.0f;
		}
	}
}
