package org.taskchou.mcapibridge.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.taskchou.mcapibridge.block.ScreenBlock;
import org.taskchou.mcapibridge.block.ScreenBlockEntity;
import org.taskchou.mcapibridge.client.ScreenTextureManager;

public class ScreenBlockRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
    public ScreenBlockRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(ScreenBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        Identifier textureId = ScreenTextureManager.getTexture(entity.screenId);
        if (textureId == null) return;

        BlockState state = entity.getCachedState();
        Direction facing = state.contains(ScreenBlock.FACING) ? state.get(ScreenBlock.FACING) : Direction.NORTH;

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);



        float rotation = switch (facing) {
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.translate(0, 0, -0.53);

        float u1 = (float) entity.gridX / entity.width;
        float u2 = (float) (entity.gridX + 1) / entity.width;
        float v1 = 1.0f - (float) (entity.gridY + 1) / entity.height;
        float v2 = 1.0f - (float) entity.gridY / entity.height;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getText(textureId));

        // TR (Top Right)
        buffer.vertex(matrix, 0.5f, 0.5f, 0).color(255,255,255,255).texture(u1, v1).light(0xF000F0).next();
        // BR (Bottom Right)
        buffer.vertex(matrix, 0.5f, -0.5f, 0).color(255,255,255,255).texture(u1, v2).light(0xF000F0).next();
        // BL (Bottom Left)
        buffer.vertex(matrix, -0.5f, -0.5f, 0).color(255,255,255,255).texture(u2, v2).light(0xF000F0).next();
        // TL (Top Left)
        buffer.vertex(matrix, -0.5f, 0.5f, 0).color(255,255,255,255).texture(u2, v1).light(0xF000F0).next();

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(ScreenBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }
}