# MCAPIBridge Audio System Documentation

#### English|[简体中文](https://github.com/TaotianZhufang/MCAPIBridge/blob/main/Bridges/Python/PyAudio_zh_CN.MD)

## 📚 AudioManager Class

All audio functions are accessed via `mc.audio`.

---

## 🎵 Audio Loading Functions

### `generate_tone(target, audio_id, frequency, duration, sample_rate)`

**Function**: Generate a sine wave tone

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name, `""` for first player, `"@a"` for all players | Required |
| audio_id | str | Unique audio identifier (custom name) | Required |
| frequency | float | Frequency in Hz | 440 |
| duration | float | Duration in seconds | 1.0 |
| sample_rate | int | Sample rate | 44100 |

**Examples**:
```python
# Generate A4 tone (440Hz), 2 seconds
mc.audio.generate_tone("Steve", "beep", 440, 2.0)

# Generate high C (523Hz), 1 second
mc.audio.generate_tone("Steve", "high_c", 523, 1.0)

# Generate for all players
mc.audio.generate_tone("@a", "alarm", 880, 3.0)

# Generate for first player
mc.audio.generate_tone("", "tone", 440, 1.0)
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

### `load_wav(target, audio_id, filepath)`

**Function**: Load a WAV audio file

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audio_id | str | Unique audio identifier |
| filepath | str | Path to WAV file |

**Supported Formats**:
- 8-bit or 16-bit PCM
- Mono or Stereo (auto-converts to mono)
- Any sample rate

**Examples**:
```python
# Load music file
mc.audio.load_wav("Steve", "bgm", "music/background.wav")

# Load sound effect
mc.audio.load_wav("Steve", "explosion", "sounds/boom.wav")

# Load for all players
mc.audio.load_wav("@a", "victory", "sounds/win.wav")
```

**Notes**:
- Large files are automatically chunked for transmission
- Wait before playing after loading (recommended: `time.sleep(1.0)`)

---

### `load_raw(target, audio_id, pcm_data, sample_rate)`

**Function**: Load raw PCM audio data

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name | Required |
| audio_id | str | Unique audio identifier | Required |
| pcm_data | bytes | 16-bit signed integer PCM data | Required |
| sample_rate | int | Sample rate | 44100 |

**Example**:
```python
import struct
import math

# Manually generate audio data
sample_rate = 44100
duration = 2.0
frequency = 440

samples = []
for i in range(int(sample_rate * duration)):
    t = i / sample_rate
    value = int(32767 * math.sin(2 * math.pi * frequency * t))
    samples.append(value)

pcm_data = struct.pack(f'<{len(samples)}h', *samples)
mc.audio.load_raw("Steve", "custom", pcm_data, sample_rate)
```

---

## ▶️ Playback Control Functions

### `play(target, audio_id, volume, loop)`

**Function**: Normal playback (no 3D effect)

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name | Required |
| audio_id | str | Loaded audio ID | Required |
| volume | float | Volume (0.0 - 1.0) | 1.0 |
| loop | bool | Whether to loop | False |

**Examples**:
```python
# Load audio
mc.audio.generate_tone("Steve", "beep", 440, 2.0)
time.sleep(1.0)

# Normal playback
mc.audio.play("Steve", "beep")

# Set volume
mc.audio.play("Steve", "beep", volume=0.5)

# Loop playback
mc.audio.play("Steve", "beep", volume=0.8, loop=True)

# Stop after 3 seconds
time.sleep(3)
mc.audio.stop("Steve", "beep")
```

---

### `play_3d(target, audio_id, x, y, z, volume, rolloff)`

**Function**: 3D spatial audio playback

**Parameters**:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| target | str | Player name | Required |
| audio_id | str | Loaded audio ID | Required |
| x, y, z | float | Sound source world coordinates | Required |
| volume | float | Volume (0.0 - 1.0) | 1.0 |
| rolloff | float | Distance attenuation factor | 1.0 |

**Rolloff Parameter Explanation**:
| Value | Effect |
|-------|--------|
| 0.5 | Slow attenuation, audible from far away |
| 1.0 | Normal attenuation |
| 2.0 | Fast attenuation, only audible nearby |

**Examples**:
```python
# Get player position
pos = mc.getPlayerPos("Steve")

# Play 10 blocks in front of player
mc.audio.generate_tone("Steve", "front", 500, 5.0)
time.sleep(1.0)

front = pos.forward(10)
mc.audio.play_3d("Steve", "front", front.x, front.y, front.z, 1.0, 1.0)

# Play at fixed coordinates
mc.audio.play_3d("Steve", "sound", 100, 64, 200, 0.8, 0.5)

# Slow attenuation (audible from far)
mc.audio.play_3d("Steve", "ambient", x, y, z, 1.0, 0.3)

# Fast attenuation (only audible nearby)
mc.audio.play_3d("Steve", "whisper", x, y, z, 1.0, 2.0)
```

---

### `pause(target, audio_id)`

**Function**: Pause playback

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audio_id | str | Audio ID |

**Example**:
```python
mc.audio.play("Steve", "music", loop=True)
time.sleep(5)

