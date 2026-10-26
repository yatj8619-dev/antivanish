package com.antivanish;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class AntiVanishScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget field;

    public AntiVanishScreen(Screen parent) {
        super(Text.literal("AntiVanish"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        field = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 10, 200, 20, Text.literal("Player"));
        field.setMaxLength(16);
        field.setText(AntiVanishClient.targetName);
        addDrawableChild(field);
        setInitialFocus(field);
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            AntiVanishClient.targetName = field.getText().trim();
            AntiVanishClient.save();
            close();
        }).dimensions(width / 2 - 100, height / 2 + 20, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 50, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Target player name:"), width / 2, height / 2 - 25, 0xAAAAAA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
