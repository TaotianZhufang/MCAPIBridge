package org.taskchou.mcapibridge.item;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ScreenBlockItem extends BlockItem {

    public ScreenBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("block.mcapibridge.screen.desc").formatted(Formatting.GRAY));

        // tooltip.add(Text.translatable("block.mcapibridge.screen.desc_2").formatted(Formatting.DARK_GRAY));

        super.appendTooltip(stack, context, tooltip, type);
    }
}