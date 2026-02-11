namespace MCAPIBridge
{
    /// <summary>Screen Location</summary>
    public class ScreenLocation
    {
        public double X { get; private set; }
        public double Y { get; private set; }
        public double Z { get; private set; }
        public string Dimension { get; private set; }

        public ScreenLocation(double x, double y, double z, string dimension = "minecraft:overworld")
        {
            X = x;
            Y = y;
            Z = z;
            Dimension = dimension;
        }

        public override string ToString()
        {
            return string.Format("Loc({0:F1}, {1:F1}, {2:F1}, {3})", X, Y, Z, Dimension);
        }
    }
}