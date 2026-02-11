# MCAPIBridge C# Library

MCAPIBridge is a mod for Minecraft loaded with Fabric. This library offers ways to connect Minecraft with this mod in C#.

## QuickStart

### Install
Ensure your mod loaded.

**Please ensure you are using the latest library. Now version is 0.1.3.**

Use NuGet to install this.
```
dotnet add package MCAPIBridge
```

Or in Visual Studio: **Tools** → **NuGet Package Manager** → **Manage NuGet Packages** → Search `MCAPIBridge`

This is a simple example to connect.
```csharp
using MCAPIBridge;

// Default ip is localhost and port is 4711
// You can use new MinecraftClient("YOURIP", YOURPORT) to change
using (var mc = new MinecraftClient())
{
    // Show a message
    mc.PostToChat("§aHello, Minecraft! C# is here.");
}
```

---

## Classes

### class `MinecraftClient`

#### Connection

```csharp
// Connect to default localhost:4711
var mc = new MinecraftClient();

// Connect to custom host and port
var mc = new MinecraftClient("192.168.1.100", 4711);

// Don't forget to dispose when done
mc.Dispose();

// Or use 'using' statement (recommended)
using (var mc = new MinecraftClient())
{
    // Your code here
}
```

---

#### Basic methods

##### `MinecraftClient.PostToChat(msg)`
Send a message to chat screen.
*   **msg**: String message. Support `§`. You can check it on Minecraft wiki.

##### `MinecraftClient.RunCommand(cmd)`
Run commands as Server.
*   **cmd**: String command without '/'. Leading slash is automatically removed.

Ex: `mc.RunCommand("time set day")`

---

#### World methods

##### `MinecraftClient.SetBlock(x, y, z, blockId, dimension)`
Set a block at the point given.
*   **x, y, z**: Int positions.
*   **blockId**: String ID. If it is Minecraft vanilla, the ID can be written without `minecraft:`. Ex: `"stone"`, `"diamond_block"`. Support block states like `"oak_stairs[facing=north]"`. Support other namespaces.
*   **dimension**: Optional String target dimension/player name.

##### `MinecraftClient.GetBlock(x, y, z, dimension)`
Gets the block ID at the specified coordinates.
*   **x, y, z**: Int positions.
*   **dimension**: Optional String target dimension/player name.
*   **return**: String block ID. Ex. `"minecraft:grass_block"`.

##### `MinecraftClient.SpawnEntity(x, y, z, entityId, yaw, pitch, dimension)`
Spawn an entity at the point given.
*   **x, y, z**: Double positions.
*   **entityId**: String ID. If it is Minecraft vanilla, the ID can be written without `minecraft:`. Ex: `"zombie"`, `"pig"`, `"lightning_bolt"`. Support other namespaces.
*   **yaw**: Optional float degree. Horizontal degree. Default 0.
*   **pitch**: Optional float degree. Vertical degree. Default 0.
*   **dimension**: Optional String target dimension/player name.
*   **return**: Int entity ID.

##### `MinecraftClient.SpawnParticle(x, y, z, particleId, count, dx, dy, dz, speed, dimension)`
Spawn particle at the point given.
*   **x, y, z**: Double positions.
*   **particleId**: String ID. If it is Minecraft vanilla, the ID can be written without `minecraft:`. Ex: `"flame"`, `"heart"`. Support other namespaces.
*   **count**: Int count. Default 10.
*   **dx, dy, dz**: Optional double diffusion ranges. Default 0. (when count=0, it represents the direction vector)
*   **speed**: Optional double speed. Default 0.
*   **dimension**: Optional String target dimension/player name.

##### `MinecraftClient.SetSign(x, y, z, line1, line2, line3, line4, dimension)`
Sets the text on a sign block.
*   **x, y, z**: Int positions.
*   **line1-4**: String text for each line. Default empty.
*   **dimension**: Optional String target dimension/player name.

**The block at the position must already be a sign.**

##### `MinecraftClient.SetBlockNbt(x, y, z, nbtString, dimension)`
Modifies the NBT data of a block entity (Tile Entity).
*   **x, y, z**: Int coordinates.
*   **nbtString**: Valid SNBT string.
*   **dimension**: Optional String target dimension/player name.

##### `MinecraftClient.GetEntities(x, y, z, radius, dimension)`
Gets entities near the specified coordinates.
*   **x, y, z**: Double positions.
*   **radius**: Double search radius. Default 10.
*   **dimension**: Optional String target dimension/player name.
*   **return**: List of `EntityInfo` objects with `Id`, `Type`, and `Pos` properties.

