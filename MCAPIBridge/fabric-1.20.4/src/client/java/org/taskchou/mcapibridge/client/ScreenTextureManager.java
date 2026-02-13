package org.taskchou.mcapibridge.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenTextureManager {

    private static final ConcurrentHashMap<Integer, NativeImageBackedTexture> textureCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Identifier> idCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Long> lastFrameTime = new ConcurrentHashMap<>();

    private static final ExecutorService decoderExecutor = Executors.newSingleThreadExecutor();
    private static final long MAX_LATENCY = 500;

    public static Identifier getTexture(int screenId) {
        return idCache.get(screenId);
    }

    public static void updateTexture(int screenId, byte[] data, long serverTimestamp) {
        long clientNow = System.currentTimeMillis();

        if (clientNow - serverTimestamp > MAX_LATENCY) {
            //System.out.println("Dropped laggy frame: " + (clientNow - serverTimestamp) + "ms");
            return;
        }

        long last = lastFrameTime.getOrDefault(screenId, 0L);
        if (serverTimestamp < last) {
            return;
        }
        lastFrameTime.put(screenId, serverTimestamp);

        decoderExecutor.submit(() -> {
            try {
                NativeImage newImage = decodeImage(data);
                if (newImage == null) return;

                MinecraftClient.getInstance().execute(() -> {
                    uploadToGPU(screenId, newImage);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static NativeImage decodeImage(byte[] data) {
        ByteBuffer buffer = null;
        try {
            buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data);
            buffer.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer c = stack.mallocInt(1);

                ByteBuffer pixels = STBImage.stbi_load_from_memory(buffer, w, h, c, 4);

                if (pixels == null) {
                    throw new IOException("STB failed: " + STBImage.stbi_failure_reason());
                }


                int width = w.get(0);
                int height = h.get(0);
                NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);

                long pixelAddr = MemoryUtil.memAddress(pixels);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int offset = (y * width + x) * 4;
                        int r = MemoryUtil.memGetByte(pixelAddr + offset) & 0xFF;
                        int g = MemoryUtil.memGetByte(pixelAddr + offset + 1) & 0xFF;
                        int b = MemoryUtil.memGetByte(pixelAddr + offset + 2) & 0xFF;
                        int a = MemoryUtil.memGetByte(pixelAddr + offset + 3) & 0xFF;

                        image.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                    }
                }

                STBImage.stbi_image_free(pixels);
                return image;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (buffer != null) MemoryUtil.memFree(buffer);
        }
    }

    private static void uploadToGPU(int screenId, NativeImage newImage) {
        NativeImageBackedTexture existingTexture = textureCache.get(screenId);
        Identifier id = idCache.computeIfAbsent(screenId, k -> new Identifier("mcapibridge", "screen_" + k));

        if (existingTexture != null &&
                existingTexture.getImage() != null &&
                existingTexture.getImage().getWidth() == newImage.getWidth() &&
                existingTexture.getImage().getHeight() == newImage.getHeight()) {

            existingTexture.setImage(newImage);
            existingTexture.upload();


        } else {
            if (existingTexture != null) {
                existingTexture.close();
                MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
            }

            NativeImageBackedTexture newTexture = new NativeImageBackedTexture(newImage);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, newTexture);
            textureCache.put(screenId, newTexture);
        }
    }
}