package cz.nebixy.rainy.client.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import cz.nebixy.rainy.client.gui.ShulkerTooltip;
import cz.nebixy.rainy.client.module.ShulkerPreviewModule;

/**
 * U shulkeru s obsahem vrátí jako obrázek v tooltipu náš náhled (ShulkerTooltip.Data).
 */
@Mixin(ItemStack.class)
public class ShulkerTooltipMixin {
	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
	private void rainy$shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (!ShulkerPreviewModule.active || cir.getReturnValue().isPresent()) {
			return;
		}
		ShulkerTooltip.Data data = ShulkerTooltip.from((ItemStack) (Object) this);
		if (data != null) {
			cir.setReturnValue(Optional.of(data));
		}
	}
}
