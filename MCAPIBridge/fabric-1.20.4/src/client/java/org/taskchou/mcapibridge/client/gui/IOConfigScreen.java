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

public class IOConfigScreen extends Screen {
    private final BlockPos pos;
    private final int currentId;
    private final boolean currentMode;

    private TextFieldWidget idField;
    private CyclingButtonWidget<Boolean> modeBtn;
    private TextWidget warningField;

    public IOConfigScreen(BlockPos pos, int currentId, boolean currentMode) {
        super(Text.translatable("mcapibridge.gui.io.title"));
        this.pos = pos;
        this.currentId = currentId;
        this.currentMode = currentMode;
    }

    @Override
    protected void init() {
        GridWidget grid = new GridWidget();
        grid.getMainPositioner().margin(4);
        GridWidget.Adder adder = grid.createAdder(1);

        adder.add(new TextWidget(this.title, textRenderer));

        adder.add(new TextWidget(Text.translatable("mcapibridge.gui.io.label.id"),textRenderer).alignLeft());
        this.idField = new TextFieldWidget(textRenderer, 200, 20, Text.empty());
        this.idField.setMaxLength(9);
        this.idField.setText(String.valueOf(currentId));
        this.idField.setChangedListener(this::validate);
        adder.add(this.idField);

        this.warningField = new TextWidget(200, 20, Text.empty(), textRenderer);
        adder.add(this.warningField);

        adder.add(new TextWidget(Text.translatable("mcapibridge.gui.io.label.mode"), textRenderer).alignLeft());
        this.modeBtn = CyclingButtonWidget.<Boolean>builder(mode -> {
                    if (mode) return Text.translatable("mcapibridge.gui.io.mode.output").styled(s -> s.withColor(0x55FF55));
                    else return Text.translatable("mcapibridge.gui.io.mode.input").styled(s -> s.withColor(0xFFAA00));
                })
                .values(false, true)
                .initially(currentMode)
                .build(0, 0, 200, 20, Text.translatable("mcapibridge.gui.io.label.mode"));
        adder.add(this.modeBtn);

        adder.add(ButtonWidget.builder(Text.translatable("mcapibridge.gui.io.button.save"), b -> save()).width(200).build());

        grid.refreshPositions();
        SimplePositioningWidget.setPos(grid, 0, 0, this.width, this.height, 0.5f, 0.5f);
        grid.forEachChild(this::addDrawableChild);

        setInitialFocus(idField);
    }

    private void validate(String text) {
        try {
            Integer.parseInt(text);
            idField.setEditableColor(0xFFFFFF);
            warningField.setMessage(Text.empty());
        } catch (NumberFormatException e) {
            idField.setEditableColor(0xFF0000);
            warningField.setMessage(Text.translatable("mcapibridge.gui.io.error.nan"));
            warningField.setTextColor(0xFF0000);
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
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}