# Pause
mc.audio.pause("Steve", "music")
time.sleep(2)

# Resume playback
mc.audio.play("Steve", "music")
```

---

### `stop(target, audio_id)`

**Function**: Stop playback

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audio_id | str | Audio ID |

**Example**:
```python
mc.audio.play("Steve", "music", loop=True)
time.sleep(10)

# Stop
mc.audio.stop("Steve", "music")
```

---

## 🎛️ Audio Property Functions

### `set_volume(target, audio_id, volume)`

**Function**: Dynamically adjust volume

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audio_id | str | Audio ID |
| volume | float | Volume (0.0 - 1.0) |

**Example**:
```python
mc.audio.play("Steve", "music")

# Volume fade
for v in [1.0, 0.8, 0.6, 0.4, 0.2, 0.0]:
    mc.audio.set_volume("Steve", "music", v)
    time.sleep(0.5)
```

---

### `set_position(target, audio_id, x, y, z)` (requires _send call)

**Function**: Update 3D sound source position (for moving sources)

**Example**:
```python
import math

pos = mc.getPlayerPos("Steve")

# Play 3D sound effect
mc.audio.play_3d("Steve", "moving", pos.x + 10, pos.y, pos.z, 1.0, 0.5)

# Make sound source circle around player
for i in range(60):
    angle = i * 0.1
    x = pos.x + 10 * math.cos(angle)
    z = pos.z + 10 * math.sin(angle)
    mc._send(f"audio.position(Steve,moving,{x},{pos.y},{z})")
    time.sleep(0.1)
```

---

### `unload(target, audio_id)`

**Function**: Unload audio, free memory

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| target | str | Player name |
| audio_id | str | Audio ID |

**Examples**:
```python
# Unload after use
mc.audio.stop("Steve", "music")
mc.audio.unload("Steve", "music")

# Batch unload
audio_ids = ["sound1", "sound2", "sound3"]
for aid in audio_ids:
    mc.audio.unload("Steve", aid)
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
mc.audio.generate_tone(player, "beep", 440, 2.0)
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
mc.audio.load_wav(player, "bgm", "music.wav")
time.sleep(2.0)

# Loop playback
mc.audio.play(player, "bgm", volume=0.6, loop=True)

# Play for 30 seconds
time.sleep(30)

# Fade out
for v in [0.5, 0.4, 0.3, 0.2, 0.1, 0.0]:
    mc.audio.set_volume(player, "bgm", v)
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
mc.audio.generate_tone(player, "alert", 600, 3.0)
time.sleep(1.0)

# Play 10 blocks in front of player
front = pos.forward(10)
mc.audio.play_3d(player, "alert", front.x, front.y, front.z, 1.0, 1.0)

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
mc.audio.generate_tone(player, "surround", 500, 15.0)
time.sleep(1.5)

# Start playback
mc.audio.play_3d(player, "surround", pos.x + 10, pos.y, pos.z, 1.0, 0.3)

mc.postToChat("Sound is circling around you!")

# Circle around
radius = 10
for i in range(80):
    angle = i * 0.12
    x = pos.x + radius * math.cos(angle)
    z = pos.z + radius * math.sin(angle)
    mc._send(f"audio.position({player},surround,{x},{pos.y},{z})")
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
mc.audio.generate_tone(player, "c", 262, 3.0)
mc.audio.generate_tone(player, "e", 330, 3.0)
mc.audio.generate_tone(player, "g", 392, 3.0)
time.sleep(1.5)

mc.postToChat("C Major Chord!")

# Play simultaneously
mc.audio.play(player, "c", volume=0.6)
mc.audio.play(player, "e", volume=0.6)
mc.audio.play(player, "g", volume=0.6)

time.sleep(4)

# Cleanup
for note in ["c", "e", "g"]:
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

mc.audio.generate_tone(player, "approach", 440, 10.0)
time.sleep(1.0)

# Start from far away
start_dist = 50
end_dist = 3

mc.audio.play_3d(player, "approach", pos.x, pos.y, pos.z + start_dist, 1.0, 1.0)

mc.postToChat("Sound approaching...")

# Approach
for i in range(30):
    dist = start_dist - (start_dist - end_dist) * (i / 30)
    mc._send(f"audio.position({player},approach,{pos.x},{pos.y},{pos.z + dist})")
    time.sleep(0.1)

mc.postToChat("Closest point!")
time.sleep(1)

mc.postToChat("Sound moving away...")

# Move away
for i in range(30):
    dist = end_dist + (start_dist - end_dist) * (i / 30)
    mc._send(f"audio.position({player},approach,{pos.x},{pos.y},{pos.z + dist})")
    time.sleep(0.1)

mc.audio.stop(player, "approach")
mc.audio.unload(player, "approach")
```

---

## ⚠️ Important Notes

1. **Loading Wait Time**: Audio loading takes time, larger files need longer wait
   ```python
   mc.audio.generate_tone(player, "tone", 440, 5.0)
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