using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Sockets;
using System.Text;
using System.Threading;

namespace MCAPIBridge
{
    /// <summary>MCAPIBridge Client</summary>
    public class MinecraftClient : IDisposable
    {
        private TcpClient _client;
        private NetworkStream _stream;
        private StreamReader _reader;
        // private StreamWriter _writer;
        private bool _connected;

        public string Host { get; private set; }
        public int Port { get; private set; }

        public AudioManager Audio { get; private set; }
        public IOManager IO { get; private set; }

        public MinecraftClient(string host = "localhost", int port = 4711)
        {
            Host = host;
            Port = port;
            Audio = new AudioManager(this);
            IO = new IOManager(this);

            if (!Connect())
            {
                throw new Exception(string.Format("Can't connect Minecraft: ({0}:{1})", Host, Port));
            }
        }

        private bool Connect()
        {
            try
            {
                if (_client != null) _client.Close();

                _client = new TcpClient();
                _client.Connect(Host, Port);

                _client.ReceiveTimeout = 5000;
                _client.SendTimeout = 5000;

                _stream = _client.GetStream();
                _reader = new StreamReader(_stream, Encoding.UTF8);

                _connected = true;
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine("[MCAPI] Connection Error: " + ex.Message);
                _connected = false;
                return false;
            }
        }

        internal void Send(string command)
        {
            if (!_connected && !Connect()) return;

            try
            {
                byte[] data = Encoding.UTF8.GetBytes(command + "\n");

                _stream.Write(data, 0, data.Length);
                _stream.Flush();
            }
            catch (Exception ex)
            {
                Console.WriteLine("[MCAPI] Send Error: " + ex.Message);
                _connected = false;

   
                if (Connect())
                {
                    try
                    {
                        byte[] data = Encoding.UTF8.GetBytes(command + "\n");
                        _stream.Write(data, 0, data.Length);
                        _stream.Flush();
                    }
                    catch {  }
                }
            }
        }

        internal string Receive()
        {
            if (!_connected) return null;
            try
            {
                return _reader.ReadLine()?.Trim();
            }
            catch
            {
                _connected = false;
                return null;
            }
        }

        // ==================== Chat ====================

        /// <summary>Post Chat</summary>
        public void PostToChat(string message)
        {
            Send(string.Format("chat.post({0})", message));
        }

        /// <summary>Run Command</summary>
        public void RunCommand(string command)
        {
            if (command.StartsWith("/")) command = command.Substring(1);
            Send(string.Format("server.runCommand({0})", command));
        }

        // ==================== Block ====================

        /// <summary>Set Block</summary>
        public void SetBlock(int x, int y, int z, string blockId, string dimension = null)
        {
            if (dimension != null)
                Send(string.Format("world.setBlock({0},{1},{2},{3},{4})", x, y, z, blockId, dimension));
            else
                Send(string.Format("world.setBlock({0},{1},{2},{3})", x, y, z, blockId));
        }

        /// <summary>Get Block</summary>
        public string GetBlock(int x, int y, int z, string dimension = null)
        {
            if (dimension != null)
                Send(string.Format("world.getBlock({0},{1},{2},{3})", x, y, z, dimension));
            else
                Send(string.Format("world.getBlock({0},{1},{2})", x, y, z));
            var result = Receive();
            return result ?? "minecraft:air";
        }

        // ==================== Entity ====================

        /// <summary>Spawn Entity</summary>
        public int SpawnEntity(double x, double y, double z, string entityId, float yaw = 0, float pitch = 0, string dimension = null)
        {
            if (dimension != null)
                Send(string.Format("world.spawnEntity({0},{1},{2},{3},{4},{5},{6})", x, y, z, entityId, yaw, pitch, dimension));
            else
                Send(string.Format("world.spawnEntity({0},{1},{2},{3},{4},{5})", x, y, z, entityId, yaw, pitch));

            var result = Receive();
            int id;
            return int.TryParse(result, out id) ? id : -1;
        }

        /// <summary>Spawn Particle</summary>
        public void SpawnParticle(double x, double y, double z, string particleId, int count = 10,
                                  double dx = 0, double dy = 0, double dz = 0, double speed = 0, string dimension = null)
        {
            var cmd = string.Format("{0},{1},{2},{3},{4},{5},{6},{7},{8}", x, y, z, particleId, count, dx, dy, dz, speed);
            if (dimension != null) cmd += "," + dimension;
            Send(string.Format("world.spawnParticle({0})", cmd));
        }

        /// <summary>Set Entity Velocity</summary>
        public void SetEntityVelocity(int entityId, double vx, double vy, double vz)
        {
            Send(string.Format("entity.setVelocity({0},{1},{2},{3})", entityId, vx, vy, vz));
        }

