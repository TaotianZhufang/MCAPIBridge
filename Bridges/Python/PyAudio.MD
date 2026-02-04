# MCAPIBridge Audio System Documentation

#### English|[简体中文](https://github.com/TaotianZhufang/MCAPIBridge/blob/main/Bridges/Python/PyAudio_zh_CN.MD)

## 📚 AudioManager Class

All audio functions are accessed via `mc.audio`.

```python
from mc import Minecraft

mc = Minecraft()
# Access audio functions via mc.audio
```

---

## 🎵 Audio Loading Functions

### `generateTone(target, audioId, frequency, duration, sampleRate)`

**Function**: Generate a sine wave tone

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name, `""` for first player, `"@a"` for all players | Required |
| audioId | str | Unique audio identifier | Required |
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

---

### `loadWav(target, audioId, filepath)`

**Function**: Load a WAV audio file

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audioId | str | Unique audio identifier |
| filepath | str | Path to WAV file |

**Supported Formats**:
- 8-bit or 16-bit PCM
- Mono or Stereo (auto-converts to mono)
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
- Large files are automatically chunked for transmission
- Wait before playing after loading (recommended: `time.sleep(1.0)`)

---

### `loadRaw(target, audioId, pcmData, sampleRate)`

**Function**: Load raw PCM audio data

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

---

## ▶️ Playback Control Functions

### `play(target, audioId, volume, loop)`

**Function**: Normal playback (no 3D effect)

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

---

### `play3d(target, audioId, x, y, z, volume, rolloff)`

**Function**: 3D spatial audio playback

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name | Required |
| audioId | str | Loaded audio ID | Required |
| x, y, z | float | Sound source world coordinates | Required |
| volume | float | Volume (0.0 - 1.0) | 1.0 |
| rolloff | float | Distance attenuation factor | 1.0 |

**Rolloff Parameter**:
| Value | Effect |
|-------|--------|
| 0.5 | Slow attenuation, audible from far away |
| 1.0 | Normal attenuation |
| 2.0 | Fast attenuation, only audible nearby |

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

---

### `pause(target, audioId)`

**Function**: Pause playback

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audioId | str | Audio ID |

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

---

### `stop(target, audioId)`

**Function**: Stop playback

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audioId | str | Audio ID |

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

**Function**: Dynamically adjust volume

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

---

### `setPosition(target, audioId, x, y, z)`

**Function**: Update 3D sound source position (for moving sources)

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

---

### `unload(target, audioId)`

**Function**: Unload audio, free memory

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

## 📖 Complete Usage Examples

### Example 1: Simple Sound Effect

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Generate and play
mc.audio.generateTone(player, "beep", 440, 2.0)
time.sleep(1.0)
mc.audio.play(player, "beep")
time.sleep(2.5)
mc.audio.unload(player, "beep")
```

---

### Example 2: Background Music

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Load WAV file
mc.audio.loadWav(player, "bgm", "music.wav")
time.sleep(2.0)

# Loop playback
mc.audio.play(player, "bgm", volume=0.6, loop=True)

# Play for 30 seconds
time.sleep(30)

# Fade out
for v in [0.5, 0.4, 0.3, 0.2, 0.1, 0.0]:
    mc.audio.setVolume(player, "bgm", v)
    time.sleep(0.3)

mc.audio.stop(player, "bgm")
mc.audio.unload(player, "bgm")
```

---

### Example 3: 3D Sound Effect

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]
pos = mc.getPlayerPos(player)

# Generate sound effect
mc.audio.generateTone(player, "alert", 600, 3.0)
time.sleep(1.0)

# Play 10 blocks in front of player
front = pos.forward(10)
mc.audio.play3d(player, "alert", front.x, front.y, front.z, 1.0, 1.0)

mc.postToChat("Turn around to feel the 3D effect!")
time.sleep(4)