---

#### Entity methods

##### `MinecraftClient.TeleportEntity(entityId, x, y, z)`
Teleport an entity.
*   **entityId**: Int entity ID.
*   **x, y, z**: Double target coordinates.

##### `MinecraftClient.SetEntityVelocity(entityId, vx, vy, vz)`
Sets the velocity of an entity.
*   **entityId**: Int ID.
*   **vx, vy, vz**: Double velocity components.

##### `MinecraftClient.SetEntityNoGravity(entityId, enable)`
Enables or disables gravity for an entity.
*   **entityId**: Int ID.
*   **enable**: Bool. Default true.

##### `MinecraftClient.SetEntityNbt(entityId, nbtString)`
Modifies the NBT data of an entity directly using JSON format.
*   **entityId**: Int Entity ID.
*   **nbtString**: Valid SNBT string (e.g., `"{NoAI:1b, Glowing:1b}"`).
*   *Note*: Useful for setting attributes like Scale in 1.20.6 (`{Attributes:[{Name:"generic.scale",Base:2.0d}]}`).

##### `MinecraftClient.LookAt(target, x, y, z)`
Forces a player or entity to look at a specific coordinate.
*   **target**: String player name or Entity ID.
*   **x, y, z**: Double coordinates to look at.
*   *Example*: `mc.LookAt("Steve", 0, 100, 0)` forces Steve to look up.

---

#### Player Info methods

##### `MinecraftClient.GetOnlinePlayers()`
Get players online.
*   **return**: List of `PlayerInfo` objects with `Name` and `Id` properties.

##### `MinecraftClient.GetPlayerPos(target)`
Get player's position and rotation.
*   **target**: Optional String player name. Default empty (first player).
*   **return**: `PlayerPos` object with `X`, `Y`, `Z`, `Yaw`, `Pitch` properties.

##### `MinecraftClient.GetPlayerEntityId(name)`
Get player's entity ID by name.
*   **name**: String player name.
*   **return**: Nullable Int ID. Returns null if not found.

##### `MinecraftClient.GetPlayerName(entityId)`
Get player's name by entity ID.
*   **entityId**: Int ID.
*   **return**: String player name. Returns null if not found.

##### `MinecraftClient.GetPlayerDetails(target)`
Get player's details.
*   **target**: Optional String player name.
*   **return**: `PlayerDetails` object including:
    *   `Name`: String player name
    *   `Id`: Int ID
    *   `Mode`: String gamemode
    *   `Health`: Float health
    *   `MaxHealth`: Float max health
    *   `Food`: Int food
    *   `HeldItem`: String item held
    *   `HeldCount`: Int item held count

##### `MinecraftClient.GetDirectionVector(target)`
Calculates the direction vector based on a player's rotation. Useful for shooting projectiles.
*   **target**: Optional String player name.
*   **return**: `Vec3` normalized direction vector.

---

#### Player State methods

##### `MinecraftClient.Teleport(x, y, z, target)`
Teleport player.
*   **x, y, z**: Double target coordinates.
*   **target**: Optional String player name.

##### `MinecraftClient.SetHealth(target, amount)`
Set player's health.
*   **target**: String player name.
*   **amount**: Float health.

##### `MinecraftClient.SetFood(target, amount)`
Set player's food.
*   **target**: String player name.
*   **amount**: Int food (0-20).

##### `MinecraftClient.GiveEffect(target, effectName, durationSec, amplifier)`
Apply effect to player.
*   **target**: String player name.
*   **effectName**: String effect ID. Ex: `"speed"`, `"night_vision"`.
*   **durationSec**: Int seconds. Default 30.
*   **amplifier**: Int amplifier. Default 1.

##### `MinecraftClient.SetFlying(target, allowFlight, isFlying)`
Enable player to fly in survival mode.
*   **target**: String player name.
*   **allowFlight**: Bool allow flight. Default true.
*   **isFlying**: Bool currently flying. Default true.

##### `MinecraftClient.SetFlySpeed(target, speed)`
Set flight speed.
*   **target**: String player name.
*   **speed**: Float speed. Default 0.05f.

##### `MinecraftClient.SetWalkSpeed(target, speed)`
Set walk speed.
*   **target**: String player name.
*   **speed**: Float speed. Default 0.1f.

