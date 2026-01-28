import socket
import math
import time

class Vec3:
    def __init__(self, x, y, z):
        self.x, self.y, self.z = x, y, z
    def __repr__(self): return f"Vec3({self.x:.2f}, {self.y:.2f}, {self.z:.2f})"
    def __sub__(self, o): return Vec3(self.x-o.x, self.y-o.y, self.z-o.z)
    def length(self): return math.sqrt(self.x**2 + self.y**2 + self.z**2)

class PlayerPos(Vec3):
    def __init__(self, x, y, z, yaw, pitch):
        super().__init__(x, y, z)
        self.yaw = yaw
        self.pitch = pitch

    def __repr__(self):
        return f"PlayerPos(x={self.x:.1f}, y={self.y:.1f}, z={self.z:.1f}, yaw={self.yaw:.1f}, pitch={self.pitch:.1f})"
    
    def forward(self, distance=1.0):
        yaw_rad = math.radians(self.yaw)
        pitch_rad = math.radians(self.pitch)
        
        vx = -math.sin(yaw_rad) * math.cos(pitch_rad)
        vy = -math.sin(pitch_rad)
        vz = math.cos(yaw_rad) * math.cos(pitch_rad)
        
        return Vec3(self.x + vx * distance, self.y + vy * distance, self.z + vz * distance)

class ChatPost:
    def __init__(self, name, message):
        self.name = name
        self.message = message
    
    def __repr__(self):
        return f"[{self.name}]: {self.message}"

class BlockHit:
    def __init__(self, x, y, z, face, entityId, action):
        self.pos = Vec3(x, y, z)
        self.face = face
        self.entityId = entityId
        self.action = action # 1=Left,2=Right
        self.type = "RIGHT_CLICK" if action == 2 else "LEFT_CLICK"