mc.audio.unload(player, "alert")
```

---

### Example 4: Surround Sound Effect

```python
from mc import Minecraft
import time
import math

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]
pos = mc.getPlayerPos(player)

# Generate long audio
mc.audio.generateTone(player, "surround", 500, 15.0)
time.sleep(1.5)

# Start playback
mc.audio.play3d(player, "surround", pos.x + 10, pos.y, pos.z, 1.0, 0.3)

mc.postToChat("Sound circling around you!")

# Circle around
radius = 10
for i in range(80):
    angle = i * 0.12
    x = pos.x + radius * math.cos(angle)
    z = pos.z + radius * math.sin(angle)
    mc.audio.setPosition(player, "surround", x, pos.y, z)
    time.sleep(0.1)

mc.audio.stop(player, "surround")
mc.audio.unload(player, "surround")
```

---

### Example 5: Multi-Source Chord

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# C Major chord: C E G
mc.audio.generateTone(player, "noteC", 262, 3.0)
mc.audio.generateTone(player, "noteE", 330, 3.0)
mc.audio.generateTone(player, "noteG", 392, 3.0)
time.sleep(1.5)

mc.postToChat("C Major Chord!")

# Play simultaneously
mc.audio.play(player, "noteC", volume=0.6)
mc.audio.play(player, "noteE", volume=0.6)
mc.audio.play(player, "noteG", volume=0.6)

time.sleep(4)

# Cleanup
for note in ["noteC", "noteE", "noteG"]:
    mc.audio.unload(player, note)
```

---

### Example 6: Distance Attenuation Effect

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]
pos = mc.getPlayerPos(player)

mc.audio.generateTone(player, "approach", 440, 10.0)
time.sleep(1.0)

# Start from far away
startDist = 50
endDist = 3

mc.audio.play3d(player, "approach", pos.x, pos.y, pos.z + startDist, 1.0, 1.0)

mc.postToChat("Sound approaching...")

# Approach
for i in range(30):
    dist = startDist - (startDist - endDist) * (i / 30)
    mc.audio.setPosition(player, "approach", pos.x, pos.y, pos.z + dist)
    time.sleep(0.1)

mc.postToChat("Closest point!")
time.sleep(1)

mc.postToChat("Sound moving away...")

# Move away
for i in range(30):
    dist = endDist + (startDist - endDist) * (i / 30)
    mc.audio.setPosition(player, "approach", pos.x, pos.y, pos.z + dist)
    time.sleep(0.1)

mc.audio.stop(player, "approach")
mc.audio.unload(player, "approach")
```

---

## ⚠️ Important Notes

1. **Loading Wait Time**: Audio loading takes time, larger files need longer wait
   ```python
   mc.audio.generateTone(player, "tone", 440, 5.0)
   time.sleep(1.0)  # Wait for loading
   mc.audio.play(player, "tone")
   ```

2. **Resource Release**: Remember to `unload` after use
   ```python
   mc.audio.stop(player, "sound")
   mc.audio.unload(player, "sound")
   ```

3. **Unique Audio ID**: Same ID will overwrite previous audio

4. **WAV Format**: Only PCM format supported, compressed WAV not supported

5. **Target Parameter**:
   - `"PlayerName"` - Specific player
   - `""` - First player
   - `"@a"` - All players

---

## 🔧 Quick Reference

```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# Generate tone
mc.audio.generateTone(player, "id", frequency=440, duration=2.0)

# Load WAV
mc.audio.loadWav(player, "id", "file.wav")

# Normal play
mc.audio.play(player, "id", volume=1.0, loop=False)

# 3D play
mc.audio.play3d(player, "id", x, y, z, volume=1.0, rolloff=1.0)

# Update position
mc.audio.setPosition(player, "id", x, y, z)

# Set volume
mc.audio.setVolume(player, "id", 0.5)

# Pause
mc.audio.pause(player, "id")

# Stop
mc.audio.stop(player, "id")

# Unload
mc.audio.unload(player, "id")
```