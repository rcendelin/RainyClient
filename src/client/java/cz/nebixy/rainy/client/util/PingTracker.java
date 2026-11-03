package cz.nebixy.rainy.client.util;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.Util;

/**
 * Skutečný ping: každé 2 sekundy pošle serveru "ping" a změří, za jak dlouho přijde "pong".
 * Některé servery (třeba DonutSMP) v TABu ukazují falešný ping, tohle se zfalšovat nedá.
 */
public final class PingTracker {
	private PingTracker() {
	}

	private static final int INTERVAL_TICKS = 40; // 2 sekundy
	private static final Deque<Long> SAMPLES = new ArrayDeque<>();
	private static int ticks;
	private static volatile long lastPongAt;

	/** Volá se každý tick, když je zapnutý Ping HUD. */
	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			SAMPLES.clear();
			return;
		}
		if (++ticks >= INTERVAL_TICKS) {
			ticks = 0;
			mc.player.connection.send(new ServerboundPingRequestPacket(Util.getMillis()));
		}
	}

	/** Volá PingMixin, když přijde odpověď ze serveru. */
	public static void onPong(long sentAt) {
		long rtt = Util.getMillis() - sentAt;
		if (rtt < 0 || rtt > 60_000) {
			return;
		}
		synchronized (SAMPLES) {
			SAMPLES.addLast(rtt);
			while (SAMPLES.size() > 5) {
				SAMPLES.pollFirst();
			}
		}
		lastPongAt = System.currentTimeMillis();
	}

	/** Průměr posledních měření v ms, nebo -1, když server na ping neodpovídá. */
	public static int ping() {
		if (System.currentTimeMillis() - lastPongAt > 10_000) {
			return -1;
		}
		synchronized (SAMPLES) {
			if (SAMPLES.isEmpty()) {
				return -1;
			}
			long sum = 0;
			for (long s : SAMPLES) {
				sum += s;
			}
			return (int) Math.round(sum / (double) SAMPLES.size());
		}
	}
}
