using System;

namespace MCAPIBridge
{
    /// <summary>
    /// Library for IO block
    /// </summary>
    public class IOManager
    {
        private readonly MinecraftClient _mc;

        internal IOManager(MinecraftClient mc) => _mc = mc;

        /// <summary>HAL_GPIO_WritePin(GPIO_PIN_X, Value(0-15))</summary>
        public void Write(int channelId, int value)
        {
            var power = Math.Max(0, Math.Min(15, value));
            _mc.Send($"io.write({channelId},{power})");
        }

        /// <summary>HAL_GPIO_WritePin(GPIO_PIN_X, GPIO_PIN_(RE)SET);</summary>
        public void Write(int channelId, bool value) => Write(channelId, value ? 15 : 0);

        /// <summary>HAL_GPIO_ReadPin(GPIO_PIN_X);</summary>
        public int Read(int channelId)
        {
            _mc.Send($"io.read({channelId})");
            return int.TryParse(_mc.Receive(), out var result) ? result : 0;
        }

        /// <summary>HAL_GPIO_ReadPin(GPIO_PIN_X) > threshold</summary>
        public bool IsHigh(int channelId, int threshold = 7) => Read(channelId) > threshold;

        /// <summary>HAL_GPIO_ReadPin(GPIO_PIN_X) <= threshold</summary>
        public bool IsLow(int channelId, int threshold = 7) => Read(channelId) <= threshold;

        /// <summary>HAL_GPIO_Init(x,y,z,GPIO_PIN_X,GPIO_MODE_XX,dimension)</summary>
        public void Config(int x, int y, int z, int channelId, bool isOutput, string dimension = "")
        {
            _mc.Send($"io.config({x},{y},{z},{channelId},{(isOutput ? "true" : "false")},{dimension})");
        }

        /// <summary>HAL_GPIO_Init(x,y,z,GPIO_PIN_X,GPIO_MODE_XX,dimension)</summary>
        public void Config(int x, int y, int z, int channelId, string mode, string dimension = "")
        {
            var isOutput = mode.ToLower() is "out" or "output";
            Config(x, y, z, channelId, isOutput, dimension);
        }
    }
}