##### `MinecraftClient.SetGodMode(target, enable)`
Enable invulnerability.
*   **target**: String player name.
*   **enable**: Bool. Default true.

---

#### Inventory methods

##### `MinecraftClient.GetInventory(target)`
Get player's inventory.
*   **target**: Optional String player name.
*   **return**: List of `InventoryItem` objects with `Slot`, `Id`, `Count` properties.

##### `MinecraftClient.Give(target, itemId, count)`
Give player item.
*   **target**: String player name.
*   **itemId**: String item ID.
*   **count**: Int count. Default 1.

##### `MinecraftClient.ClearInventory(target, itemId)`
Clear inventory.
*   **target**: String player name.
*   **itemId**: Optional String item ID. Empty clears all.

---

#### Events methods

##### `MinecraftClient.PollBlockHits()`
Get click events.
*   **return**: List of `BlockHit` objects:
    *   **Pos**: `Vec3` position
    *   **Face**: Int click face
    *   **EntityId**: Int ID of clicking player
    *   **Action**: Int action type: 1=left click, 2=right click, 101-105=Keyboard pressed (Bind keys at Minecraft settings)
    *   **Type**: String action: `"LEFT_CLICK"`, `"RIGHT_CLICK"`, or `"KEY_MACRO_1"` ~ `"KEY_MACRO_5"`

##### `MinecraftClient.PollChatPosts()`
Get player message events.
*   **return**: List of `ChatPost` objects:
    *   **Name**: String player name
    *   **Message**: String message

---

#### Screen methods

##### `MinecraftClient.UpdateScreen(screenId, imageData)`
Update the content of a Custom Screen block.
*   **screenId**: Int ID of the target screen (configured in-game via Stick).
*   **imageData**: String Base64 encoded image data (JPG/PNG).

##### `MinecraftClient.GetScreenLocations(screenId)`
Get world coordinates for all screen blocks with the specified ID.
*   **screenId**: Int ID of the screen.
*   **return**: List of `ScreenLocation` objects with `X`, `Y`, `Z`, `Dimension` properties.

##### `MinecraftClient.RegisterScreen(screenId, x, y, z, dimension)`
Manually register a screen location (used for programmatic screen creation).
*   **screenId**: Int ID of the screen.
*   **x, y, z**: Double coordinates of the screen center.
*   **dimension**: Optional String dimension ID (e.g., `"minecraft:the_nether"`). Default empty.

##### `MinecraftClient.CreateScreenWall(startX, startY, startZ, width, height, axis, screenId, dimension)`
Automatically build a screen wall and register it.
*   **startX, startY, startZ**: Int starting coordinates.
*   **width**: Int width of the screen (in blocks).
*   **height**: Int height of the screen (in blocks).
*   **axis**: Char `'x'` (East-West wall) or `'z'` (North-South wall). Default 'x'.
*   **screenId**: Int ID to assign to the screen. Default 1.
*   **dimension**: Optional String dimension ID. Default empty.

---

### class `AudioManager`

Provides audio playback functionality. All functions are accessed via `mc.Audio`.

#### Loading methods

##### `AudioManager.LoadWav(target, audioId, filepath)`
Load a WAV file and send to clients.
*   **target**: String target player. Use `"@a"` for all players.
*   **audioId**: String unique ID for this audio.
*   **filepath**: String path to WAV file. Supports 8/16-bit PCM, Mono/Stereo.

##### `AudioManager.LoadRaw(target, audioId, pcmData, sampleRate)`
Load raw PCM data.
*   **target**: String target player.
*   **audioId**: String unique ID.
*   **pcmData**: Byte array of 16-bit PCM samples.
*   **sampleRate**: Int sample rate. Default 44100.

##### `AudioManager.GenerateTone(target, audioId, frequency, duration, sampleRate)`
Generate a sine wave tone.
*   **target**: String target player.
*   **audioId**: String unique ID.
*   **frequency**: Double frequency in Hz. Default 440.
*   **duration**: Double duration in seconds. Default 1.0.
*   **sampleRate**: Int sample rate. Default 44100.

---

#### Playback methods

##### `AudioManager.Play(target, audioId, volume, loop)`
Play audio (2D, no spatial positioning).
*   **target**: String target player. Use `"@a"` for all players.
*   **audioId**: String ID of loaded audio.
*   **volume**: Float volume (0.0 - 1.0). Default 1.0f.
*   **loop**: Bool loop playback. Default false.

