# MCAPIBridge

**MCAPIBridge** is a powerful Fabric mod for **Minecraft 1.20.6** that establishes a real-time TCP socket connection between the game and external scripts (Python, etc.). It exposes a comprehensive API allowing programmatic control over the world, entities, and player states, acting as a programmable interface for Minecraft.

*Inspired by the classic **[Minecraft Pi Protocol (MCPI)](https://github.com/martinohanlon/mcpi)** and the **[RaspberryJuice](https://github.com/zhuowei/RaspberryJuice)** plugin.*

---

## ⚡ Core Capabilities

### 🔌 Socket Server
*   **Port 4711**: Opens a local TCP server listening for text-based commands.
*   **Protocol**: Robust line-based protocol compatible with any programming language.
*   **Multi-Client**: Supports multiple concurrent script connections via multithreading.

### 🌍 World & Dimension Control
*   **Block Manipulation**: Place or query any block (supports modded blocks) instantly.
*   **Dimension Support**: Commands can target specific dimensions (Overworld, Nether, The End) automatically.
*   **Sign Editing**: Modify text on sign blocks dynamically.
*   **Server Commands**: Execute any vanilla server command (e.g., `/time`, `/weather`) with OP privileges.

### 🧟 Entity & Physics Engine
*   **Advanced Spawning**: Summon entities with precise coordinates and **rotation (Yaw/Pitch)**.
*   **Physics Control**:
    *   **Velocity**: Apply force vectors to entities to launch them (creating projectiles).
    *   **Gravity**: Toggle gravity on/off for specific entities (e.g., creating laser-straight arrows).
*   **Entity Radar**: Scan and retrieve a list of all entities within a specified radius.
*   **Particle System**: Generate visual effects with custom count, speed, and diffusion volume.
*   **Forced Look**: Control where a player or entity is looking with lookAt, perfect for cinematics or horror mechanics.

### 👤 Player Management
*   **Stats Control**: Read and modify **Health** (can break vanilla limits), **Food Level**, and **Game Mode**.
*   **Abilities**: Toggle **Flight** (even in Survival mode), **God Mode** (Invulnerability), and adjust **Movement Speeds** (Walk/Fly).
*   **Inventory API**:
    *   **Read**: Retrieve full inventory contents (Main, Armor, Offhand).
    *   **Write**: Give items or Clear specific items/slots.
*   **Detailed Info**: Access real-time data including exact position, rotation, and held items.
*   **Teleportation**: Teleport players or entities across coordinates and dimensions.

### 🖱️ Input & Events System
*   **Dual-Sided Architecture**: Includes both Client and Server logic for precise input detection.
*   **Raycasting Interactions**:
    *   **Right Click**: Detects interaction up to **500 blocks** away (Server-side raycasting).
    *   **Left Click**: Detects interactions and attacks.
*   **Chat Listener**: Streams in-game chat messages to the external script in real-time.
*   **Macro Keys**: Registers **5 Custom Macro Keys** in the game's controls menu. Scripts can listen for these specific key presses to trigger custom logic.

### 📦 Deep Data Access (NBT)
*   **NBT Modification**: Directly write raw NBT data to **Entities** and **Block Entities**.
*   **Advanced Customization**: Create giants, freeze mobs (`NoAI`), make entities glow, or fill chests with custom loot using JSON-like NBT strings.

---

## 📦 Requirements

*   **Minecraft Version**: 1.20.6
*   **Mod Loader**: Fabric Loader
*   **Dependencies**: Fabric API 0.100.8

## Install
#### [Modrinth](https://modrinth.com/mod/mcapibridge)

## ⚠️ Security Warning

This mod opens **Port 4711** and grants **Full Administrative (OP) Permissions** to any program that connects to it.

*   **Do not** install this on a public server without a firewall.
*   It is designed for single-player, LAN, or private server use only.

## Bridges
#### [Python Libary](https://github.com/TaotianZhufang/MCAPIBridge/blob/main/Bridges/Python/README.MD)

#### [Python Examples](https://github.com/TaotianZhufang/MCAPIBridge/tree/main/Bridges/Example/Python)

*More languages will be supported future*

## Update
#### 2/1/2026 Alpha-0.2.2~~~~
*   **Deep Data Control**: Added **NBT modification** support for entities and blocks, allowing advanced customization like giant mobs or custom loot.
*   **Camera Control**: Implemented `lookAt` API to force players or entities to look at specific coordinates.

#### 1/31/2026 Alpha-0.2.1

**Bug Fixed**: Single hit or chat event can't be handled by above two scripts.

#### 1/31/2026 Alpha-0.2.0

*   **Client-Side Input**: Now detects Left/Right clicks even when aiming at the air (using client packets).
*   **Macro Keys**: Added 5 customizable keys in the game settings that scripts can listen to for triggering custom actions.

