package org.taskchou.mcapibridge.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.taskchou.mcapibridge.block.IOBlock;
import org.taskchou.mcapibridge.block.IOBlockEntity;

public class IOBlockRenderer implements BlockEntityRenderer<IOBlockEntity> {

    private static final Identifier TEX_INPUT = Identifier.of("mcapibridge", "textures/block/io_top_in_emissive.png");
    private static final Identifier TEX_OUTPUT = Identifier.of("mcapibridge", "textures/block/io_top_out_emissive.png");

    public IOBlockRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(IOBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        boolean mode = entity.getCachedState().get(IOBlock.OUTPUT_MODE);
        Identifier tex = mode ? TEX_OUTPUT : TEX_INPUT;

        matrices.push();
        matrices.translate(0, 1.001, 0);

        //VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutout());//Fantastic Bug
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(tex));


        Matrix4f mat = matrices.peek().getPositionMatrix();

        buffer.vertex(mat, 0, 0, 0).color(255,255,255,255).texture(0, 0)
                .overlay(overlay).light(0xF000F0).normal(0, 1, 0);

        buffer.vertex(mat, 0, 0, 1).color(255,255,255,255).texture(0, 1)
                .overlay(overlay).light(0xF000F0).normal(0, 1, 0);

        buffer.vertex(mat, 1, 0, 1).color(255,255,255,255).texture(1, 1)
                .overlay(overlay).light(0xF000F0).normal(0, 1, 0);

        buffer.vertex(mat, 1, 0, 0).color(255,255,255,255).texture(1, 0)
                .overlay(overlay).light(0xF000F0).normal(0, 1, 0);

        matrices.pop();
    }
}