##### `AudioManager.Play3D(target, audioId, x, y, z, volume, rolloff, loop, dimension, offset)`
Play audio with 3D spatial positioning.
*   **target**: String target player.
*   **audioId**: String ID of loaded audio.
*   **x, y, z**: Double world coordinates.
*   **volume**: Float volume. Default 1.0f.
*   **rolloff**: Float distance attenuation factor. Default 1.0f.
*   **loop**: Bool loop playback. Default false.
*   **dimension**: Optional String dimension ID. Default empty.
*   **offset**: Float start offset in seconds. Default 0.0f.

##### `AudioManager.PlayAt(audioId, x, y, z, radius, volume)`
Play audio at position (simplified 3D).
*   **audioId**: String ID of loaded audio.
*   **x, y, z**: Double world coordinates.
*   **radius**: Double hearing radius. Default 32.
*   **volume**: Float volume. Default 1.0f.

##### `AudioManager.PlayOnScreen(audioId, screenId, volume, loop)`
Play audio bound to a screen block.
*   **audioId**: String ID of loaded audio.
*   **screenId**: Int screen ID.
*   **volume**: Float volume. Default 1.0f.
*   **loop**: Bool loop playback. Default false.

---

#### Control methods

##### `AudioManager.Pause(target, audioId)`
Pause audio playback.
*   **target**: String target player.
*   **audioId**: String ID of audio.

##### `AudioManager.Stop(target, audioId)`
Stop audio playback.
*   **target**: String target player.
*   **audioId**: String ID of audio.

##### `AudioManager.SetVolume(target, audioId, volume)`
Set audio volume.
*   **target**: String target player.
*   **audioId**: String ID of audio.
*   **volume**: Float volume (0.0 - 1.0).

##### `AudioManager.SetPosition(target, audioId, x, y, z)`
Update 3D audio position.
*   **target**: String target player.
*   **audioId**: String ID of audio.
*   **x, y, z**: Double new coordinates.

##### `AudioManager.Clone(target, sourceId, newId)`
Clone loaded audio data (efficient for multiple sources).
*   **target**: String target player.
*   **sourceId**: String ID of source audio.
*   **newId**: String ID for the clone.

##### `AudioManager.Unload(target, audioId)`
Unload audio from memory.
*   **target**: String target player.
*   **audioId**: String ID of audio.

##### `AudioManager.Reset()`
Reset all audio (stop and unload everything).

##### `AudioManager.SyncProgress(audioId, progress)`
Sync playback progress (for video sync).
*   **audioId**: String ID of audio.
*   **progress**: Float progress in seconds.

---

### class `IOManager`

Provides interaction with the IO blocks (redstone control). All functions are accessed via `mc.IO`.

##### `IOManager.Write(channelId, value)`
Send a signal to an IO block (Input Mode).
*   **channelId**: Int ID of the target IO channel.
*   **value**: Can be `int` (0-15) for analog signal strength, or `bool` (true=15, false=0) for digital signal.

##### `IOManager.Read(channelId)`
Read the current signal strength from an IO block (Output Mode).
*   **channelId**: Int ID of the target IO channel.
*   **return**: Int signal strength (0-15).

##### `IOManager.IsHigh(channelId, threshold)`
Check if the signal on a channel is considered "High".
*   **channelId**: Int ID.
*   **threshold**: Int threshold value. Default 7.
*   **return**: Bool true if signal > threshold.

##### `IOManager.IsLow(channelId, threshold)`
Check if the signal on a channel is considered "Low".
*   **channelId**: Int ID.
*   **threshold**: Int threshold value. Default 7.
*   **return**: Bool true if signal <= threshold.

##### `IOManager.Config(x, y, z, channelId, isOutput, dimension)`
Reconfigure an existing IO Block (set ID and Mode).
*   **x, y, z**: Int coordinates of the block.
*   **channelId**: Int new Channel ID.
*   **isOutput**: Bool mode. false=Input, true=Output.
*   **dimension**: Optional String dimension or player ID. Default empty.

