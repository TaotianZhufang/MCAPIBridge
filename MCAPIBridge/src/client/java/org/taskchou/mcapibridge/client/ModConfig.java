package org.taskchou.mcapibridge.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModConfig {

    private static final Path CONFIG_PATH = Paths.get("config", "mcapibridge.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;

    public float customAudioVolume = 1.0f;

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static ModConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                if (config != null) {
                    INSTANCE = config;
                    return config;
                }
            }
        } catch (Exception e) {
            System.out.println("[MCAPIBridge] Failed to load config: " + e.getMessage());
        }
        INSTANCE = new ModConfig();
        INSTANCE.save();
        return INSTANCE;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            System.out.println("[MCAPIBridge] Failed to save config: " + e.getMessage());
        }
    }

    public void setCustomAudioVolume(float volume) {
        this.customAudioVolume = Math.max(0f, Math.min(1f, volume));
        save();
    }
}