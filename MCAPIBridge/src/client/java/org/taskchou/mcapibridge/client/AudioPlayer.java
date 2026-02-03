package org.taskchou.mcapibridge.client;

import org.lwjgl.openal.AL10;
import org.lwjgl.BufferUtils;
import net.minecraft.util.math.MathHelper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioPlayer {

    private static final ConcurrentHashMap<String, AudioSource> sources = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, LoadingBuffer> loadingBuffers = new ConcurrentHashMap<>();

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

        System.out.println("[Audio] Load start: " + id + ", chunk: " + pcmData.length + " bytes");
    }

    public static void loadContinue(String id, byte[] pcmData) {
        LoadingBuffer buffer = loadingBuffers.get(id);
        if (buffer != null) {
            try {
                buffer.data.write(pcmData);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("[Audio] Load continue: " + id + ", chunk: " + pcmData.length + " bytes");
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

        if (fullData.length == 0) {
            System.out.println("[Audio] Warning: empty audio data for " + id);
            return;
        }

        AudioSource source = new AudioSource(finalSampleRate);

        int bufferId = AL10.alGenBuffers();
        ByteBuffer audioBuffer = BufferUtils.createByteBuffer(fullData.length);
        audioBuffer.put(fullData).flip();

        AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, audioBuffer, finalSampleRate);
        AL10.alSourcei(source.sourceId, AL10.AL_BUFFER, bufferId);
        source.bufferQueue.add(bufferId);

        sources.put(id, source);
        System.out.println("[Audio] Load complete: " + id + ", total: " + fullData.length + " bytes");
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
        System.out.println("[Audio] Loaded: " + id + ", size: " + pcmData.length + " bytes");
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
        if (source == null) {
            System.out.println("[Audio] Source not found: " + id);
            return;
        }

        AL10.alSourcei(source.sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSource3f(source.sourceId, AL10.AL_POSITION, 0, 0, 0);
        AL10.alSourcef(source.sourceId, AL10.AL_GAIN, volume);
        AL10.alSourcei(source.sourceId, AL10.AL_LOOPING, loop && !source.streaming ? AL10.AL_TRUE : AL10.AL_FALSE);
        AL10.alSourcePlay(source.sourceId);
        source.playing = true;

        System.out.println("[Audio] Playing: " + id);
    }

    public static void play3d(String id, float x, float y, float z, float volume, float rolloff, boolean loop) {
        AudioSource source = sources.get(id);
        if (source == null) {
            System.out.println("[Audio] Source not found: " + id);
            return;
        }

        AL10.alSourcef(source.sourceId, AL10.AL_GAIN, volume);
        AL10.alSourcei(source.sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        AL10.alSource3f(source.sourceId, AL10.AL_POSITION, x, y, z);
        AL10.alSourcef(source.sourceId, AL10.AL_ROLLOFF_FACTOR, rolloff);
        AL10.alSourcef(source.sourceId, AL10.AL_REFERENCE_DISTANCE, 5.0f);
        AL10.alSourcef(source.sourceId, AL10.AL_MAX_DISTANCE, 100.0f);
        AL10.alSourcei(source.sourceId, AL10.AL_LOOPING, loop && !source.streaming ? AL10.AL_TRUE : AL10.AL_FALSE);

        AL10.alSourcePlay(source.sourceId);
        source.playing = true;

        System.out.println("[Audio] Playing 3D: " + id + " at (" + x + ", " + y + ", " + z + ")");
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
            System.out.println("[Audio] Unloaded: " + id);
        }
        loadingBuffers.remove(id);
    }

    public static void setVolume(String id, float volume) {
        AudioSource source = sources.get(id);
        if (source != null) {
            AL10.alSourcef(source.sourceId, AL10.AL_GAIN, volume);
        }
    }

    public static void setPosition(String id, float x, float y, float z) {
        AudioSource source = sources.get(id);
        if (source != null) {
            AL10.alSource3f(source.sourceId, AL10.AL_POSITION, x, y, z);
        }
    }

    public static void updateListener(float x, float y, float z, float yaw, float pitch) {
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
        for (AudioSource source : sources.values()) {
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
}