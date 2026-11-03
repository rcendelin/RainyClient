package cz.nebixy.rainy.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import cz.nebixy.rainy.client.util.RainyBadge;

/**
 * TAB: před jméno lidí s Rainy Clientem přidá logo.
 */
@Mixin(PlayerTabOverlay.class)
public class TabListMixin {
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void rainy$badge(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		if (RainyBadge.enabled && RainyBadge.isRainy(info)) {
			cir.setReturnValue(RainyBadge.prefix(cir.getReturnValue()));
		}
	}
}
