package cz.nebixy.rainy.client.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

import cz.nebixy.rainy.client.gui.ShulkerTooltip;
import cz.nebixy.rainy.client.module.ShulkerPreviewModule;

/**
 * Když se ukazuje náhled shulkeru, schová textový seznam itemů, který Minecraft dává do tooltipu.
 */
@Mixin(ItemContainerContents.class)
public class ContainerTooltipMixin {
	@Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
	private void rainy$hideList(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components, CallbackInfo ci) {
		if (ShulkerPreviewModule.active && components instanceof ItemStack stack && ShulkerTooltip.from(stack) != null) {
			ci.cancel();
		}
	}
}
