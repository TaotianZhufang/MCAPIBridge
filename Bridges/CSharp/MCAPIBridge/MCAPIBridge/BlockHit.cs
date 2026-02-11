namespace MCAPIBridge
{
    /// <summary>Block Hit Event</summary>
    public class BlockHit
    {
        public Vec3 Pos { get; private set; }
        public int Face { get; private set; }
        public int EntityId { get; private set; }
        public int Action { get; private set; }
        public string Type { get; private set; }

        public BlockHit(int x, int y, int z, int face, int entityId, int action)
        {
            Pos = new Vec3(x, y, z);
            Face = face;
            EntityId = entityId;
            Action = action;

            if (action == 1)
                Type = "LEFT_CLICK";
            else if (action == 2)
                Type = "RIGHT_CLICK";
            else if (action > 100)
                Type = "KEY_MACRO_" + (action - 100);
            else
                Type = "UNKNOWN";
        }
    }
}