        /// <summary>SetEntity No Gravity</summary>
        public void SetEntityNoGravity(int entityId, bool enable = true)
        {
            Send(string.Format("entity.setNoGravity({0},{1})", entityId, enable ? "true" : "false"));
        }

        /// <summary>Teleport Entity</summary>
        public void TeleportEntity(int entityId, double x, double y, double z)
        {
            Send(string.Format("entity.teleport({0},{1},{2},{3})", entityId, x, y, z));
        }

        /// <summary>Set Entity NBT</summary>
        public void SetEntityNbt(int entityId, string nbtString)
        {
            Send(string.Format("entity.setNbt({0},{1})", entityId, nbtString));
        }

        // ==================== Player ====================

        /// <summary>Get Player Position</summary>
        public PlayerPos GetPlayerPos(string target = "")
        {
            Send(string.Format("player.getPos({0})", target));
            var data = Receive();
            if (string.IsNullOrEmpty(data)) return new PlayerPos(0, 0, 0, 0, 0);

            var parts = data.Split(',');
            if (parts.Length >= 5)
            {
                return new PlayerPos(
                    double.Parse(parts[0]),
                    double.Parse(parts[1]),
                    double.Parse(parts[2]),
                    float.Parse(parts[3]),
                    float.Parse(parts[4])
                );
            }
            return new PlayerPos(0, 0, 0, 0, 0);
        }

        /// <summary>Get Player Direction Vector</summary>
        public Vec3 GetDirectionVector(string target = "")
        {
            var pos = GetPlayerPos(target);
            var yawRad = pos.Yaw * Math.PI / 180.0;
            var pitchRad = pos.Pitch * Math.PI / 180.0;

            return new Vec3(
                -Math.Sin(yawRad) * Math.Cos(pitchRad),
                -Math.Sin(pitchRad),
                Math.Cos(yawRad) * Math.Cos(pitchRad)
            );
        }

        /// <summary>/tp</summary>
        public void Teleport(double x, double y, double z, string target = "")
        {
            if (!string.IsNullOrEmpty(target))
                Send(string.Format("player.teleport({0},{1},{2},{3})", target, x, y, z));
            else
                Send(string.Format("player.teleport({0},{1},{2})", x, y, z));
        }

        /// <summary>Set Health</summary>
        public void SetHealth(string target, float amount)
        {
            Send(string.Format("player.setHealth({0},{1})", target, amount));
        }

        /// <summary>Set Food</summary>
        public void SetFood(string target, int amount)
        {
            Send(string.Format("player.setFood({0},{1})", target, amount));
        }

        /// <summary>/give</summary>
        public void Give(string target, string itemId, int count = 1)
        {
            Send(string.Format("player.give({0},{1},{2})", target, itemId, count));
        }

        /// <summary>Clear Inventory</summary>
        public void ClearInventory(string target, string itemId = "")
        {
            Send(string.Format("player.clear({0},{1})", target, itemId));
        }

        /// <summary>Give Effect</summary>
        public void GiveEffect(string target, string effectName, int durationSec = 30, int amplifier = 1)
        {
            Send(string.Format("player.effect({0},{1},{2},{3})", target, effectName, durationSec, amplifier));
        }

        /// <summary>Set Fly</summary>
        public void SetFlying(string target, bool allowFlight = true, bool isFlying = true)
        {
            Send(string.Format("player.setFlying({0},{1},{2})", target, allowFlight ? "true" : "false", isFlying ? "true" : "false"));
        }

        /// <summary>Set Fly Speed</summary>
        public void SetFlySpeed(string target, float speed = 0.05f)
        {
            Send(string.Format("player.setSpeed({0},true,{1})", target, speed));
        }

        /// <summary>Set Walk Speed</summary>
        public void SetWalkSpeed(string target, float speed = 0.1f)
        {
            Send(string.Format("player.setSpeed({0},false,{1})", target, speed));
        }

        /// <summary>Set God Mode</summary>
        public void SetGodMode(string target, bool enable = true)
        {
            Send(string.Format("player.setGod({0},{1})", target, enable ? "true" : "false"));
        }

        /// <summary>Player Look At</summary>
        public void LookAt(string target, double x, double y, double z)
        {
            Send(string.Format("player.lookAt({0},{1},{2},{3})", target, x, y, z));
        }

        /// <summary>Get Online Players</summary>
        public List<PlayerInfo> GetOnlinePlayers()
        {
            Send("world.getPlayers()");
            var data = Receive();
            var players = new List<PlayerInfo>();

            if (string.IsNullOrEmpty(data)) return players;

            foreach (var item in data.Split('|'))
            {
                var parts = item.Split(',');
                int id;
                if (parts.Length == 2 && int.TryParse(parts[1], out id))
                    players.Add(new PlayerInfo(parts[0], id));
            }
            return players;
        }

