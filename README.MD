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

### 🔊 Custom Audio System
*   **Tone Generation**: Generate sine wave tones with custom frequency, duration, and sample rate.
*   **WAV File Playback**: Load and play custom WAV audio files (8/16-bit PCM, Mono/Stereo).
*   **3D Spatial Audio**: Play sounds at specific world coordinates with realistic **distance attenuation**.
*   **Dynamic Controls**: Real-time **volume adjustment**, **loop playback**, **pause/stop**, and **position updates** for moving sound sources.
*   **Multi-Source Mixing**: Play multiple audio sources simultaneously (e.g., chords, ambient layers).
*   **Audio Cloning**: Efficiently clone loaded audio data for low-latency multi-speaker setups.

### 🖥️ Custom Screen System
*   **Dynamic Web Screen**: A new custom block `mcapibridge:screen` that can display real-time images or video streams controlled by scripts.
*   **Seamless Tiling**: Automatically merges adjacent screen blocks into a single large display, perfect for video walls or billboards.
*   **High Performance**: Uses client-side texture rendering and JPG/PNG streaming for smooth 30 FPS playback.Recommend JPG streaming.
*   **Interactive Configuration**: Right-click screen blocks with to easily configure IDs and layout without commands.
*   **Crafting Recipe**:
    ```
    G G G   (Glass Pane)
    R D R   (Redstone, Glowstone Dust, Redstone)
    I I I   (Iron Ingot)
    ```

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
#### [Python Library](https://github.com/TaotianZhufang/MCAPIBridge/blob/main/Bridges/Python/README.MD)

#### [Python Examples](https://github.com/TaotianZhufang/MCAPIBridge/tree/main/Bridges/Example/Python)

*More languages will be supported in the future.*

---

## 📋 Update Log

#### 2/6/2026 Alpha-0.4.0
*   **Custom Screen System**: Introduced `mcapibridge:screen` block for displaying real-time images and video streams.
    *   **Video Wall**: Supports seamless multi-block screen assembly with automatic layout detection.
    *   **High-Res Streaming**: Capable of streaming 1080p/720p content at 30 FPS via JPG/PNG compression.
    *   **Easy Config**: Right-click to open a GUI for setting Screen IDs.
*   **Audio Optimization**: Added `audio.clone` command to efficiently replicate audio sources for multi-speaker setups (e.g., surround sound cinemas) without re-uploading data.
*   **Bugs fixed**:
    *   Fix audio on play3d witch can't set volume to 0 automatically.
    *   Fix socket can still keep connected with bridges sometimes.

#### 2/4/2026 Alpha-0.3.1
*   **Audio Settings Integration**: Added native "MCAPIBridge Audio" volume slider in Minecraft's Sound Options menu.

#### 2/4/2026 Alpha-0.3.0
*   **Custom Audio System**: Added full audio playback capabilities via OpenAL.
    *   **Tone Generation**: Create sine wave tones with configurable frequency and duration.
    *   **WAV Support**: Load and play custom `.wav` files (auto-converts stereo to mono).
    *   **3D Spatial Audio**: Play audio at world coordinates with realistic distance attenuation and rolloff control.
    *   **Dynamic Position**: Update sound source positions in real-time for surround effects.
    *   **Playback Controls**: Volume adjustment, loop toggle, pause, stop, and unload functions.
    *   **Multi-Source**: Play multiple audio sources simultaneously for chords and layered audio.

#### 2/1/2026 Alpha-0.2.2
*   **Deep Data Control**: Added **NBT modification** support for entities and blocks, allowing advanced customization like giant mobs or custom loot.
*   **Camera Control**: Implemented `lookAt` API to force players or entities to look at specific coordinates.

#### 1/31/2026 Alpha-0.2.1
*   **Bug Fixed**: Single hit or chat event can now be handled by multiple connected scripts.

#### 1/31/2026 Alpha-0.2.0
*   **Client-Side Input**: Now detects Left/Right clicks even when aiming at the air (using client packets).
*   **Macro Keys**: Added 5 customizable keys in the game settings that scripts can listen to for triggering custom actions.

---

## 🗺️ Roadmap

*   **More Language Bridges**: C#, C/C++, Arduino for ESP32, Micropython for ESP32 libraries.
