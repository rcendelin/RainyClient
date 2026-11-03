package cz.nebixy.rainy.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import cz.nebixy.rainy.client.module.HealthTagsModule;
import cz.nebixy.rainy.client.util.RainyBadge;

/**
 * Jmenovky hráčů: ikonka Rainy před jménem a srdíčka "❤ 20" nad jménem.
 *
 * Minecraft kreslí scoreText na místo jména a jméno o řádek výš, takže je prohodíme:
 * jméno pošleme do scoreText a srdíčka dáme nahoru místo jména.
 * Když jméno vidět není (tvoje postava v F5, nebo server jména schovává), ukážeme jen srdíčka.
 */
@Mixin(AvatarRenderer.class)
public class HealthTagMixin {
	private static final double MAX_DISTANCE_SQ = 64 * 64;
	/** O kolik bloků výš dát srdíčka, když server kreslí vlastní jmenovky (třeba DonutSMP má 2 řádky). */
	private static final double SERVER_TAG_OFFSET = 0.75;

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void rainy$nameTags(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		if (!(entity instanceof Player)) {
			return;
		}
		boolean showHearts = HealthTagsModule.active;
		boolean showBadge = RainyBadge.enabled && RainyBadge.isRainy(entity.getUUID());
		if (!showHearts && !showBadge) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || state.distanceToCameraSq > MAX_DISTANCE_SQ) {
			return;
		}
		boolean self = entity == mc.player;
		if (self && mc.options.getCameraType().isFirstPerson()) {
			return; // sám sebe v první osobě nevidíš
		}
		if (!self && entity.isInvisibleTo(mc.player)) {
			return; // neviditelné hráče neprozrazujeme
		}

		// Ikonka Rainy před jménem
		if (showBadge && state.nameTag != null) {
			state.nameTag = RainyBadge.prefix(state.nameTag);
		}
		if (!showHearts) {
			return;
		}

		MutableComponent hearts = hearts(entity);

		if (state.nameTag == null) {
			// Jméno není vidět (tvoje postava v F5, nebo server jména schovává a kreslí vlastní).
			// Ukážeme aspoň srdíčka (s ikonkou, když má Rainy). U ostatních kousek výš, ať nepřekryjí jmenovku serveru.
			Vec3 attachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
			if (attachment == null) {
				return;
			}
			state.nameTagAttachment = self ? attachment : attachment.add(0, SERVER_TAG_OFFSET, 0);
			state.nameTag = showBadge ? RainyBadge.prefix(hearts) : hearts;
		} else if (state.scoreText == null) {
			state.scoreText = state.nameTag; // jméno (s ikonkou) dolů
			state.nameTag = hearts;          // srdíčka nahoru
		} else {
			// Server už pod jménem něco ukazuje, tak srdíčka přidáme před jméno
			state.nameTag = Component.empty().append(hearts).append(Component.literal("  ")).append(state.nameTag);
		}
	}

	private static MutableComponent hearts(Avatar entity) {
		float max = Math.max(1f, entity.getMaxHealth());
		int health = (int) Math.ceil(entity.getHealth());
		int absorption = (int) Math.ceil(entity.getAbsorptionAmount());
		float ratio = entity.getHealth() / max;
		// Číslo podle toho, kolik zbývá: zelená = hodně, žlutá = půlka, červená = málo
		ChatFormatting numberColor = ratio > 0.6f ? ChatFormatting.GREEN : ratio > 0.3f ? ChatFormatting.YELLOW : ChatFormatting.RED;

		MutableComponent text = Component.literal("❤ ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
				.append(Component.literal(String.valueOf(health)).withStyle(numberColor, ChatFormatting.BOLD));
		if (absorption > 0) {
			text.append(Component.literal(" +" + absorption).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		}
		return text;
	}
}
