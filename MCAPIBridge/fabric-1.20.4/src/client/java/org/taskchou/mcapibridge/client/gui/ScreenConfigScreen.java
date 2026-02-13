package org.taskchou.mcapibridge.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.taskchou.mcapibridge.Mcapibridge;

public class ScreenConfigScreen extends Screen {
    private final BlockPos pos;
    private final int currentId;

    private String savedText = null;

    private TextFieldWidget idField;
    private TextWidget warningField;

    public ScreenConfigScreen(BlockPos pos, int currentId) {
        super(Text.translatable("mcapibridge.gui.title"));
        this.pos = pos;
        this.currentId = currentId;
    }

    @Override
    protected void init() {
        GridWidget grid = new GridWidget();
        grid.getMainPositioner().margin(4);
        GridWidget.Adder adder = grid.createAdder(1);

        adder.add(new TextWidget(this.title, this.textRenderer));

        adder.add(new TextWidget(Text.translatable("mcapibridge.gui.label.id"),this.textRenderer).alignLeft());

        this.idField = new TextFieldWidget(this.textRenderer, 200, 20, Text.translatable("mcapibridge.gui.input.id"));
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

        adder.add(this.idField);

        this.warningField = new TextWidget(200, 20, Text.empty(), this.textRenderer);
        this.warningField.alignCenter();
        adder.add(this.warningField);

        adder.add(ButtonWidget.builder(Text.translatable("mcapibridge.gui.button.save"), button -> save())
                .width(200)
                .build());

        grid.refreshPositions();
        SimplePositioningWidget.setPos(grid, 0, 0, this.width, this.height, 0.5f, 0.5f);

        grid.forEachChild(this::addDrawableChild);

        this.setInitialFocus(this.idField);
    }

    private void validate(String text) {
        try {
            Integer.parseInt(text);
            this.idField.setEditableColor(0xFFFFFF);
            this.warningField.setMessage(Text.empty());
        } catch (NumberFormatException e) {
            this.idField.setEditableColor(0xFF0000);
            this.warningField.setTextColor(0xFF0000);
            this.warningField.setMessage(Text.translatable("mcapibridge.gui.error.nan"));
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
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}