package com.flashmod.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Estado do poder no cliente + números de balanceamento (fáceis de ajustar). */
@Environment(EnvType.CLIENT)
public final class FlashClientState {
    private FlashClientState() {}

    // Blocos por tick (20 ticks = 1 segundo): nível 1 ~11 b/s, nível 2 ~22 b/s, nível 3 ~40 b/s.
    public static final double[] SPEEDS = {0.55, 1.1, 2.0};
    // Energia gasta por tick em cada nível.
    public static final double[] DRAIN = {0.10, 0.25, 0.55};
    public static final double RECHARGE = 0.35;
    public static final double MAX_ENERGY = 100.0;
    public static final double MIN_TO_START = 10.0;
    public static final double MIN_AFTER_EXHAUSTED = 25.0;

    public static final double WALL_CLIMB_SPEED = 0.45;

    /** 0 = desligado, 1..3 = nível ativo. */
    public static int level = 0;
    /** Nível escolhido para quando ligar. */
    public static int selectedLevel = 1;
    public static double energy = MAX_ENERGY;
    public static boolean exhausted = false;

    // Controle de sincronização com o servidor.
    static int sentLevel = -1;
    static int sentColor = -1;
    static boolean sentRainbow = false;
    static int sentTimer = 0;

    public static void reset() {
        level = 0;
        energy = MAX_ENERGY;
        exhausted = false;
        sentLevel = -1;
        sentTimer = 0;
    }
}
