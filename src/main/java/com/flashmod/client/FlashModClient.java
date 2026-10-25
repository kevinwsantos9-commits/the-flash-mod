package com.flashmod.client;

import com.flashmod.network.FlashStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class FlashModClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static KeyBinding levelKey;
    private static KeyBinding colorKey;

    @Override
    public void onInitializeClient() {
        FlashConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flashmod.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.flashmod"));
        levelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flashmod.level", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.flashmod"));
        colorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flashmod.color", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.flashmod"));

        ClientTickEvents.START_CLIENT_TICK.register(FlashModClient::tick);
        HudRenderCallback.EVENT.register(FlashHud::render);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> FlashClientState.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FlashClientState.reset());
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        while (toggleKey.wasPressed()) {
            toggle();
        }
        while (levelKey.wasPressed()) {
            cycleLevel(player);
        }
        while (colorKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new ColorScreen());
            }
        }

        if (FlashClientState.level > 0) {
            if (!player.isCreative()) {
                FlashClientState.energy -= FlashClientState.DRAIN[FlashClientState.level - 1];
            }
            if (FlashClientState.energy <= 0) {
                FlashClientState.energy = 0;
                FlashClientState.level = 0;
                FlashClientState.exhausted = true;
                player.sendMessage(Text.translatable("msg.flashmod.exhausted"), true);
            } else {
                applyMovement(player);
            }
        } else {
            FlashClientState.energy = Math.min(FlashClientState.MAX_ENERGY,
                    FlashClientState.energy + FlashClientState.RECHARGE);
            if (FlashClientState.exhausted && FlashClientState.energy >= FlashClientState.MIN_AFTER_EXHAUSTED) {
                FlashClientState.exhausted = false;
            }
        }

        sync();
    }

    private static void toggle() {
        if (FlashClientState.level > 0) {
            FlashClientState.level = 0;
        } else if (!FlashClientState.exhausted && FlashClientState.energy >= FlashClientState.MIN_TO_START) {
            FlashClientState.level = FlashClientState.selectedLevel;
        }
    }

    private static void cycleLevel(ClientPlayerEntity player) {
        FlashClientState.selectedLevel = FlashClientState.selectedLevel % 3 + 1;
        if (FlashClientState.level > 0) {
            FlashClientState.level = FlashClientState.selectedLevel;
        }
        player.sendMessage(Text.translatable("msg.flashmod.level", FlashClientState.selectedLevel), true);
    }

    /** Define a velocidade horizontal na direção das teclas de movimento e sobe paredes ao bater nelas. */
    private static void applyMovement(ClientPlayerEntity player) {
        if (player.hasVehicle() || player.isFallFlying() || player.getAbilities().flying) return;

        float forward = player.input.movementForward;
        float side = player.input.movementSideways;
        if (forward == 0 && side == 0) return;

        double len = Math.sqrt(forward * forward + side * side);
        double nf = forward / len;
        double ns = side / len;

        double speed = FlashClientState.SPEEDS[FlashClientState.level - 1];
        float yawRad = player.getYaw() * 0.017453292F;
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double vx = (ns * cos - nf * sin) * speed;
        double vz = (nf * cos + ns * sin) * speed;

        Vec3d v = player.getVelocity();
        double vy = v.y;

        // Correr na parede: bateu em um bloco andando para frente -> sobe.
        if (player.horizontalCollision && forward > 0) {
            vy = FlashClientState.WALL_CLIMB_SPEED;
            player.fallDistance = 0;
        }

        player.setVelocity(vx, vy, vz);
    }

    /** Envia o estado ao servidor quando muda (e a cada 5 s, por segurança). */
    private static void sync() {
        int color = FlashConfig.color;
        boolean rainbow = FlashConfig.rainbow;
        FlashClientState.sentTimer++;

        boolean changed = FlashClientState.level != FlashClientState.sentLevel
                || color != FlashClientState.sentColor
                || rainbow != FlashClientState.sentRainbow;
        if (!changed && FlashClientState.sentTimer < 100) return;

        // Servidor sem o mod: não envia nada.
        if (!ClientPlayNetworking.canSend(FlashStatePayload.ID)) return;

        ClientPlayNetworking.send(new FlashStatePayload(FlashClientState.level, color, rainbow));
        FlashClientState.sentLevel = FlashClientState.level;
        FlashClientState.sentColor = color;
        FlashClientState.sentRainbow = rainbow;
        FlashClientState.sentTimer = 0;
    }
}
