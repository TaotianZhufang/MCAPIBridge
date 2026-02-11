using System;

namespace MCAPIBridge
{
    /// <summary>3D Vector</summary>
    public class Vec3
    {
        public double X { get; set; }
        public double Y { get; set; }
        public double Z { get; set; }

        public Vec3(double x, double y, double z)
        {
            X = x;
            Y = y;
            Z = z;
        }

        public double Length()
        {
            return Math.Sqrt(X * X + Y * Y + Z * Z);
        }

        public static Vec3 operator +(Vec3 a, Vec3 b)
        {
            return new Vec3(a.X + b.X, a.Y + b.Y, a.Z + b.Z);
        }

        public static Vec3 operator -(Vec3 a, Vec3 b)
        {
            return new Vec3(a.X - b.X, a.Y - b.Y, a.Z - b.Z);
        }

        public static Vec3 operator *(Vec3 v, double s)
        {
            return new Vec3(v.X * s, v.Y * s, v.Z * s);
        }

        public override string ToString()
        {
            return string.Format("Vec3({0:F2}, {1:F2}, {2:F2})", X, Y, Z);
        }
    }
}