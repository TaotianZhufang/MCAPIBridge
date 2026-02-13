package org.taskchou.mcapibridge.client;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.BufferUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioPlayer {

    private static final ConcurrentHashMap<String, AudioSource> sources = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LoadingBuffer> loadingBuffers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Integer>> bufferCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastPlayTime = new ConcurrentHashMap<>();

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
        public String dimension = "minecraft:overworld";

        public long playStartRealTime = 0;
        public float playStartOffset = 0;

        public boolean is3D = false;
        public float x, y, z;
        public float maxDistance = 100.0f;

        public AudioSource(int sampleRate) {
            this.sampleRate = sampleRate;
            this.sourceId = AL10.alGenSources();
            int err = AL10.alGetError();
            if (err != AL10.AL_NO_ERROR) {
                System.err.println("[AudioPlayer] alGenSources error: " + err);
                this.sourceId = -1;
            }
        }

        public void cleanup() {
            System.out.println("[AudioPlayer] cleanup called, releasing " + sources.size() + " sources");
            if (sourceId == -1) return;
            AL10.alSourceStop(sourceId);
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0);
            AL10.alDeleteSources(sourceId);
            sourceId = -1;
            bufferQueue.clear();
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
        } catch (Exception e) {}
        return ModConfig.get().customAudioVolume;
    }

    public static void loadStart(String id, byte[] pcmData, int sampleRate) {
        AudioSource old = sources.remove(id);
        if (old != null) old.cleanup();
        loadingBuffers.remove(id);

        LoadingBuffer buffer = new LoadingBuffer();
        buffer.sampleRate = sampleRate;
        try { buffer.data.write(pcmData); } catch (Exception e) {}
        loadingBuffers.put(id, buffer);
    }

    public static void loadContinue(String id, byte[] pcmData) {
        LoadingBuffer buffer = loadingBuffers.get(id);
        if (buffer != null) {
            try { buffer.data.write(pcmData); } catch (Exception e) {}
        }
    }

    public static void loadEnd(String id, byte[] pcmData, int sampleRate) {
        LoadingBuffer buffer = loadingBuffers.remove(id);

        byte[] fullData;
        int finalSampleRate;

        if (buffer != null) {
            try { if (pcmData.length > 0) buffer.data.write(pcmData); } catch (Exception e) {}
            fullData = buffer.data.toByteArray();
            finalSampleRate = buffer.sampleRate;
        } else {
            fullData = pcmData;
            finalSampleRate = sampleRate;
        }

        if (fullData.length == 0) return;
        doLoad(id, fullData, finalSampleRate);
    }

    public static void loadAudio(String id, byte[] pcmData, int sampleRate) {
        AudioSource old = sources.remove(id);
        if (old != null) old.cleanup();
        doLoad(id, pcmData, sampleRate);
    }

    private static void doLoad(String id, byte[] pcmData, int sampleRate) {
        deleteBuffersIfOwned(id);

        AL10.alGetError();

        AudioSource source = new AudioSource(sampleRate);
        if (source.sourceId == -1) {
            System.out.println("[AudioPlayer] FAILED to create source! Total sources: " + sources.size());
            return;
        }

        int bufferId = AL10.alGenBuffers();
        ByteBuffer audioBuffer = BufferUtils.createByteBuffer(pcmData.length);
        audioBuffer.put(pcmData).flip();

        AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, audioBuffer, sampleRate);

        int alSize = AL10.alGetBufferi(bufferId, AL10.AL_SIZE);
        int alFreq = AL10.alGetBufferi(bufferId, AL10.AL_FREQUENCY);
        int alBits = AL10.alGetBufferi(bufferId, AL10.AL_BITS);
        int alChannels = AL10.alGetBufferi(bufferId, AL10.AL_CHANNELS);
        float alDuration = (float) alSize / (alFreq * (alBits / 8) * alChannels);
        System.out.println("[AudioPlayer] Buffer info: size=" + alSize
                + " freq=" + alFreq
                + " bits=" + alBits
                + " channels=" + alChannels
                + " duration=" + alDuration + "s"
                + " inputSize=" + pcmData.length);

        AL10.alSourcei(source.sourceId, AL10.AL_BUFFER, bufferId);
        source.bufferQueue.add(bufferId);

        List<Integer> buffers = new ArrayList<>();
        buffers.add(bufferId);
        bufferCache.put(id, buffers);

        sources.put(id, source);
        System.out.println("[AudioPlayer] Loaded " + id + " (" + pcmData.length + " bytes, " + sampleRate + "Hz)");
    }

    public static void streamAudio(String id, byte[] pcmData, int sampleRate) {
        AudioSource source = sources.get(id);
        if (source == null) {
            source = new AudioSource(sampleRate);
            if (source.sourceId == -1) return;
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
        bufferCache.computeIfAbsent(id, k -> new ArrayList<>()).add(bufferId);
    }

    public static void play(String id, float volume, boolean loop, float offset) {
        long now = System.currentTimeMillis();
        Long last = lastPlayTime.get(id);
        if (last != null && now - last < 200) return;
        lastPlayTime.put(id, now);

        AudioSource source = sources.get(id);
        if (source == null || source.sourceId == -1) return;

        source.baseVolume = volume;
        float finalVolume = volume * getVolumeMultiplier();

        AL10.alSourcef(source.sourceId, AL10.AL_GAIN, finalVolume);
        AL10.alSourcei(source.sourceId, AL10.AL_LOOPING,
                loop && !source.streaming ? AL10.AL_TRUE : AL10.AL_FALSE);

        if (!source.is3D) {
            AL10.alSourcei(source.sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(source.sourceId, AL10.AL_POSITION, 0, 0, 0);
        }

        int state = AL10.alGetSourcei(source.sourceId, AL10.AL_SOURCE_STATE);
        if (state == AL10.AL_PLAYING || state == AL10.AL_PAUSED) {
            AL10.alSourceStop(source.sourceId);
        }

        if (offset > 0.01f) {
            seekTo(source, offset, loop);
        }
        AL10.alSourcePlay(source.sourceId);

        source.playing = true;
    }

    public static void play3d(String id, float x, float y, float z, float volume,
                              float rolloff, boolean loop, String dimension, float offset) {
        long now = System.currentTimeMillis();
        Long last = lastPlayTime.get(id);
        if (last != null && now - last < 200) return;
        lastPlayTime.put(id, now);

        AudioSource source = sources.get(id);
        if (source == null || source.sourceId == -1) return;

        source.baseVolume = volume;
        source.is3D = true;
        source.x = x; source.y = y; source.z = z;
        source.maxDistance = 100.0f;
        source.dimension = dimension;

        float userOffset = ModConfig.get().audioSyncOffset;
        float finalOffset = offset + userOffset;
        if (finalOffset < 0) finalOffset = 0;

        float finalVolume = volume * getVolumeMultiplier();

        AL10.alSourcef(source.sourceId, AL10.AL_GAIN, finalVolume);
        AL10.alSourcei(source.sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        AL10.alSource3f(source.sourceId, AL10.AL_POSITION, x, y, z);
        AL10.alSourcef(source.sourceId, AL10.AL_ROLLOFF_FACTOR, rolloff);
        AL10.alSourcef(source.sourceId, AL10.AL_REFERENCE_DISTANCE, 5.0f);
        AL10.alSourcef(source.sourceId, AL10.AL_MAX_DISTANCE, 99999.0f);
        AL10.alSourcei(source.sourceId, AL10.AL_LOOPING,
                loop && !source.streaming ? AL10.AL_TRUE : AL10.AL_FALSE);

        int state = AL10.alGetSourcei(source.sourceId, AL10.AL_SOURCE_STATE);
        if (state == AL10.AL_PLAYING || state == AL10.AL_PAUSED) {
            AL10.alSourceStop(source.sourceId);
        }

        if (finalOffset > 0.01f) {
            seekTo(source, finalOffset, loop);
        }
        AL10.alSourcePlay(source.sourceId);

        source.playing = true;
    }

    private static void seekTo(AudioSource source, float offset, boolean loop) {
        int bufSize = 0;
        if (!source.bufferQueue.isEmpty()) {
            bufSize = AL10.alGetBufferi(source.bufferQueue.peek(), AL10.AL_SIZE);
        }
        float totalDuration = (float) bufSize / (source.sampleRate * 2);

        float seekOffset = offset;
        if (totalDuration > 0 && seekOffset >= totalDuration) {
            if (loop) seekOffset = seekOffset % totalDuration;
            else seekOffset = totalDuration - 0.01f;
        }
        if (seekOffset < 0) seekOffset = 0;

        AL10.alSourceRewind(source.sourceId);
        AL10.alSourcef(source.sourceId, 0x1024, seekOffset);
    }

    public static void resume(String id) {
        AudioSource source = sources.get(id);
        if (source == null || source.sourceId == -1) return;
        int state = AL10.alGetSourcei(source.sourceId, AL10.AL_SOURCE_STATE);
        if (state == AL10.AL_PAUSED) {
            AL10.alSourcePlay(source.sourceId);
            source.playing = true;
        }
    }

    public static void pause(String id) {
        AudioSource source = sources.get(id);
        if (source != null && source.sourceId != -1) {
            AL10.alSourcePause(source.sourceId);
            source.playing = false;
        }
    }

    public static void stop(String id) {
        AudioSource source = sources.get(id);
        if (source != null && source.sourceId != -1) {
            AL10.alSourceStop(source.sourceId);
            source.playing = false;
        }
    }

    public static void stopAll() {
        for (AudioSource source : sources.values()) {
            if (source.sourceId != -1) {
                AL10.alSourceStop(source.sourceId);
                source.playing = false;
            }
        }
    }

    public static void setVolume(String id, float volume) {
        AudioSource source = sources.get(id);
        if (source != null && source.sourceId != -1) {
            source.baseVolume = volume;
            AL10.alSourcef(source.sourceId, AL10.AL_GAIN, volume * getVolumeMultiplier());
        }
    }

    public static void setPosition(String id, float x, float y, float z) {
        AudioSource source = sources.get(id);
        if (source != null && source.sourceId != -1) {
            source.x = x; source.y = y; source.z = z;
            AL10.alSource3f(source.sourceId, AL10.AL_POSITION, x, y, z);
        }
    }

    public static void updateListener(float x, float y, float z, float yaw, float pitch) {
        listenerX = x; listenerY = y; listenerZ = z;
        AL10.alListener3f(AL10.AL_POSITION, x, y, z);

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float frontX = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        float frontY = -MathHelper.sin(pitchRad);
        float frontZ = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

        AL10.alListenerfv(AL10.AL_ORIENTATION, new float[]{frontX, frontY, frontZ, 0, 1, 0});
    }
    private static final ConcurrentHashMap<String, Long> driftLogTime = new ConcurrentHashMap<>();


    public static void update() {
        long now = System.currentTimeMillis();

        for (var entry : sources.entrySet()) {
            AudioSource source = entry.getValue();
            if (source.sourceId == -1 || !source.playing) continue;

            int state = AL10.alGetSourcei(source.sourceId, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_PLAYING && source.playStartRealTime > 0) {
                int bytePos = AL10.alGetSourcei(source.sourceId, AL11.AL_BYTE_OFFSET);
                float audioPos = (float) bytePos / (source.sampleRate * 2);

                float realElapsed = (now - source.playStartRealTime) / 1000.0f;
                float expectedPos = source.playStartOffset + realElapsed;

                Long lastLog = driftLogTime.getOrDefault(entry.getKey(), 0L);
                if (true) {
                    driftLogTime.put(entry.getKey(), now);
                    System.out.println("[AudioPlayer] DRIFT " + entry.getKey()
                            + " audioPos=" + String.format("%.2f", audioPos) + "s"
                            + " expectedPos=" + String.format("%.2f", expectedPos) + "s"
                            + " diff=" + String.format("%.2f", audioPos - expectedPos) + "s");
                }
            }
        }

        float volumeMultiplier = getVolumeMultiplier();
        String currentDimension = "";
        if (clientInstance != null && clientInstance.world != null) {
            currentDimension = clientInstance.world.getRegistryKey().getValue().toString();
        }

        for (AudioSource source : sources.values()) {
            if (source.sourceId == -1) continue;

            if (source.playing) {
                float finalVolume = source.baseVolume * volumeMultiplier;
                if (source.is3D) {
                    if (!source.dimension.equals(currentDimension)) {
                        finalVolume = 0.0f;
                    } else {
                        double distSq = (source.x - listenerX) * (source.x - listenerX) +
                                (source.y - listenerY) * (source.y - listenerY) +
                                (source.z - listenerZ) * (source.z - listenerZ);
                        if (distSq > source.maxDistance * source.maxDistance) {
                            finalVolume = 0.0f;
                        }
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

    public static void cloneAudio(String newId, String sourceId) {
        List<Integer> srcBuffers = bufferCache.get(sourceId);
        AudioSource src = sources.get(sourceId);
        if (src == null || srcBuffers == null || srcBuffers.isEmpty()) {
            System.err.println("[AudioPlayer] Clone failed: source not found " + sourceId);
            return;
        }

        AudioSource old = sources.remove(newId);
        if (old != null) old.cleanup();

        AudioSource newSource = new AudioSource(src.sampleRate);
        if (newSource.sourceId == -1) return;

        int bufferId = srcBuffers.get(0);
        AL10.alSourcei(newSource.sourceId, AL10.AL_BUFFER, bufferId);
        newSource.bufferQueue.add(bufferId);

        sources.put(newId, newSource);
        bufferCache.put(newId, srcBuffers);
    }

    public static void unload(String id) {
        AudioSource source = sources.remove(id);
        if (source != null) source.cleanup();
        deleteBuffersIfOwned(id);
    }

    private static void deleteBuffersIfOwned(String id) {
        List<Integer> buffers = bufferCache.remove(id);
        if (buffers == null) return;

        for (var entry : bufferCache.entrySet()) {
            if (entry.getValue() == buffers) return;
        }
        for (int buf : buffers) {
            AL10.alDeleteBuffers(buf);
        }
    }

    public static void cleanup() {
        for (AudioSource source : sources.values()) {
            if (source.sourceId != -1) {
                AL10.alSourceStop(source.sourceId);
                AL10.alSourcei(source.sourceId, AL10.AL_BUFFER, 0);
                AL10.alDeleteSources(source.sourceId);
            }
        }
        sources.clear();
        loadingBuffers.clear();
        lastPlayTime.clear();

        Set<Integer> allBuffers = new HashSet<>();
        for (List<Integer> bufs : bufferCache.values()) {
            allBuffers.addAll(bufs);
        }
        for (int buf : allBuffers) {
            AL10.alDeleteBuffers(buf);
        }
        bufferCache.clear();
        driftLogTime.clear();
    }
}