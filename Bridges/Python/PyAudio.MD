# MCAPIBridge Audio System Documentation

#### English | [简体中文](https://github.com/TaotianZhufang/MCAPIBridge/blob/main/Bridges/Python/PyAudio_zh_CN.MD)

## 📚 AudioManager Class

All audio functions are accessed via `mc.audio`.

```python
from mc import Minecraft

mc = Minecraft()
# Access audio functions via mc.audio
```

---

## 🎵 Audio Loading & Management

### `generateTone(target, audioId, frequency, duration, sampleRate)`

**Function**: Generate a sine wave tone and upload it to the client.

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name, `""` for first player, `"@a"` for all players | Required |
| audioId | str | Unique audio identifier (custom name) | Required |
| frequency | float | Frequency in Hz | 440 |
| duration | float | Duration in seconds | 1.0 |
| sampleRate | int | Sample rate | 44100 |

**Example**:
```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Generate A4 tone (440Hz), 2 seconds
mc.audio.generateTone(player, "beep", 440, 2.0)

# Generate high C (523Hz), 1 second
mc.audio.generateTone(player, "highC", 523, 1.0)

# Generate for all players
mc.audio.generateTone("@a", "alarm", 880, 3.0)

# Generate for first player
mc.audio.generateTone("", "tone", 440, 1.0)
```

**Common Frequency Reference**:
| Note | Frequency (Hz) |
|------|----------------|
| C4 (Middle C) | 262 |
| D4 | 294 |
| E4 | 330 |
| F4 | 349 |
| G4 | 392 |
| A4 | 440 |
| B4 | 494 |
| C5 | 523 |
| A5 | 880 |

### `loadWav(target, audioId, filepath)`

**Function**: Load a local WAV audio file to the client.

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audioId | str | Unique audio identifier |
| filepath | str | Path to local WAV file |

**Supported Formats**:
- 8-bit or 16-bit PCM
- Mono or Stereo (auto-converts to mono for 3D spatialization)
- Any sample rate

**Example**:
```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Load music file
mc.audio.loadWav(player, "bgm", "music/background.wav")

# Load sound effect
mc.audio.loadWav(player, "explosion", "sounds/boom.wav")

# Load for all players
mc.audio.loadWav("@a", "victory", "sounds/win.wav")
```

**Notes**:
- Large files are automatically chunked for transmission.
- Wait before playing after loading (recommended: `time.sleep(1.0)` or more for large files).

### `loadRaw(target, audioId, pcmData, sampleRate)`

**Function**: Load raw PCM audio data (bytes).

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name | Required |
| audioId | str | Unique audio identifier | Required |
| pcmData | bytes | 16-bit signed integer PCM data | Required |
| sampleRate | int | Sample rate | 44100 |

**Example**:
```python
from mc import Minecraft
import struct
import math

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Manually generate audio data
sampleRate = 44100
duration = 2.0
frequency = 440

samples = []
for i in range(int(sampleRate * duration)):
    t = i / sampleRate
    value = int(32767 * math.sin(2 * math.pi * frequency * t))
    samples.append(value)

pcmData = struct.pack(f'<{len(samples)}h', *samples)
mc.audio.loadRaw(player, "custom", pcmData, sampleRate)
```

### `clone(target, sourceId, newId)`

**Function**: Clone an existing audio source. This reuses the memory buffer of the source, saving bandwidth and RAM. Essential for multi-speaker setups.

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| sourceId | str | ID of the existing loaded audio |
| newId | str | ID for the new instance |

**Example**:
```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Load audio once
mc.audio.generateTone(player, "master_beep", 440, 2.0)

# Clone it for multi-source playback
mc.audio.clone(player, "master_beep", "beep_left")
mc.audio.clone(player, "master_beep", "beep_right")

# Play clones at different locations
mc.audio.play3d(player, "beep_left", 100, 64, 200, 1.0, 1.0)
mc.audio.play3d(player, "beep_right", 110, 64, 200, 1.0, 1.0)
```

### `unload(target, audioId)`

**Function**: Unload audio and free memory.

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audioId | str | Audio ID |

**Example**:
```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Unload after use
mc.audio.stop(player, "music")
mc.audio.unload(player, "music")

# Batch unload
audioIds = ["sound1", "sound2", "sound3"]
for aid in audioIds:
    mc.audio.unload(player, aid)
```

---

## ▶️ Playback Control Functions

### `play(target, audioId, volume, loop)`

**Function**: Normal 2D playback (no spatial effects, follows player). Used for BGM or UI sounds.

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name | Required |
| audioId | str | Loaded audio ID | Required |
| volume | float | Volume (0.0 - 1.0) | 1.0 |
| loop | bool | Whether to loop | False |

**Example**:
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Load audio
mc.audio.generateTone(player, "beep", 440, 2.0)
time.sleep(1.0)

# Normal playback
mc.audio.play(player, "beep")

# Set volume
mc.audio.play(player, "beep", volume=0.5)

