package org.taskchou.mcapibridge.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.taskchou.mcapibridge.Mcapibridge;

public class ScreenConfigScreen extends Screen {
    private final BlockPos pos;
    private final int currentId;

    private String savedText = null;
    private TextFieldWidget idField;
    private Text warningText = Text.empty();

    public ScreenConfigScreen(BlockPos pos, int currentId) {
        super(Text.translatable("mcapibridge.gui.title"));
        this.pos = pos;
        this.currentId = currentId;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        this.idField = new TextFieldWidget(this.textRenderer, centerX - 100, startY + 20, 200, 20, Text.translatable("mcapibridge.gui.input.id"));
        this.idField.setMaxLength(9);

        if (this.savedText != null) {
            this.idField.setText(this.savedText);
        } else {
            this.idField.setText(String.valueOf(currentId));
        }

        this.idField.setChangedListener(text -> {
            this.savedText = text;
            validate(text);
        });

        this.addDrawableChild(this.idField);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mcapibridge.gui.button.save"), button -> save())
                .dimensions(centerX - 100, startY + 70, 200, 20)
                .build());

        this.setInitialFocus(this.idField);
    }

    private void validate(String text) {
        try {
            Integer.parseInt(text);
            this.idField.setEditableColor(0xFFFFFF);
            this.warningText = Text.empty();
        } catch (NumberFormatException e) {
            this.idField.setEditableColor(0xFF0000);
            this.warningText = Text.translatable("mcapibridge.gui.error.nan");
        }
    }

    private void save() {
        try {
            int newId = Integer.parseInt(this.idField.getText());

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            buf.writeInt(newId);

            ClientPlayNetworking.send(Mcapibridge.SCREEN_SET_ID_ID, buf);

            this.close();
        } catch (NumberFormatException e) {
            validate(this.idField.getText());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, startY - 10, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcapibridge.gui.label.id"), centerX - 100, startY + 10, 0xAAAAAA);

        if (this.warningText != Text.empty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.warningText, centerX, startY + 45, 0xFF0000);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}