package com.flashmod.client;

import com.flashmod.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.Random;

/** Barra de energia (Speed Force) e linhas de velocidade nas bordas da tela. */
@Environment(EnvType.CLIENT)
public final class FlashHud {
    private FlashHud() {}

    public static int currentColor(MinecraftClient mc) {
        if (FlashConfig.rainbow) {
            long t = mc.world != null ? mc.world.getTime() : 0;
            return ColorUtil.rainbow(t * 2.0);
        }
        return FlashConfig.color;
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;

        int level = FlashClientState.level;
        int color = currentColor(mc);
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        // Linhas de velocidade nas laterais (nível 2 e 3), na cor do raio.
        if (level >= 2) {
            Random r = new Random(mc.world.getTime());
            int lines = level == 3 ? 18 : 10;
            for (int i = 0; i < lines; i++) {
                int ly = r.nextInt(h);
                int len = 30 + r.nextInt(level == 3 ? 120 : 70);
                int lx = r.nextBoolean() ? 0 : w - len;
                ctx.fill(lx, ly, lx + len, ly + 1, 0x55000000 | color);
            }
        }

        // Barra de energia: só aparece se estiver usando ou se não estiver cheia.
        if (level == 0 && FlashClientState.energy >= FlashClientState.MAX_ENERGY) return;

        int barW = 90;
        int barH = 6;
        int x = 10;
        int y = h - 20;
        double pct = FlashClientState.energy / FlashClientState.MAX_ENERGY;
        int filled = (int) (barW * pct);

        ctx.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xAA000000);
        ctx.fill(x, y, x + barW, y + barH, 0xFF2A2A2A);
        int barColor = FlashClientState.exhausted ? 0xFF777777 : (0xFF000000 | color);
        ctx.fill(x, y, x + filled, y + barH, barColor);
        ctx.fill(x, y, x + filled, y + 1, 0x66FFFFFF);

        Text label = Text.translatable("hud.flashmod.energy");
        if (level > 0) {
            label = Text.empty().append(label).append("  ")
                    .append(Text.translatable("hud.flashmod.level", level));
        }
        ctx.drawText(mc.textRenderer, label, x, y - 11, 0xFFFFFFFF, true);
    }
}