class Minecraft:
    def __init__(self, host="localhost", port=4711):
        self.host = host
        self.port = port
        self.socket = None
        self.file_reader = None
        self.connected = False
        self._connect()

    def _connect(self):
        try:
            if self.socket:
                try: self.socket.close()
                except: pass
            
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(5) 
            self.socket.connect((self.host, self.port))
            
            self.file_reader = self.socket.makefile('r', encoding='utf-8')
            self.connected = True
            print(f"[MCAPI] Connected ({self.host}:{self.port}).")
            return True
        except (ConnectionRefusedError, socket.timeout):
            print("[MCAPI] Lost connection.Retry...")
            self.connected = False
            return False

    def _send(self, cmd):
        try:
            if not self.connected:
                if not self._connect(): return

            self.socket.sendall((cmd + "\n").encode("utf-8"))
        except (BrokenPipeError, ConnectionResetError, socket.timeout, OSError):
            print("[MCAPI] Lost connection.Retry...")
            if self._connect():
                try:
                    self.socket.sendall((cmd + "\n").encode("utf-8"))
                except:
                    print("[MCAPI] Retry failed.")

    def _recv(self):
        if not self.connected: return ""
        try:
            line = self.file_reader.readline()
            if not line:
                print("[MCAPI] Retry.")
                self.connected = False
                for i in range(3):
                    time.sleep(1)
                    if self._connect():
                        return self._recv()
                return ""
            return line.strip()
        except (socket.timeout, OSError):
            return ""

    def postToChat(self, msg):
        self._send(f"chat.post({msg})")

    def runCommand(self, cmd):
        if cmd.startswith("/"): cmd = cmd[1:]
        self._send(f"server.runCommand({cmd})")

    def setBlock(self, x, y, z, block_id, dimension=None):
        if dimension:
            self._send(f"world.setBlock({int(x)},{int(y)},{int(z)},{block_id},{dimension})")
        else:
            self._send(f"world.setBlock({int(x)},{int(y)},{int(z)},{block_id})")

    def spawnEntity(self, x, y, z, entity_id, yaw=0.0, pitch=0.0, dimension=None):
        if dimension:
            self._send(f"world.spawnEntity({x},{y},{z},{entity_id},{yaw},{pitch},{dimension})")
        else:
            self._send(f"world.spawnEntity({x},{y},{z},{entity_id},{yaw},{pitch})")
            
        return int(self._recv())

    def spawnParticle(self, x, y, z, particle_id, count=10, dx=0.0, dy=0.0, dz=0.0, speed=0.0, dimension=None):
        cmd = f"{x},{y},{z},{particle_id},{count},{dx},{dy},{dz},{speed}"
        if dimension:
            cmd += f",{dimension}"
        self._send(f"world.spawnParticle({cmd})")

    def setEntityVelocity(self, entity_id, vx, vy, vz):
        self._send(f"entity.setVelocity({entity_id},{vx},{vy},{vz})")

    def getDirectionVector(self,target=""):
        pos = self.getPlayerPos(target=target)
        
        self._send("player.getPos()")
        data = self._recv().split(",")
        if len(data) < 5: return Vec3(0,0,1)
        
        yaw = float(data[3])
        pitch = float(data[4])
        
        yaw_rad = math.radians(yaw)
        pitch_rad = math.radians(pitch)
        
        vx = -math.sin(yaw_rad) * math.cos(pitch_rad)
        vy = -math.sin(pitch_rad)
        vz = math.cos(yaw_rad) * math.cos(pitch_rad)
        
        return Vec3(vx, vy, vz)

    def setEntityNoGravity(self, entity_id, enable=True):
        val = "true" if enable else "false"
        self._send(f"entity.setNoGravity({entity_id},{val})")

    def getPlayerPos(self, target=""):
        self._send(f"player.getPos({target})")
        data = self._recv()
        if not data: return PlayerPos(0,0,0,0,0)
        # x, y, z, yaw, pitch
        parts = data.split(",")
        if len(parts) >= 5:
            return PlayerPos(
                float(parts[0]), float(parts[1]), float(parts[2]),
                float(parts[3]), float(parts[4])
            )
        return PlayerPos(0,0,0,0,0)

    def getOnlinePlayers(self):
        self._send("world.getPlayers()")
        d = self._recv()
        if not d: return []
        players = []
        for item in d.split("|"):
            parts = item.split(",")
            if len(parts) == 2:
                players.append({"name": parts[0], "id": int(parts[1])})
        return players

    def getPlayerDetails(self, target=""):
        self._send(f"player.getDetails({target})")
        d = self._recv()
        if not d or "Error" in d: return None
        p = d.split(",")
        # Name,ID,Mode,HP,MaxHP,Food,HeldItem,Count
        return {
            "name": p[0],
            "id": int(p[1]),
            "mode": p[2],
            "health": float(p[3]),
            "max_health": float(p[4]),
            "food": int(p[5]),
            "held_item": p[6],
            "held_count": int(p[7])
        }

    def getPlayerEntityId(self, name):
        players = self.getOnlinePlayers()
        for p in players:
            if p['name'] == name:
                return p['id']
        return None

    def getPlayerName(self, entity_id):
        players = self.getOnlinePlayers()
        for p in players:
            if p['id'] == entity_id:
                return p['name']
        return None
    
    def getInventory(self, target=""):
        self._send(f"player.getInventory({target})")
        d = self._recv()
        
        if not d or "EMPTY" in d or "ERROR" in d: return []
        
        items = []
        for item_str in d.split("|"):
            parts = item_str.split(":")
            if len(parts) == 3:
                items.append({
                    "slot": int(parts[0]),
                    "id": parts[1],
                    "count": int(parts[2])
                })
            elif len(parts) == 4:
                items.append({
                    "slot": int(parts[0]),
                    "id": f"{parts[1]}:{parts[2]}",
                    "count": int(parts[3])
                })
        return items

    def setHealth(self, target, amount):
        self._send(f"player.setHealth({target},{amount})")

    def setFood(self, target, amount):
        self._send(f"player.setFood({target},{amount})")

    def give(self, target, item_id, count=1):
        self._send(f"player.give({target},{item_id},{count})")

    def clearInventory(self, target, item_id=""):
        self._send(f"player.clear({target},{item_id})")

    def giveEffect(self, target, effect_name, duration_sec=30, amplifier=1):
        self._send(f"player.effect({target},{effect_name},{duration_sec},{amplifier})")

    def teleport(self, x, y, z, target=""):
        if target:
            self._send(f"player.teleport({target},{x},{y},{z})")
        else:
            self._send(f"player.teleport({x},{y},{z})")

    def teleportEntity(self, entity_id, x, y, z):
        self._send(f"entity.teleport({entity_id},{x},{y},{z})")

    def pollBlockHits(self):
        self._send("events.block.hits()")
        d = self._recv()
        hits = []
        if not d: return hits
        for item in d.split("|"):
            p = item.split(",")
            if len(p) >= 6:
                hits.append(BlockHit(
                    int(p[0]), int(p[1]), int(p[2]), 
                    int(p[3]), int(p[4]), int(p[5])
                ))
        return hits
    
    def pollChatPosts(self):
        """
        [ChatPost(name='Steve', message='Hello'), ...]
        """
        self._send("events.chat.posts()")
        data = self._recv()
        posts = []
        if not data: return posts
        
        for item in data.split("|"):
            parts = item.split(",", 1)
            if len(parts) == 2:
                posts.append(ChatPost(parts[0], parts[1]))
        return posts

    def setFlying(self, target, allow_flight=True, is_flying=True):
        a = "true" if allow_flight else "false"
        f = "true" if is_flying else "false"
        self._send(f"player.setFlying({target},{a},{f})")

    def setFlySpeed(self, target, speed=0.05):
        # Default speed:0.05
        self._send(f"player.setSpeed({target},true,{speed})")

    def setWalkSpeed(self, target, speed=0.1):
        # Default speed:0.1
        self._send(f"player.setSpeed({target},false,{speed})")

    def setGodMode(self, target, enable=True):
        val = "true" if enable else "false"
        self._send(f"player.setGod({target},{val})")

    def getBlock(self, x, y, z, dimension=None):
        cmd = f"world.getBlock({int(x)},{int(y)},{int(z)})"
        if dimension: cmd = f"world.getBlock({int(x)},{int(y)},{int(z)},{dimension})"
        self._send(cmd)
        return self._recv()

    def getEntities(self, x, y, z, radius=10, dimension=None):
        """
        [{'id': 123, 'type': 'zombie', 'pos': Vec3}, ...]
        """
        cmd = f"world.getEntities({x},{y},{z},{radius})"
        if dimension: cmd = f"world.getEntities({x},{y},{z},{radius},{dimension})"
        self._send(cmd)
        data = self._recv()
        if not data: return []
        
        entities = []
        for item in data.split("|"):
            # ID,Type,X,Y,Z
            p = item.split(",")
            if len(p) >= 5:
                entities.append({
                    "id": int(p[0]),
                    "type": p[1],
                    "pos": Vec3(float(p[2]), float(p[3]), float(p[4]))
                })
        return entities

    def setSign(self, x, y, z, line1="", line2="", line3="", line4="", dimension=None):
        l1 = line1.replace(",", "，")
        l2 = line2.replace(",", "，")
        l3 = line3.replace(",", "，")
        l4 = line4.replace(",", "，")
        
        cmd = f"world.setSign({int(x)},{int(y)},{int(z)},{l1},{l2},{l3},{l4})"
        if dimension: cmd += f",{dimension}"
        self._send(cmd)