        /// <summary>Get Player Entity Id</summary>
        public int? GetPlayerEntityId(string name)
        {
            var players = GetOnlinePlayers();
            foreach (var p in players)
            {
                if (p.Name == name)
                    return p.Id;
            }
            return null;
        }

        /// <summary>Get Player Name</summary>
        public string GetPlayerName(int entityId)
        {
            var players = GetOnlinePlayers();
            foreach (var p in players)
            {
                if (p.Id == entityId)
                    return p.Name;
            }
            return null;
        }

        /// <summary>Get Inventory</summary>
        public List<InventoryItem> GetInventory(string target = "")
        {
            Send(string.Format("player.getInventory({0})", target));
            var data = Receive();
            var items = new List<InventoryItem>();

            if (string.IsNullOrEmpty(data) || data.Contains("EMPTY") || data.Contains("ERROR"))
                return items;

            foreach (var itemStr in data.Split('|'))
            {
                var parts = itemStr.Split(':');
                if (parts.Length == 3)
                    items.Add(new InventoryItem(int.Parse(parts[0]), parts[1], int.Parse(parts[2])));
                else if (parts.Length == 4)
                    items.Add(new InventoryItem(int.Parse(parts[0]), parts[1] + ":" + parts[2], int.Parse(parts[3])));
            }
            return items;
        }

        /// <summary>Get Player Details</summary>
        public PlayerDetails GetPlayerDetails(string target = "")
        {
            Send(string.Format("player.getDetails({0})", target));
            var data = Receive();

            if (string.IsNullOrEmpty(data) || data.Contains("Error"))
                return null;

            var parts = data.Split(',');
            // Name,ID,Mode,HP,MaxHP,Food,HeldItem,Count
            if (parts.Length < 8) return null;

            return new PlayerDetails(
                parts[0],
                int.Parse(parts[1]),
                parts[2],
                float.Parse(parts[3]),
                float.Parse(parts[4]),
                int.Parse(parts[5]),
                parts[6],
                int.Parse(parts[7])
            );
        }

        // ==================== Entity Info ====================

        /// <summary>Get Entities Nearby</summary>
        public List<EntityInfo> GetEntities(double x, double y, double z, double radius = 10, string dimension = null)
        {
            if (dimension != null)
                Send(string.Format("world.getEntities({0},{1},{2},{3},{4})", x, y, z, radius, dimension));
            else
                Send(string.Format("world.getEntities({0},{1},{2},{3})", x, y, z, radius));

            var data = Receive();
            var entities = new List<EntityInfo>();

            if (string.IsNullOrEmpty(data)) return entities;

            foreach (var item in data.Split('|'))
            {
                var parts = item.Split(',');
                if (parts.Length >= 5)
                {
                    entities.Add(new EntityInfo(
                        int.Parse(parts[0]),
                        parts[1],
                        new Vec3(double.Parse(parts[2]), double.Parse(parts[3]), double.Parse(parts[4]))
                    ));
                }
            }
            return entities;
        }

        // ==================== Event ====================

        /// <summary>Get Hit Event</summary>
        public List<BlockHit> PollBlockHits()
        {
            Send("events.block.hits()");
            var data = Receive();
            var hits = new List<BlockHit>();

            if (string.IsNullOrEmpty(data)) return hits;

            foreach (var item in data.Split('|'))
            {
                var parts = item.Split(',');
                if (parts.Length >= 6)
                {
                    hits.Add(new BlockHit(
                        int.Parse(parts[0]),
                        int.Parse(parts[1]),
                        int.Parse(parts[2]),
                        int.Parse(parts[3]),
                        int.Parse(parts[4]),
                        int.Parse(parts[5])
                    ));
                }
            }
            return hits;
        }

        /// <summary>Get Chat Event</summary>
        public List<ChatPost> PollChatPosts()
        {
            Send("events.chat.posts()");
            var data = Receive();
            var posts = new List<ChatPost>();

            if (string.IsNullOrEmpty(data)) return posts;

            foreach (var item in data.Split('|'))
            {
                var idx = item.IndexOf(',');
                if (idx > 0)
                    posts.Add(new ChatPost(item.Substring(0, idx), item.Substring(idx + 1)));
            }
            return posts;
        }

        // ==================== Other ====================

