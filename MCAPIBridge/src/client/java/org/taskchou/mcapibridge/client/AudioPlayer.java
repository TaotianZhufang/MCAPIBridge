package org.taskchou.mcapibridge.client;

import org.lwjgl.openal.AL10;
import org.lwjgl.BufferUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioPlayer {

    private static final ConcurrentHashMap<String, AudioSource> sources = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LoadingBuffer> loadingBuffers = new ConcurrentHashMap<>();

    private static MinecraftClient clientInstance = null;
    private static float listenerX, listenerY, listenerZ;

    private static class LoadingBuffer {
        int sampleRate;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
    }

    public static class AudioSource {
        public int sourceId;
        public ConcurrentLinkedQueue<Integer> bufferQueue = new ConcurrentLinkedQueue<>();
        public int sampleRate;
        public boolean playing = false;
        public boolean streaming = false;
        public float baseVolume = 1.0f;

        public boolean is3D = false;
        public float x, y, z;
        public float maxDistance = 100.0f;

        public AudioSource(int sampleRate) {
            this.sourceId = AL10.alGenSources();
            this.sampleRate = sampleRate;
        }

        public void cleanup() {
            AL10.alSourceStop(sourceId);

            Integer bufferId;
            while ((bufferId = bufferQueue.poll()) != null) {
                AL10.alDeleteBuffers(bufferId);
            }

            int processed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
            if (processed > 0) {
                IntBuffer buffers = BufferUtils.createIntBuffer(processed);
                AL10.alSourceUnqueueBuffers(sourceId, buffers);
                while (buffers.hasRemaining()) {
                    AL10.alDeleteBuffers(buffers.get());
                }
            }

            AL10.alDeleteSources(sourceId);
        }
    }

    public static void setClient(MinecraftClient client) {
        clientInstance = client;
    }

    private static float getVolumeMultiplier() {
        try {
            if (clientInstance != null && clientInstance.options != null) {
                double master = clientInstance.options.getSoundVolumeOption(SoundCategory.MASTER).getValue();
                float custom = ModConfig.get().customAudioVolume;
                return (float) master * custom;
            }
        } catch (Exception e) {
        }

        return ModConfig.get().customAudioVolume;
    }

    public static void loadStart(String id, byte[] pcmData, int sampleRate) {
        if (sources.containsKey(id)) {
            sources.get(id).cleanup();
            sources.remove(id);
        }
        loadingBuffers.remove(id);

        LoadingBuffer buffer = new LoadingBuffer();
        buffer.sampleRate = sampleRate;
        try {
            buffer.data.write(pcmData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadingBuffers.put(id, buffer);
    }

    public static void loadContinue(String id, byte[] pcmData) {
        LoadingBuffer buffer = loadingBuffers.get(id);
        if (buffer != null) {
            try {
                buffer.data.write(pcmData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadEnd(String id, byte[] pcmData, int sampleRate) {
        LoadingBuffer buffer = loadingBuffers.remove(id);

        byte[] fullData;
        int finalSampleRate;

        if (buffer != null) {
            try {
                if (pcmData.length > 0) {
                    buffer.data.write(pcmData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            fullData = buffer.data.toByteArray();
            finalSampleRate = buffer.sampleRate;
        } else {
            fullData = pcmData;
            finalSampleRate = sampleRate;
        }

        if (fullData.length == 0) return;

        AudioSource source = new AudioSource(finalSampleRate);

        int bufferId = AL10.alGenBuffers();
        ByteBuffer audioBuffer = BufferUtils.createByteBuffer(fullData.length);
        audioBuffer.put(fullData).flip();

        AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, audioBuffer, finalSampleRate);
        AL10.alSourcei(source.sourceId, AL10.AL_BUFFER, bufferId);
        source.bufferQueue.add(bufferId);

        sources.put(id, source);
    }

    public static void loadAudio(String id, byte[] pcmData, int sampleRate) {
        if (sources.containsKey(id)) {
            sources.get(id).cleanup();
            sources.remove(id);
        }

        AudioSource source = new AudioSource(sampleRate);

        int bufferId = AL10.alGenBuffers();
        ByteBuffer audioBuffer = BufferUtils.createByteBuffer(pcmData.length);
        audioBuffer.put(pcmData).flip();

        AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, audioBuffer, sampleRate);
        AL10.alSourcei(source.sourceId, AL10.AL_BUFFER, bufferId);
        source.bufferQueue.add(bufferId);

        sources.put(id, source);
    }

    public static void streamAudio(String id, byte[] pcmData, int sampleRate) {
        AudioSource source = sources.get(id);

        if (source == null) {
            source = new AudioSource(sampleRate);
            source.streaming = true;
            sources.put(id, source);
        }

        int bufferId = AL10.alGenBuffers();
        ByteBuffer audioBuffer = BufferUtils.createByteBuffer(pcmData.length);
        audioBuffer.put(pcmData).flip();

        AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, audioBuffer, sampleRate);
        AL10.alSourceQueueBuffers(source.sourceId, bufferId);
        source.bufferQueue.add(bufferId);

        if (source.playing && AL10.alGetSourcei(source.sourceId, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            AL10.alSourcePlay(source.sourceId);
        }
    }

    public static void play(String id, float volume, boolean loop) {
        AudioSource source = sources.get(id);
        if (source == null) return;
        source.is3D = false;

        source.baseVolume = volume;
        float finalVolume = volume * getVolumeMultiplier();

        AL10.alSourcei(source.sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSource3f(source.sourceId, AL10.AL_POSITION, 0, 0, 0);
        AL10.alSourcef(source.sourceId, AL10.AL_GAIN, finalVolume);
        AL10.alSourcei(source.sourceId, AL10.AL_LOOPING, loop && !source.streaming ? AL10.AL_TRUE : AL10.AL_FALSE);
        AL10.alSourcePlay(source.sourceId);
        source.playing = true;
    }

    public static void play3d(String id, float x, float y, float z, float volume, float rolloff, boolean loop) {
        AudioSource source = sources.get(id);
        if (source == null) return;

        source.baseVolume = volume;

        source.is3D = true;
        source.x = x;
        source.y = y;
        source.z = z;
        source.maxDistance = 100.0f;

        float finalVolume = volume * getVolumeMultiplier();

        AL10.alSourcef(source.sourceId, AL10.AL_GAIN, finalVolume);
        AL10.alSourcei(source.sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        AL10.alSource3f(source.sourceId, AL10.AL_POSITION, x, y, z);
        AL10.alSourcef(source.sourceId, AL10.AL_ROLLOFF_FACTOR, rolloff);
        AL10.alSourcef(source.sourceId, AL10.AL_REFERENCE_DISTANCE, 5.0f);

        AL10.alSourcef(source.sourceId, AL10.AL_MAX_DISTANCE, 99999.0f);

        AL10.alSourcei(source.sourceId, AL10.AL_LOOPING, loop && !source.streaming ? AL10.AL_TRUE : AL10.AL_FALSE);

        AL10.alSourcePlay(source.sourceId);
        source.playing = true;
    }

    public static void pause(String id) {
        AudioSource source = sources.get(id);
        if (source != null) {
            AL10.alSourcePause(source.sourceId);
            source.playing = false;
        }
    }

    public static void stop(String id) {
        AudioSource source = sources.get(id);
        if (source != null) {
            AL10.alSourceStop(source.sourceId);
            source.playing = false;
        }
    }

    public static void unload(String id) {
        AudioSource source = sources.remove(id);
        if (source != null) {
            source.cleanup();
        }
        loadingBuffers.remove(id);
    }

    public static void setVolume(String id, float volume) {
        AudioSource source = sources.get(id);
        if (source != null) {
            source.baseVolume = volume;
            float finalVolume = volume * getVolumeMultiplier();
            AL10.alSourcef(source.sourceId, AL10.AL_GAIN, finalVolume);
        }
    }

    public static void setPosition(String id, float x, float y, float z) {
        AudioSource source = sources.get(id);
        if (source != null) {
            source.x = x;
            source.y = y;
            source.z = z;
            AL10.alSource3f(source.sourceId, AL10.AL_POSITION, x, y, z);
        }
    }

    public static void updateListener(float x, float y, float z, float yaw, float pitch) {
        listenerX = x;
        listenerY = y;
        listenerZ = z;

        AL10.alListener3f(AL10.AL_POSITION, x, y, z);

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float frontX = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        float frontY = -MathHelper.sin(pitchRad);
        float frontZ = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

        float[] orientation = {frontX, frontY, frontZ, 0, 1, 0};
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
    }

    public static void update() {
        float volumeMultiplier = getVolumeMultiplier();

        for (AudioSource source : sources.values()) {
            if (source.playing) {
                float finalVolume = source.baseVolume * volumeMultiplier;
                if (source.is3D) {
                    double distSq = (source.x - listenerX) * (source.x - listenerX) +
                            (source.y - listenerY) * (source.y - listenerY) +
                            (source.z - listenerZ) * (source.z - listenerZ);

                    if (distSq > source.maxDistance * source.maxDistance) {
                        finalVolume = 0.0f;
                    }
                }
                AL10.alSourcef(source.sourceId, AL10.AL_GAIN, finalVolume);
            }

            if (source.streaming) {
                int processed = AL10.alGetSourcei(source.sourceId, AL10.AL_BUFFERS_PROCESSED);
                while (processed > 0) {
                    int bufferId = AL10.alSourceUnqueueBuffers(source.sourceId);
                    AL10.alDeleteBuffers(bufferId);
                    source.bufferQueue.remove(bufferId);
                    processed--;
                }
            }
        }
    }

    public static void cleanup() {
        for (String id : sources.keySet()) {
            unload(id);
        }
    }

    public static void cloneAudio(String newId, String sourceId) {
        AudioSource src = sources.get(sourceId);
        if (src == null) {
            System.out.println("[Audio] Clone failed: source " + sourceId + " not found");
            return;
        }


        AudioSource newSource = new AudioSource(src.sampleRate);
        newSource.baseVolume = src.baseVolume;

        for (Integer bufferId : src.bufferQueue) {
            if (AL10.alIsBuffer(bufferId)) {
                AL10.alSourceQueueBuffers(newSource.sourceId, bufferId);
                newSource.bufferQueue.add(bufferId);
            }
        }

        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            System.out.println("[Audio] OpenAL Error in clone: " + error);
        }

        sources.put(newId, newSource);
        System.out.println("[Audio] Cloned: " + sourceId + " -> " + newId);
    }
}