##### `IOManager.Config(x, y, z, channelId, mode, dimension)`
Reconfigure an existing IO Block (set ID and Mode) with string mode.
*   **x, y, z**: Int coordinates of the block.
*   **channelId**: Int new Channel ID.
*   **mode**: String mode.
    *   `"in"`, `"input"`: Input Mode (C# controls MC redstone).
    *   `"out"`, `"output"`: Output Mode (MC redstone triggers C#).
*   **dimension**: Optional String dimension or player ID. Default empty.

---

### class `Vec3`

Represents a 3D vector/coordinate.
*   **Properties**: `X`, `Y`, `Z` (double).
*   **Methods**:
    *   `Length()`: Returns vector length.
*   **Operators**: `+`, `-`, `*` (scalar multiplication).

---

### class `PlayerPos`

**Inherits Vec3.** Represents player position with rotation.
*   **Properties**: `X`, `Y`, `Z` (double), `Yaw`, `Pitch` (float).
*   **Methods**:
    *   `Forward(distance)`: Returns a new `Vec3` position at `distance` blocks ahead of the player's view. Default distance is 1.0.

---

### class `ScreenLocation`

Represents the location of a screen or audio source, including dimension information.
*   **Properties**: `X`, `Y`, `Z` (double), `Dimension` (string).

---

### class `BlockHit`

Represents a block click event.
*   **Properties**:
    *   `Pos`: `Vec3` block position.
    *   `Face`: Int clicked face.
    *   `EntityId`: Int player entity ID.
    *   `Action`: Int action code (1=left, 2=right, 101-105=macro keys).
    *   `Type`: String action type (`"LEFT_CLICK"`, `"RIGHT_CLICK"`, `"KEY_MACRO_1"` ~ `"KEY_MACRO_5"`).

---

### class `ChatPost`

Represents a chat message.
*   **Properties**:
    *   `Name`: String player name.
    *   `Message`: String message content.

---

### class `PlayerInfo`

Represents basic player information.
*   **Properties**:
    *   `Name`: String player name.
    *   `Id`: Int entity ID.

---

### class `PlayerDetails`

Represents detailed player information.
*   **Properties**:
    *   `Name`: String player name.
    *   `Id`: Int entity ID.
    *   `Mode`: String gamemode.
    *   `Health`: Float current health.
    *   `MaxHealth`: Float maximum health.
    *   `Food`: Int food level.
    *   `HeldItem`: String held item ID.
    *   `HeldCount`: Int held item count.

---

### class `InventoryItem`

Represents an item in inventory.
*   **Properties**:
    *   `Slot`: Int slot number (0-40).
    *   `Id`: String item ID.
    *   `Count`: Int stack count.

---

### class `EntityInfo`

Represents entity information.
*   **Properties**:
    *   `Id`: Int entity ID.
    *   `Type`: String entity type (e.g., `"minecraft:zombie"`).
    *   `Pos`: `Vec3` position.

---

## Example: Simple Game Loop

```csharp
using MCAPIBridge;
using System;
using System.Threading;

class Program
{
    static void Main()
    {
        using (var mc = new MinecraftClient())
        {
            mc.PostToChat("§aGame Started!");

            while (true)
            {
                // Check for block hits
                var hits = mc.PollBlockHits();
                foreach (var hit in hits)
                {
                    if (hit.Type == "RIGHT_CLICK")
                    {
                        // Place diamond block where player clicks
                        mc.SetBlock((int)hit.Pos.X, (int)hit.Pos.Y, (int)hit.Pos.Z, "diamond_block");
                        mc.SpawnParticle(hit.Pos.X, hit.Pos.Y + 1, hit.Pos.Z, "happy_villager", 20);
                    }
                }

                // Check for chat commands
                var chats = mc.PollChatPosts();
                foreach (var chat in chats)
                {
                    if (chat.Message == "!heal")
                    {
                        mc.SetHealth(chat.Name, 20);
                        mc.PostToChat(string.Format("§a{0} has been healed!", chat.Name));
                    }
                    else if (chat.Message == "!fly")
                    {
                        mc.SetFlying(chat.Name, true, true);
                        mc.PostToChat(string.Format("§b{0} can now fly!", chat.Name));
                    }
                }

                Thread.Sleep(50);  // 20 ticks per second
            }
        }
    }
}
```

---

## Requirements

- **.NET Standard 2.0+** / **.NET 6.0+** / **.NET 8.0+**
- **MCAPIBridge Mod** installed on Minecraft server
- Minecraft: See [Modrinth](https://modrinth.com/mod/mcapibridge)

---

## Links

- **Mod Download**: [Modrinth](https://modrinth.com/mod/mcapibridge)
- **GitHub**: [MCAPIBridge](https://github.com/TaotianZhufang/MCAPIBridge)
- **Python Library**: [Python README](https://github.com/TaotianZhufang/MCAPIBridge/blob/main/Bridges/Python/README.MD)

---

## License

MIT License