using System;

namespace MCAPIBridge
{
    /// <summary>Player Position,Yaw,Pitch</summary>
    public class PlayerPos : Vec3
    {
        public float Yaw { get; set; }
        public float Pitch { get; set; }

        public PlayerPos(double x, double y, double z, float yaw, float pitch) : base(x, y, z)
        {
            Yaw = yaw;
            Pitch = pitch;
        }

        public Vec3 Forward(double distance = 1.0)
        {
            var yawRad = Yaw * Math.PI / 180.0;
            var pitchRad = Pitch * Math.PI / 180.0;

            var vx = -Math.Sin(yawRad) * Math.Cos(pitchRad);
            var vy = -Math.Sin(pitchRad);
            var vz = Math.Cos(yawRad) * Math.Cos(pitchRad);

            return new Vec3(X + vx * distance, Y + vy * distance, Z + vz * distance);
        }

        public override string ToString()
        {
            return string.Format("PlayerPos(x={0:F1}, y={1:F1}, z={2:F1}, yaw={3:F1}, pitch={4:F1})", X, Y, Z, Yaw, Pitch);
        }
    }
}