package org.taskchou.mcapibridge.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.taskchou.mcapibridge.Mcapibridge;

public class IOConfigScreen extends Screen {
    private final BlockPos pos;
    private final int currentId;
    private final boolean currentMode;

    private TextFieldWidget idField;
    private CyclingButtonWidget<Boolean> modeBtn;
    private Text warningText = Text.empty();

    public IOConfigScreen(BlockPos pos, int currentId, boolean currentMode) {
        super(Text.translatable("mcapibridge.gui.io.title"));
        this.pos = pos;
        this.currentId = currentId;
        this.currentMode = currentMode;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        this.idField = new TextFieldWidget(this.textRenderer, centerX - 100, startY + 20, 200, 20, Text.empty());
        this.idField.setMaxLength(9);
        this.idField.setText(String.valueOf(currentId));
        this.idField.setChangedListener(this::validate);
        this.addDrawableChild(this.idField);

        this.modeBtn = CyclingButtonWidget.<Boolean>builder(mode -> {
                    if (mode) return Text.translatable("mcapibridge.gui.io.mode.output").styled(s -> s.withColor(0x55FF55));
                    else return Text.translatable("mcapibridge.gui.io.mode.input").styled(s -> s.withColor(0xFFAA00));
                })
                .values(false, true)
                .initially(currentMode)
                .build(centerX - 100, startY + 70, 200, 20, Text.translatable("mcapibridge.gui.io.label.mode"));
        this.addDrawableChild(this.modeBtn);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mcapibridge.gui.io.button.save"), b -> save())
                .dimensions(centerX - 100, startY + 100, 200, 20)
                .build());

        this.setInitialFocus(this.idField);
    }

    private void validate(String text) {
        try {
            Integer.parseInt(text);
            idField.setEditableColor(0xFFFFFF);
            warningText = Text.empty();
        } catch (NumberFormatException e) {
            idField.setEditableColor(0xFF0000);
            warningText = Text.translatable("mcapibridge.gui.io.error.nan");
        }
    }

    private void save() {
        try {
            int newId = Integer.parseInt(idField.getText());
            boolean newMode = modeBtn.getValue();

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            buf.writeInt(newId);
            buf.writeBoolean(newMode);

            ClientPlayNetworking.send(Mcapibridge.IO_SET_CONFIG_ID, buf);

            this.close();
        } catch (NumberFormatException e) {
            validate(idField.getText());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, startY - 10, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcapibridge.gui.io.label.id"), centerX - 100, startY + 10, 0xAAAAAA);

        if (this.warningText != Text.empty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.warningText, centerX, startY + 45, 0xFF0000);
        }

        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcapibridge.gui.io.label.mode"), centerX - 100, startY + 60, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}