# Loop playback
mc.audio.play(player, "beep", volume=0.8, loop=True)

# Stop after 3 seconds
time.sleep(3)
mc.audio.stop(player, "beep")
```

### `play3d(target, audioId, x, y, z, volume, rolloff, loop, dimension, offset)`

**Function**: 3D spatial audio playback at a fixed location.

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name | Required |
| audioId | str | Loaded audio ID | Required |
| x, y, z | float | Sound source world coordinates | Required |
| volume | float | Volume (0.0 - 1.0) | 1.0 |
| rolloff | float | Distance attenuation factor | 1.0 |
| loop | bool | Whether to loop | False |
| dimension | str | Dimension ID (e.g. `minecraft:the_nether`). `""` = auto. | `""` |
| offset | float | Start playback from N seconds | 0.0 |

**Rolloff Parameter**:
| Value | Effect |
|-------|--------|
| 0.5 | Slow attenuation (audible from far) |
| 1.0 | Normal attenuation |
| 2.0 | Fast attenuation (only audible nearby) |

**Example**:
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Get player position
pos = mc.getPlayerPos(player)

# Play 10 blocks in front of player
mc.audio.generateTone(player, "front", 500, 5.0)
time.sleep(1.0)

front = pos.forward(10)
mc.audio.play3d(player, "front", front.x, front.y, front.z, 1.0, 1.0)

# Play at fixed coordinates
mc.audio.play3d(player, "sound", 100, 64, 200, 0.8, 0.5)

# Slow attenuation (audible from far)
mc.audio.play3d(player, "ambient", x, y, z, 1.0, 0.3)

# Fast attenuation (only audible nearby)
mc.audio.play3d(player, "whisper", x, y, z, 1.0, 2.0)
```

### `pause(target, audioId)`

**Function**: Pause playback.

**Example**:
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

mc.audio.play(player, "music", loop=True)
time.sleep(5)

# Pause
mc.audio.pause(player, "music")
time.sleep(2)

# Resume playback
mc.audio.play(player, "music")
```

### `stop(target, audioId)`

**Function**: Stop playback (resets progress).

**Example**:
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

mc.audio.play(player, "music", loop=True)
time.sleep(10)

# Stop
mc.audio.stop(player, "music")
```

---

## 🎛️ Audio Property Functions

### `setVolume(target, audioId, volume)`

**Function**: Dynamically adjust volume of a playing audio.

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audioId | str | Audio ID |
| volume | float | Volume (0.0 - 1.0) |

**Example**:
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

mc.audio.play(player, "music")

# Volume fade
for v in [1.0, 0.8, 0.6, 0.4, 0.2, 0.0]:
    mc.audio.setVolume(player, "music", v)
    time.sleep(0.5)
```

### `setPosition(target, audioId, x, y, z)`

**Function**: Update 3D sound source position (for moving sound sources).

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audioId | str | Audio ID |
| x, y, z | float | New world coordinates |

**Example**:
```python
from mc import Minecraft
import math

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]
pos = mc.getPlayerPos(player)

# Play 3D sound effect
mc.audio.play3d(player, "moving", pos.x + 10, pos.y, pos.z, 1.0, 0.5)

# Make sound source circle around player
for i in range(60):
    angle = i * 0.1
    x = pos.x + 10 * math.cos(angle)
    z = pos.z + 10 * math.sin(angle)
    mc.audio.setPosition(player, "moving", x, pos.y, z)
    time.sleep(0.1)
```

### `syncProgress(audioId, progress)`

**Function**: Manually sync the server-side playback timer. Useful for long videos to correct audio drift.

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| audioId | str | Audio ID |
| progress | float | Current timestamp in seconds |

---

## ⚠️ Important Notes

1.  **Loading Wait Time**: Audio loading takes time (network transfer). For large files (>5MB), use `time.sleep(2.0)` or more before calling `play` or `clone`.
2.  **Resource Release**: Always `unload` audio IDs when they are no longer needed to free up client RAM.
3.  **Unique Audio ID**: Using an existing ID will overwrite the previous audio data associated with it.
4.  **WAV Format**: The library supports PCM WAV. Compressed WAVs are not supported directly.
5.  **Target Parameter**:
    *   `"PlayerName"` - Specific player.
    *   `""` - First player in the list.
    *   `"@a"` - All online players.

---

## 🔧 Quick Reference

```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 1. Generate/Load
mc.audio.generateTone(player, "id", frequency=440, duration=2.0)
# OR
mc.audio.loadWav(player, "id", "file.wav")

# 2. Play
# 2D BGM
mc.audio.play(player, "id", volume=1.0, loop=False)
# 3D Positional
mc.audio.play3d(player, "id", x, y, z, volume=1.0, rolloff=1.0)

# 3. Control
mc.audio.setVolume(player, "id", 0.5)
mc.audio.setPosition(player, "id", new_x, new_y, new_z)
mc.audio.pause(player, "id")
mc.audio.stop(player, "id")

# 4. Cleanup
mc.audio.unload(player, "id")
```