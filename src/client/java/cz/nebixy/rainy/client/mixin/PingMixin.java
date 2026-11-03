package cz.nebixy.rainy.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.PingDebugMonitor;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;

import cz.nebixy.rainy.client.util.PingTracker;

/**
 * Každá odpověď "pong" ze serveru jde přes PingDebugMonitor (graf pingu na F3).
 * Odtud si vezmeme čas a spočítáme skutečný ping.
 */
@Mixin(PingDebugMonitor.class)
public class PingMixin {
	@Inject(method = "onPongReceived", at = @At("HEAD"))
	private void rainy$onPong(ClientboundPongResponsePacket packet, CallbackInfo ci) {
		PingTracker.onPong(packet.time());
	}
}
