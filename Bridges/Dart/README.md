# MCAPIBridge Dart Library

MCAPIBridge is a mod for Minecraft loaded with Fabric. This library offers ways to connect Minecraft with this mod in Dart/Flutter.

[![pub package](https://img.shields.io/pub/v/mcapibridge.svg)](https://pub.dev/packages/mcapibridge)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Features

- 🧱 **World Control**: Set/get blocks, fill areas, spawn entities and particles
- 🧑 **Player Management**: Teleport, inventory, effects, flying, god mode
- 📺 **Screen System**: Push pixel images to in-game screen walls
- 🔊 **Audio System**: Load WAV/PCM files, play 2D/3D positional audio
- ⚡ **IO System**: Read/write redstone signals programmatically
- 📡 **Event Polling**: Block hit events, chat message events
- 🎯 **Entity Control**: Velocity, gravity, NBT modification

## Getting Started

### Prerequisites

1. Minecraft with the **MCAPIBridge Fabric mod** installed
2. The mod's TCP server running (default port: 4711)

### Installation

Add this to your `pubspec.yaml`:

```yaml
dependencies:
  mcapibridge: ^1.0.3
```

Then run:

```bash
dart pub get
```

## Usage

### Quick Start

```dart
import 'package:mcapibridge/mcapibridge.dart';

void main() async {
  // Connect to Minecraft server
  // Default host is 'localhost' and port is 4711
  final mc = await Minecraft.connect(
    host: 'localhost',
    port: 4711,
  );

  // Send a chat message
  mc.postToChat('§aHello, Minecraft! Dart is here.');

  // Get player position
  final pos = await mc.getPlayerPos();
  print('Player at: $pos');

  // Place a diamond block
  mc.setBlock(pos.x.toInt(), pos.y.toInt() - 1, pos.z.toInt(), 'diamond_block');

  // Disconnect when done
  await mc.disconnect();
}
```

---

## API Reference

### Class `Minecraft`

Main class for communicating with the Minecraft server.

#### Connection

##### `Minecraft.connect({host, port, timeout})`

Creates a connection to the Minecraft server.

```dart
final mc = await Minecraft.connect(
  host: 'localhost',     // Server IP
  port: 4711,            // Server port
  timeout: Duration(seconds: 5),
);
```

##### `mc.reconnect({timeout})`

Reconnects to the server.

##### `mc.disconnect()`

Closes the connection.

##### `mc.connected`

Boolean property indicating connection status.

---

#### Basic Methods

##### `mc.postToChat(msg)`

Send a message to the chat screen.

- **msg**: String message. Supports `§` color codes.

```dart
mc.postToChat('§aGreen text! §cRed text!');
```

##### `mc.runCommand(cmd)`

Run commands as Server.

- **cmd**: String command without '/'.

```dart
mc.runCommand('time set day');
mc.runCommand('weather clear');
```

---

#### World Methods

##### `mc.setBlock(x, y, z, blockId, [dimension])`

Set a block at the specified position.

- **x, y, z**: Int positions.
- **blockId**: String ID. Can be written without `minecraft:` prefix.
- **dimension**: Optional String dimension ID.

```dart
mc.setBlock(0, 80, 0, 'stone');
mc.setBlock(0, 81, 0, 'minecraft:diamond_block');
```

##### `mc.getBlock(x, y, z, [dimension])`

Gets the block ID at the specified coordinates.

- **Returns**: `Future<String>` block ID (e.g., `"minecraft:grass_block"`).

```dart
final block = await mc.getBlock(0, 80, 0);
print(block); // minecraft:stone
```

##### `mc.spawnEntity(x, y, z, entityId, {yaw, pitch, dimension})`

Spawn an entity at the specified position.

- **entityId**: String ID (e.g., `"zombie"`, `"pig"`, `"lightning_bolt"`).
- **yaw**: Optional double horizontal rotation.
- **pitch**: Optional double vertical rotation.
- **Returns**: `Future<int>` entity ID.

```dart
final zombieId = await mc.spawnEntity(0, 80, 0, 'zombie');
```

##### `mc.spawnParticle(x, y, z, particleId, {count, dx, dy, dz, speed, dimension})`

Spawn particles at the specified position.

- **particleId**: String ID (e.g., `"flame"`, `"heart"`).
- **count**: Int particle count (default 10).
- **dx, dy, dz**: Double diffusion ranges.
- **speed**: Double particle speed.

```dart
mc.spawnParticle(0, 80, 0, 'flame', count: 50, speed: 0.1);
```

##### `mc.getEntities(x, y, z, radius, [dimension])`

Get entities within a radius.

- **Returns**: `Future<List<Map>>` with `id`, `type`, and `pos` (Vec3).

```dart
final entities = await mc.getEntities(0, 80, 0, 10);
for (final e in entities) {
  print('${e['type']} at ${e['pos']}');
}
```

##### `mc.setSign(x, y, z, {l1, l2, l3, l4, dimension})`

Set text on a sign block. **The block must already be a sign.**

```dart
mc.setSign(0, 80, 0, l1: 'Line 1', l2: 'Line 2', l3: 'Line 3', l4: 'Line 4');
```

##### `mc.setBlockNbt(x, y, z, nbtString, [dimension])`

Modify NBT data of a block entity.

```dart
mc.setBlockNbt(0, 80, 0, '{Lock:"secret"}');
```

---

#### Entity Methods

##### `mc.setEntityVelocity(entityId, vx, vy, vz)`

Set velocity of an entity.

```dart
mc.setEntityVelocity(zombieId, 0, 1.0, 0); // Launch upward
```

##### `mc.setEntityNoGravity(entityId, {enable})`

Enable or disable gravity for an entity.

```dart
mc.setEntityNoGravity(zombieId, enable: true);
```

##### `mc.teleportEntity(entityId, x, y, z)`

Teleport an entity to coordinates.

```dart
mc.teleportEntity(zombieId, 100, 80, 100);
```

##### `mc.setEntityNbt(entityId, nbtString)`

Modify NBT data of an entity.

```dart
mc.setEntityNbt(zombieId, '{NoAI:1b,Glowing:1b}');
```

---

#### Player Methods

##### `mc.getPlayerPos([target])`

Get player position and rotation.

- **target**: Optional String player name.
- **Returns**: `Future<PlayerPos>` with x, y, z, yaw, pitch.

```dart
final pos = await mc.getPlayerPos();
print('Position: ${pos.x}, ${pos.y}, ${pos.z}');
print('Looking: yaw=${pos.yaw}, pitch=${pos.pitch}');
```

##### `mc.getDirectionVector([target])`

Get normalized direction vector based on player rotation.

- **Returns**: `Future<Vec3>` direction vector.

```dart
final dir = await mc.getDirectionVector();
// Useful for shooting projectiles
```

##### `mc.getOnlinePlayers()`

Get all online players.

- **Returns**: `Future<List<Map>>` with `name` and `id`.

```dart
final players = await mc.getOnlinePlayers();
for (final p in players) {
  print('${p['name']} (ID: ${p['id']})');
}
```

##### `mc.getPlayerDetails([target])`

Get detailed player information.

- **Returns**: `Future<Map?>` with:
  - `name`: String
  - `id`: int
  - `mode`: String gamemode
  - `health`: double
  - `max_health`: double
  - `food`: int
  - `held_item`: String
  - `held_count`: int

```dart
final details = await mc.getPlayerDetails('Steve');
print('Health: ${details?['health']}/${details?['max_health']}');
```

##### `mc.getPlayerEntityId(name)`

Get player's entity ID by name.

```dart
final id = await mc.getPlayerEntityId('Steve');
```

##### `mc.getPlayerName(entityId)`

Get player's name by entity ID.

```dart
final name = await mc.getPlayerName(123);
```

##### `mc.getInventory([target])`

Get player's inventory.

- **Returns**: `Future<List<Map>>` with `slot`, `id`, `count`.

```dart
final inv = await mc.getInventory();
for (final item in inv) {
  print('Slot ${item['slot']}: ${item['id']} x${item['count']}');
}
```

---

#### Player State Methods

##### `mc.setHealth(target, amount)`

Set player's health.

```dart
mc.setHealth('@s', 20.0);
```

##### `mc.setFood(target, amount)`

Set player's food level (0-20).

```dart
mc.setFood('@s', 20);
```

##### `mc.giveEffect(target, effectName, {durationSec, amplifier})`

Apply an effect to the player.

```dart
mc.giveEffect('@s', 'speed', durationSec: 60, amplifier: 2);
mc.giveEffect('@s', 'night_vision', durationSec: 300);
```

##### `mc.setFlying(target, {allowFlight, isFlying})`

Enable flight in survival mode.

```dart
mc.setFlying('@s', allowFlight: true, isFlying: true);
```

##### `mc.setFlySpeed(target, {speed})`

Set flight speed (default 0.05).

```dart
mc.setFlySpeed('@s', speed: 0.1);
```

##### `mc.setWalkSpeed(target, {speed})`

Set walk speed (default 0.1).

```dart
mc.setWalkSpeed('@s', speed: 0.2);
```

##### `mc.setGodMode(target, {enable})`

Enable invulnerability.

```dart
mc.setGodMode('@s', enable: true);
```

##### `mc.lookAt(target, x, y, z)`

Force player to look at coordinates.

```dart
mc.lookAt('@s', 0, 100, 0); // Look up at sky
```

---

#### Inventory Methods

##### `mc.give(target, itemId, {count})`

Give item to player.

- **target**: String player name or selector.
- **itemId**: String item ID (e.g., `"diamond"`, `"minecraft:diamond_sword"`).
- **count**: Int count (default 1).

```dart
mc.give('@s', 'diamond', count: 64);
mc.give('Steve', 'diamond_sword');
mc.give('@a', 'golden_apple', count: 16);
```

##### `mc.clearInventory(target, [itemId])`

Clear player's inventory.

```dart
mc.clearInventory('@s');           // Clear all
mc.clearInventory('@s', 'dirt');   // Clear only dirt
```

---

#### Teleport Methods

##### `mc.teleport(x, y, z, [target])`

Teleport player to coordinates.

```dart
mc.teleport(0, 80, 0);
mc.teleport(100, 80, 100, 'Steve');
```

---

#### Event Methods

##### `mc.pollBlockHits()`

Get block click events.

- **Returns**: `Future<List<BlockHit>>`

```dart
final hits = await mc.pollBlockHits();
for (final hit in hits) {
  print('Block at ${hit.pos} was ${hit.type} by player ${hit.entityId}');
  // hit.type: 'LEFT_CLICK', 'RIGHT_CLICK', or 'KEY_MACRO_1' to 'KEY_MACRO_5'
}
```

##### `mc.pollChatPosts()`

Get chat message events.

- **Returns**: `Future<List<ChatPost>>`

```dart
final chats = await mc.pollChatPosts();
for (final chat in chats) {
  print('[${chat.name}]: ${chat.message}');
}
```

---

### Screen Methods

##### `mc.updateScreen(screenId, base64Data, [target])`

Update screen block content.

- **screenId**: Int screen ID.
- **base64Data**: String Base64 encoded image (JPG/PNG).

```dart
mc.updateScreen(1, base64ImageData);
```

##### `mc.getScreenLocations(screenId)`

Get coordinates of all screen blocks with the specified ID.

- **Returns**: `Future<List<ScreenLocation>>`

```dart
final locations = await mc.getScreenLocations(1);
for (final loc in locations) {
  print('Screen at ${loc.x}, ${loc.y}, ${loc.z} in ${loc.dimension}');
}
```

##### `mc.registerScreen(screenId, x, y, z, [dimension])`

Manually register a screen location.

```dart
mc.registerScreen(1, 0.5, 80.5, 0.5);
```

##### `mc.createScreenWall(startX, startY, startZ, width, height, {axis, screenId, dimension})`

Build and register a screen wall automatically.

- **axis**: `'x'` (East-West) or `'z'` (North-South).

```dart
mc.createScreenWall(0, 80, 0, 4, 3, axis: 'x', screenId: 1);
```

---

### Class `AudioManager`

Access via `mc.audio`.

#### Loading Audio

##### `mc.audio.loadWav(target, audioId, filepath)`

Load a WAV file. Automatically converts stereo to mono.

```dart
await mc.audio.loadWav('@a', 'bgm', '/path/to/music.wav');
```

##### `mc.audio.loadRaw(target, audioId, pcmData, {sampleRate})`

Load raw PCM data (16-bit signed, mono).

```dart
await mc.audio.loadRaw('@a', 'tone', pcmBytes, sampleRate: 44100);
```

##### `mc.audio.generateTone(target, audioId, {frequency, duration, sampleRate, volume})`

Generate a sine wave tone.

```dart
await mc.audio.generateTone('@a', 'beep', frequency: 440, duration: 1.0);
```

#### Playback Control

##### `mc.audio.play(target, audioId, {volume, loop})`

Play audio in 2D (no spatial positioning).

```dart
mc.audio.play('@a', 'bgm', volume: 0.8, loop: true);
```

##### `mc.audio.play3d(target, audioId, x, y, z, {volume, rolloff, loop, dimension, offset})`

Play audio with 3D spatial positioning.

```dart
mc.audio.play3d('@a', 'explosion', 100, 80, 100, volume: 1.0, rolloff: 1.0);
```

##### `mc.audio.pause(target, audioId)`

Pause audio playback.

```dart
mc.audio.pause('@a', 'bgm');
```

##### `mc.audio.stop(target, audioId)`

Stop audio playback.

```dart
mc.audio.stop('@a', 'bgm');
```

##### `mc.audio.unload(target, audioId)`

Unload audio from memory.

```dart
mc.audio.unload('@a', 'bgm');
```

##### `mc.audio.setVolume(target, audioId, volume)`

Change volume of playing audio.

```dart
mc.audio.setVolume('@a', 'bgm', 0.5);
```

##### `mc.audio.setPosition(target, audioId, x, y, z)`

Update 3D position of playing audio.

```dart
mc.audio.setPosition('@a', 'moving_sound', 50, 80, 50);
```

##### `mc.audio.clone(target, sourceId, newId)`

Clone an audio instance for simultaneous playback.

```dart
mc.audio.clone('@a', 'gunshot', 'gunshot_2');
```

##### `mc.audio.reset()`

Reset all audio for all players.

```dart
mc.audio.reset();
```

##### `mc.audio.playAt(audioId, x, y, z, {radius, volume})`

Play audio at a world position. All players within radius will hear it.

- **audioId**: String audio ID.
- **x, y, z**: Double world coordinates.
- **radius**: Double hearing radius (default 32).
- **volume**: Double volume 0.0-1.0 (default 1.0).

```dart
mc.audio.playAt('explosion', 100, 80, 100);
mc.audio.playAt('ambient', 0, 64, 0, radius: 50, volume: 0.5);
```

##### `mc.audio.playOnScreen(audioId, screenId, {volume, loop})`

Play audio associated with a screen.

```dart
mc.audio.playOnScreen('ui_sound', 1, volume: 1.0);
```

##### `mc.audio.syncProgress(audioId, progress)`

Synchronize audio playback progress.

```dart
mc.audio.syncProgress('bgm', 30.5); // Jump to 30.5 seconds
```

---

### Class `IOManager`

Access via `mc.io`. Provides redstone signal control.

##### `mc.io.write(channelId, value)`

Send signal to an IO block (Input Mode).

- **value**: `int` (0-15) or `bool` (true=15, false=0).

```dart
mc.io.write(1, 15);      // Full power
mc.io.write(1, true);    // Same as 15
mc.io.write(1, false);   // Power off
```

##### `mc.io.read(channelId)`

Read signal strength from an IO block (Output Mode).

- **Returns**: `F'uture<int>` (0-15).

```dart
final power = await mc.io.read(1);
print('Signal strength: $power');
```

##### `mc.io.isHigh(channelId, {threshold})`

Check if signal is high (> threshold).

```dart
if (await mc.io.isHigh(1, threshold: 7)) {
  print('Signal is high!');
}
```

##### `mc.io.isLow(channelId, {threshold})`

Check if signal is low (<= threshold).

```dart
if (await mc.io.isLow(1)) {
  print('Signal is low');
}
```

##### `mc.io.config(x, y, z, channelId, mode, [dimension])`

Configure an IO block.

- **mode**: `bool`, `'in'`/`'input'`, or `'out'`/`'output'`.

```dart
mc.io.config(0, 80, 0, 1, 'output');  // MC redstone → Dart
mc.io.config(0, 80, 1, 2, 'input');   // Dart → MC redstone
```

---

### Data Classes

#### `Vec3`

Represents a 3D vector/coordinate.

```dart
final v = Vec3(1, 2, 3);
print(v.x);           // 1.0
print(v.length());    // 3.74...
print(v.normalized());

final a = Vec3(1, 0, 0);
final b = Vec3(0, 1, 0);
print(a + b);         // Vec3(1, 1, 0)
print(a.distanceTo(b));
print(a.dot(b));
```

#### `PlayerPos`

Extends `Vec3` with rotation. Returned by `getPlayerPos()`.

```dart
final pos = await mc.getPlayerPos();
print(pos.x);         // X coordinate
print(pos.yaw);       // Horizontal rotation
print(pos.pitch);     // Vertical rotation
print(pos.direction); // Normalized direction vector
print(pos.forward(10)); // Position 10 blocks ahead
```

#### `BlockHit`

Represents a block click event.

```dart
final hits = await mc.pollBlockHits();
for (final hit in hits) {
  print(hit.pos);       // Vec3 position
  print(hit.face);      // Int face index
  print(hit.entityId);  // Int player entity ID
  print(hit.type);      // 'LEFT_CLICK', 'RIGHT_CLICK', 'KEY_MACRO_1'...'KEY_MACRO_5'
  print(hit.action);    // Int raw action code
}
```

#### `ChatPost`

Represents a chat message event.

```dart
final chats = await mc.pollChatPosts();
for (final chat in chats) {
  print(chat.name);     // Player name
  print(chat.message);  // Message content
}
```

#### `ScreenLocation`

Represents a screen location with dimension.

```dart
final locations = await mc.getScreenLocations(1);
for (final loc in locations) {
  print('${loc.x}, ${loc.y}, ${loc.z}');
  print(loc.dimension); // e.g., 'minecraft:overworld'
}
```

#### `MCConnectionException`

Thrown when connection fails.

```dart
try {
  final mc = await Minecraft.connect(host: 'localhost');
} on MCConnectionException catch (e) {
  print('Failed to connect: ${e.message}');
}
```

---

## Examples

### Event Loop

```dart
import 'package:mcapibridge/mcapibridge.dart';

void main() async {
  final mc = await Minecraft.connect();

  mc.postToChat('§aBot started! Right-click blocks to interact.');

  while (mc.connected) {
    // Handle block clicks
    final hits = await mc.pollBlockHits();
    for (final hit in hits) {
      if (hit.type == 'RIGHT_CLICK') {
        mc.setBlock(
          hit.pos.x.toInt(),
          hit.pos.y.toInt() + 1,
          hit.pos.z.toInt(),
          'torch',
        );
      }
    }

    // Handle chat commands
    final chats = await mc.pollChatPosts();
    for (final chat in chats) {
      if (chat.message == '!heal') {
        mc.setHealth(chat.name, 20);
        mc.postToChat('§a${chat.name} has been healed!');
      }
    }

    await mc.delay(100);
  }
}
```

### Building Structures

```dart
// Build a glass dome
void buildDome(Minecraft mc, int cx, int cy, int cz, int radius) {
  for (int x = -radius; x <= radius; x++) {
    for (int y = 0; y <= radius; y++) {
      for (int z = -radius; z <= radius; z++) {
        final dist = (x * x + y * y + z * z);
        if (dist <= radius * radius && dist >= (radius - 1) * (radius - 1)) {
          mc.setBlock(cx + x, cy + y, cz + z, 'glass');
        }
      }
    }
  }
}
```

### Redstone Control

```dart
// Toggle a light every second
void main() async {
  final mc = await Minecraft.connect();
  bool state = false;

  while (mc.connected) {
    state = !state;
    await mc.io.write(1, state);
    await mc.delay(1000);
  }
}
```

---

## Additional Information

- **Repository**: [GitHub](https://github.com/TaotianZhufang/MCAPIBridge/tree/main/Bridges/Dart)
- **Mod Repository**: [MCAPIBridge Mod](https://github.com/TaotianZhufang/MCAPIBridge)

### Contributing

Contributions are welcome! Please open an issue or submit a pull request.

### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
```