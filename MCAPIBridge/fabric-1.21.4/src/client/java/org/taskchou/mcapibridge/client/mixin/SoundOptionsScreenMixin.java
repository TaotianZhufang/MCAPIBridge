package org.taskchou.mcapibridge.client.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taskchou.mcapibridge.client.ModConfig;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin extends GameOptionsScreen {

    protected SoundOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    @Inject(method = "addOptions()V", at = @At("TAIL"))
    private void addCustomAudioSlider(CallbackInfo ci) {
        if (body == null) return;

        SimpleOption<Double> customAudioOption = new SimpleOption<>(
                "mcapibridge.options.customAudioVolume",
                SimpleOption.emptyTooltip(),
                (optionText, value) -> {
                    if (value == 0.0) {
                        return GameOptions.getGenericValueText(optionText, Text.translatable("options.off"));
                    }
                    return Text.literal(optionText.getString() + ": " + (int)(value * 100) + "%");
                },
                SimpleOption.DoubleSliderCallbacks.INSTANCE,
                (double) ModConfig.get().customAudioVolume,
                value -> ModConfig.get().setCustomAudioVolume(value.floatValue())
        );

        SimpleOption<Double> syncOption = new SimpleOption<>(
                "mcapibridge.options.audioSync",
                SimpleOption.emptyTooltip(),
                (text, value) -> {
                    double offset = (value - 0.5) * 10.0;
                    String sign = offset > 0 ? "+" : "";
                    return Text.literal(String.format("%s: %s%.2fs", text.getString(), sign, offset));
                },
                SimpleOption.DoubleSliderCallbacks.INSTANCE,
                (double) (ModConfig.get().audioSyncOffset / 10.0f + 0.5f),
                value -> {
                    float offset = (float) ((value - 0.5) * 10.0);
                    ModConfig.get().audioSyncOffset = offset;
                    ModConfig.get().save();
                }
        );

        body.addSingleOptionEntry(customAudioOption);
        body.addSingleOptionEntry(syncOption);
    }
}