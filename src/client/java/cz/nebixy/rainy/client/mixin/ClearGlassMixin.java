package cz.nebixy.rainy.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;

import cz.nebixy.rainy.client.module.ClearGlassModule;

/**
 * Neviditelné sklo: sklo řekne, že se nemá kreslit (jako bariéra).
 * Pořád je to normální blok, takže do něj narazíš a kurzorem uvidíš černý rámeček.
 * Funguje i se Sodiem, ten se ptá stejně.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ClearGlassMixin {
	@Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
	private void rainy$clearGlass(CallbackInfoReturnable<RenderShape> cir) {
		if (ClearGlassModule.active && ClearGlassModule.isGlass(((BlockBehaviour.BlockStateBase) (Object) this).getBlock())) {
			cir.setReturnValue(RenderShape.INVISIBLE);
		}
	}
}
