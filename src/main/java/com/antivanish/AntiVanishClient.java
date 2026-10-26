package com.antivanish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AntiVanishClient implements ClientModInitializer {
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("antivanish.txt");
    public static String targetName = "";

    private final Set<UUID> inRange = new HashSet<>();
    private static final int DURATION = 60; // ticks (3 seconds)
    private int effectTick = -1;
    private Identifier effectSkin;

    @Override
    public void onInitializeClient() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    public static void load() {
        try { if (Files.exists(CONFIG)) targetName = Files.readString(CONFIG).trim(); } catch (Exception ignored) {}
    }

    public static void save() {
        try { Files.writeString(CONFIG, targetName == null ? "" : targetName.trim()); } catch (Exception ignored) {}
    }

    private void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) { inRange.clear(); effectTick = -1; return; }
        if (effectTick >= 0 && ++effectTick > DURATION) effectTick = -1;

        Set<UUID> now = new HashSet<>();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            UUID id = p.getUuid();
            now.add(id);
            if (inRange.contains(id)) continue;

            String name = p.getGameProfile().getName();
            mc.inGameHud.getChatHud().addMessage(Text.literal("§c[AntiVanish] §f" + name + " §cis in your render distance!"));
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f));

            if (!targetName.isBlank() && name.equalsIgnoreCase(targetName.trim())) {
                effectSkin = p.getSkinTextures().texture();
                effectTick = 0;
                // Same sound as the Mining Fatigue elder guardian curse
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f));
            }
        }
        inRange.retainAll(now);
        inRange.addAll(now);
    }

    private void renderHud(DrawContext ctx, RenderTickCounter tc) {
        if (effectTick < 0 || effectSkin == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        float t = (effectTick + tc.getTickDelta(false)) / DURATION;   // 0..1
        float curve = MathHelper.sin(t * (float) Math.PI);              // grows then shrinks, like the vanilla effect
        float size = mc.getWindow().getScaledHeight() * (0.1f + 1.3f * curve);
        float alpha = Math.min(1f, curve * 1.6f);
        float spin = (1f - t) * 25f * MathHelper.sin(t * 6f);           // little wobble/rotation

        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int half = Math.round(size / 2f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));
        // face (8,8) + hat layer (40,8) from the 64x64 skin
        ctx.drawTexture(effectSkin, -half, -half, half * 2, half * 2, 8f, 8f, 8, 8, 64, 64);
        ctx.drawTexture(effectSkin, -half, -half, half * 2, half * 2, 40f, 8f, 8, 8, 64, 64);
        ctx.getMatrices().pop();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
