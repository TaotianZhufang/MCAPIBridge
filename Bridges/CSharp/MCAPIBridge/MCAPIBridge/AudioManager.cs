using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;

namespace MCAPIBridge
{
    /// <summary>Audio Manager</summary>
    public class AudioManager
    {
        private readonly MinecraftClient _mc;

        internal AudioManager(MinecraftClient mc)
        {
            _mc = mc;
        }

        /// <summary>Load WAV</summary>
        public void LoadWav(string target, string audioId, string filepath)
        {
            using (var reader = new BinaryReader(File.OpenRead(filepath)))
            {
                reader.ReadBytes(22);
                var channels = reader.ReadInt16();
                var sampleRate = reader.ReadInt32();
                reader.ReadBytes(6);
                var bitsPerSample = reader.ReadInt16();

                while (true)
                {
                    var chunkId = new string(reader.ReadChars(4));
                    var chunkSize = reader.ReadInt32();

                    if (chunkId == "data")
                    {
                        var frames = reader.ReadBytes(chunkSize);

                        if (channels == 2)
                        {
                            var monoSamples = new List<short>();
                            for (int i = 0; i < frames.Length; i += 4)
                            {
                                var left = BitConverter.ToInt16(frames, i);
                                var right = BitConverter.ToInt16(frames, i + 2);
                                monoSamples.Add((short)((left + right) / 2));
                            }
                            frames = monoSamples.SelectMany(s => BitConverter.GetBytes(s)).ToArray();
                        }

                        if (bitsPerSample == 8)
                        {
                            var samples16 = frames.Select(b => (short)((b - 128) * 256)).ToArray();
                            frames = samples16.SelectMany(s => BitConverter.GetBytes(s)).ToArray();
                        }

                        var b64Data = Convert.ToBase64String(frames);
                        const int chunkSizeB64 = 40000;

                        for (int i = 0; i < b64Data.Length; i += chunkSizeB64)
                        {
                            var length = Math.Min(chunkSizeB64, b64Data.Length - i);
                            var chunk = b64Data.Substring(i, length);

                            if (i == 0)
                                _mc.Send(string.Format("audio.load({0},{1},{2},{3})", target, audioId, sampleRate, chunk));
                            else
                                _mc.Send(string.Format("audio.stream({0},{1},{2},{3})", target, audioId, sampleRate, chunk));

                            Thread.Sleep(1);
                        }

                        _mc.Send(string.Format("audio.finishLoad({0},{1})", target, audioId));
                        Console.WriteLine(string.Format("[Audio] Loaded {0}: {1} bytes, {2}Hz", filepath, frames.Length, sampleRate));
                        break;
                    }
                    else
                    {
                        reader.ReadBytes(chunkSize);
                    }
                }
            }
        }

        /// <summary>ZLoad PCM</summary>
        public void LoadRaw(string target, string audioId, byte[] pcmData, int sampleRate = 44100)
        {
            var b64Data = Convert.ToBase64String(pcmData);
            SendAudioData(target, audioId, sampleRate, b64Data);
            Console.WriteLine(string.Format("[Audio] Loaded raw: {0} bytes, {1}Hz", pcmData.Length, sampleRate));
        }

        /// <summary>Generate Sine Wave Tone</summary>
        public void GenerateTone(string target, string audioId, double frequency = 440, double duration = 1.0, int sampleRate = 44100)
        {
            int numSamples = (int)(sampleRate * duration);
            var samples = new List<short>();

            for (int i = 0; i < numSamples; i++)
            {
                double t = (double)i / sampleRate;
                double value = Math.Sin(2 * Math.PI * frequency * t);
                samples.Add((short)(value * 32767));
            }

            var pcmData = samples.SelectMany(s => BitConverter.GetBytes(s)).ToArray();
            LoadRaw(target, audioId, pcmData, sampleRate);
            Console.WriteLine(string.Format("[Audio] Generated tone: {0}Hz, {1}s", frequency, duration));
        }

        /// <summary>Send Audio To Server</summary>
        private void SendAudioData(string target, string audioId, int sampleRate, string b64Data)
        {
            const int chunkSize = 40000;

            for (int i = 0; i < b64Data.Length; i += chunkSize)
            {
                var length = Math.Min(chunkSize, b64Data.Length - i);
                var chunk = b64Data.Substring(i, length);

                if (i == 0)
                    _mc.Send(string.Format("audio.load({0},{1},{2},{3})", target, audioId, sampleRate, chunk));
                else
                    _mc.Send(string.Format("audio.stream({0},{1},{2},{3})", target, audioId, sampleRate, chunk));

                Thread.Sleep(1);
            }

            _mc.Send(string.Format("audio.finishLoad({0},{1})", target, audioId));
        }

        /// <summary>Audio Play</summary>
        public void Play(string target, string audioId, float volume = 1.0f, bool loop = false)
        {
            _mc.Send(string.Format("audio.play({0},{1},{2},{3})", target, audioId, volume, loop ? "true" : "false"));
        }

        /// <summary>3D Audio Play</summary>
        public void Play3D(string target, string audioId, double x, double y, double z,
                           float volume = 1.0f, float rolloff = 1.0f, bool loop = false,
                           string dimension = "", float offset = 0.0f)
        {
            _mc.Send(string.Format("audio.play3d({0},{1},{2},{3},{4},{5},{6},{7},{8},{9})",
                target, audioId, x, y, z, volume, rolloff, loop ? "true" : "false", dimension, offset));
        }

        /// <summary>Play On Screen</summary>
        public void PlayOnScreen(string audioId, int screenId, float volume = 1.0f, bool loop = false)
        {
            _mc.Send(string.Format("audio.playScreen(@a,{0},{1},{2},{3})", audioId, screenId, volume, loop ? "true" : "false"));
        }

        /// <summary>Stop</summary>
        public void Stop(string target, string audioId)
        {
            _mc.Send(string.Format("audio.stop({0},{1})", target, audioId));
        }

        /// <summary>Pause</summary>
        public void Pause(string target, string audioId)
        {
            _mc.Send(string.Format("audio.pause({0},{1})", target, audioId));
        }

        /// <summary>Unload Audio</summary>
        public void Unload(string target, string audioId)
        {
            _mc.Send(string.Format("audio.unload({0},{1})", target, audioId));
        }

        /// <summary>Set Volume</summary>
        public void SetVolume(string target, string audioId, float volume)
        {
            _mc.Send(string.Format("audio.volume({0},{1},{2})", target, audioId, volume));
        }

        /// <summary>Set 3D Audio Position</summary>
        public void SetPosition(string target, string audioId, double x, double y, double z)
        {
            _mc.Send(string.Format("audio.position({0},{1},{2},{3},{4})", target, audioId, x, y, z));
        }

        /// <summary>Clone Audio</summary>
        public void Clone(string target, string sourceId, string newId)
        {
            _mc.Send(string.Format("audio.clone({0},{1},{2})", target, sourceId, newId));
        }

        /// <summary>Reset Audio</summary>
        public void Reset()
        {
            _mc.Send("audio.reset(@a)");
        }

        /// <summary>Sync Audio Progress</summary>
        public void SyncProgress(string audioId, float progress)
        {
            _mc.Send(string.Format("audio.syncProgress(@a,{0},{1})", audioId, progress));
        }
    }
}