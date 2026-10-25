package com.flashmod;

import com.flashmod.network.FlashStatePayload;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lado do servidor: guarda o estado de cada jogador e desenha o rastro de raios
 * (partículas coloridas) que todos os jogadores por perto conseguem ver.
 */
public final class FlashServer {
    private FlashServer() {}

    private static final class State {
        int level;
        int color;
        boolean rainbow;
        boolean hasLast;
        double lastX, lastY, lastZ;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static void update(ServerPlayerEntity player, FlashStatePayload payload) {
        State s = STATES.computeIfAbsent(player.getUuid(), id -> new State());
        boolean wasOff = s.level == 0;

        s.level = MathHelper.clamp(payload.level(), 0, 3);
        s.color = payload.color() & 0xFFFFFF;
        s.rainbow = payload.rainbow();

        if (s.level == 0) {
            s.hasLast = false;
        } else if (wasOff) {
            s.hasLast = false;
            player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.4f, 1.8f);
        }
    }

    public static void remove(UUID id) {
        STATES.remove(id);
    }

    public static void tick(MinecraftServer server) {
        long time = server.getTicks();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            State s = STATES.get(player.getUuid());
            if (s == null || s.level == 0 || player.isSpectator()) continue;

            // Sem dano de queda enquanto a Força da Velocidade está ativa.
            player.fallDistance = 0;

            ServerWorld world = player.getServerWorld();
            double x = player.getX();
            double y = player.getY() + 0.9;
            double z = player.getZ();

            if (!s.hasLast) {
                s.lastX = x; s.lastY = y; s.lastZ = z;
                s.hasLast = true;
            }

            double dx = x - s.lastX;
            double dy = y - s.lastY;
            double dz = z - s.lastZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Rastro contínuo entre a posição anterior e a atual.
            int steps = Math.min(6, 2 + (int) (dist * 1.5));
            for (int i = 0; i < steps; i++) {
                double t = (i + 1) / (double) steps;
                double px = s.lastX + dx * t;
                double py = s.lastY + dy * t;
                double pz = s.lastZ + dz * t;

                int core = s.rainbow ? ColorUtil.rainbow(time * 3 + i * 2) : s.color;
                dust(world, core, 1.0f, px, py, pz, 2, 0.25, 0.45, 0.25);
                if (i % 2 == 0) {
                    dust(world, ColorUtil.lighten(core, 0.5f), 1.8f, px, py, pz, 1, 0.3, 0.5, 0.3);
                }
            }

            // Pequenos arcos de raio saindo do corpo (mais arcos em níveis mais altos).
            Random rnd = player.getRandom();
            for (int a = 0; a < s.level; a++) {
                int color = s.rainbow ? ColorUtil.rainbow(time * 3 + a * 9) : s.color;
                double ax = x;
                double ay = y + (rnd.nextDouble() - 0.5) * 1.2;
                double az = z;

                double vx = rnd.nextGaussian();
                double vy = rnd.nextGaussian() * 0.6;
                double vz = rnd.nextGaussian();
                double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (len < 1.0E-4) continue;
                vx /= len; vy /= len; vz /= len;

                for (int k = 0; k < 5; k++) {
                    ax += vx * 0.25 + (rnd.nextDouble() - 0.5) * 0.25;
                    ay += vy * 0.25 + (rnd.nextDouble() - 0.5) * 0.25;
                    az += vz * 0.25 + (rnd.nextDouble() - 0.5) * 0.25;
                    dust(world, color, 0.6f, ax, ay, az, 1, 0, 0, 0);
                }
            }

            s.lastX = x; s.lastY = y; s.lastZ = z;
        }
    }

    private static void dust(ServerWorld world, int rgb, float scale,
                             double x, double y, double z,
                             int count, double sx, double sy, double sz) {
        world.spawnParticles(new DustParticleEffect(ColorUtil.toVector(rgb), scale),
                x, y, z, count, sx, sy, sz, 0.0);
    }
}
