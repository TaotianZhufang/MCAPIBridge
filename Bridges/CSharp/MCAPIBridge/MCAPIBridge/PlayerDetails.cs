namespace MCAPIBridge
{
    /// <summary>Player Details</summary>
    public class PlayerDetails
    {
        public string Name { get; set; }
        public int Id { get; set; }
        public string Mode { get; set; }
        public float Health { get; set; }
        public float MaxHealth { get; set; }
        public int Food { get; set; }
        public string HeldItem { get; set; }
        public int HeldCount { get; set; }

        public PlayerDetails(string name, int id, string mode, float health, float maxHealth, int food, string heldItem, int heldCount)
        {
            Name = name;
            Id = id;
            Mode = mode;
            Health = health;
            MaxHealth = maxHealth;
            Food = food;
            HeldItem = heldItem;
            HeldCount = heldCount;
        }

        public override string ToString()
        {
            return string.Format("{0} (ID:{1}, {2}, HP:{3}/{4})", Name, Id, Mode, Health, MaxHealth);
        }
    }
}