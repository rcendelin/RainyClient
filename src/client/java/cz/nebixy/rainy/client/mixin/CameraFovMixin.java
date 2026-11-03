package cz.nebixy.rainy.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;

import cz.nebixy.rainy.client.module.ZoomModule;

/**
 * Zoom: FOV světa se vydělí aktuálním zoomem (ruka a HUD zůstanou normální).
 */
@Mixin(Camera.class)
public class CameraFovMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void rainy$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
		float divisor = ZoomModule.fovDivisor();
		if (divisor != 1f) {
			cir.setReturnValue(cir.getReturnValue() / divisor);
		}
	}
}