        /// <summary>Set Sign</summary>
        public void SetSign(int x, int y, int z, string line1 = "", string line2 = "", string line3 = "", string line4 = "", string dimension = null)
        {
            var l1 = line1.Replace(",", "，");
            var l2 = line2.Replace(",", "，");
            var l3 = line3.Replace(",", "，");
            var l4 = line4.Replace(",", "，");

            var cmd = string.Format("world.setSign({0},{1},{2},{3},{4},{5},{6})", x, y, z, l1, l2, l3, l4);
            if (dimension != null) cmd += "," + dimension;
            Send(cmd);
        }

        /// <summary>Set Block NBT</summary>
        public void SetBlockNbt(int x, int y, int z, string nbtString, string dimension = null)
        {
            var cmd = string.Format("block.setNbt({0},{1},{2},{3})", x, y, z, nbtString);
            if (dimension != null) cmd += "," + dimension;
            Send(cmd);
        }

        // ==================== Custom Screen ====================

        /// <summary>Update Screen</summary>
        public void UpdateScreen(int screenId, string imageData)
        {
            Send(string.Format("screen.update(@a,{0},{1})", screenId, imageData));
        }

        /// <summary>Get Screen Locations</summary>
        public List<ScreenLocation> GetScreenLocations(int screenId)
        {
            Send(string.Format("screen.getPos({0})", screenId));
            var resp = Receive();
            var locations = new List<ScreenLocation>();

            if (string.IsNullOrEmpty(resp) || resp.Contains("ERROR")) return locations;

            foreach (var part in resp.Split('|'))
            {
                var c = part.Split(',');
                if (c.Length >= 4)
                    locations.Add(new ScreenLocation(double.Parse(c[0]), double.Parse(c[1]), double.Parse(c[2]), c[3]));
                else if (c.Length == 3)
                    locations.Add(new ScreenLocation(double.Parse(c[0]), double.Parse(c[1]), double.Parse(c[2])));
            }
            return locations;
        }

        /// <summary>Register Screen</summary>
        public void RegisterScreen(int screenId, double x, double y, double z, string dimension = "")
        {
            Send(string.Format("screen.register({0},{1},{2},{3},{4})", screenId, x, y, z, dimension));
        }

        /// <summary>Create Screen Wall</summary>
        public void CreateScreenWall(int startX, int startY, int startZ, int width, int height, char axis = 'x', int screenId = 1, string dimension = "")
        {
            Console.WriteLine(string.Format("Building {0}x{1} screen (ID {2})...", width, height, screenId));
            var facing = axis == 'x' ? "south" : "east";

            for (int gy = 0; gy < height; gy++)
            {
                for (int gx = 0; gx < width; gx++)
                {
                    var y = startY + gy;
                    int x, z;

                    if (axis == 'x')
                    {
                        x = startX + gx;
                        z = startZ;
                    }
                    else
                    {
                        x = startX;
                        z = startZ + (width - 1 - gx);
                    }

                    SetBlock(x, y, z, string.Format("mcapibridge:screen[facing={0}]", facing));
                    var nbt = string.Format("{{ScreenId:{0},W:{1},H:{2},GX:{3},GY:{4}}}", screenId, width, height, gx, gy);
                    SetBlockNbt(x, y, z, nbt);
                }
            }

            double cx, cy, cz;
            if (axis == 'x')
            {
                cx = startX + width / 2.0;
                cy = startY + height / 2.0;
                cz = startZ + 1.0;
            }
            else
            {
                cx = startX + 1.0;
                cy = startY + height / 2.0;
                cz = startZ + width / 2.0;
            }

            RegisterScreen(screenId, cx, cy, cz, dimension);
            Console.WriteLine(string.Format("Screen registered at ({0:F1}, {1:F1}, {2:F1})", cx, cy, cz));
        }

        public void Dispose()
        {
            if (_reader != null) _reader.Dispose();
            if (_stream != null) _stream.Dispose();
            if (_client != null) _client.Close();
        }
    }

    // ==================== Helper Class ====================

    /// <summary>Player Info</summary>
    public class PlayerInfo
    {
        public string Name { get; private set; }
        public int Id { get; private set; }

        public PlayerInfo(string name, int id)
        {
            Name = name;
            Id = id;
        }
    }

    /// <summary>Inventory Item</summary>
    public class InventoryItem
    {
        public int Slot { get; private set; }
        public string Id { get; private set; }
        public int Count { get; private set; }

        public InventoryItem(int slot, string id, int count)
        {
            Slot = slot;
            Id = id;
            Count = count;
        }
    }

    /// <summary>Entity Info</summary>
    public class EntityInfo
    {
        public int Id { get; private set; }
        public string Type { get; private set; }
        public Vec3 Pos { get; private set; }

        public EntityInfo(int id, string type, Vec3 pos)
        {
            Id = id;
            Type = type;
            Pos = pos;
        }
    }
}