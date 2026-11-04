package cz.nebixy.rainy.client.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import cz.nebixy.rainy.client.gui.MapTooltip;
import cz.nebixy.rainy.client.module.MapPreviewModule;

/**
 * U vyplněné mapy vrátí jako obrázek v tooltipu náhled mapy (MapTooltip.Data).
 */
@Mixin(ItemStack.class)
public class MapTooltipMixin {
	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
	private void rainy$mapPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (!MapPreviewModule.active || cir.getReturnValue().isPresent()) {
			return;
		}
		MapTooltip.Data data = MapTooltip.from((ItemStack) (Object) this);
		if (data != null) {
			cir.setReturnValue(Optional.of(data));
		}
	}
}
