# MCAPIBridge 音频系统完整文档

#### [English](https://github.com/TaotianZhufang/MCAPIBridge/blob/main/Bridges/Python/PyAudio.MD)|简体中文

## 📚 AudioManager 类

所有音频功能通过 `mc.audio` 访问。

```python
from mc import Minecraft

mc = Minecraft()
# 通过 mc.audio 访问音频功能
```

---

## 🎵 音频加载函数

### `generate_tone(target, audio_id, frequency, duration, sample_rate)`

**功能**：生成正弦波音调

**参数**：
| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| target | str | 玩家名，`""` 第一个玩家，`"@a"` 所有玩家 | 必填 |
| audio_id | str | 音频唯一标识符（自定义命名） | 必填 |
| frequency | float | 频率 (Hz) | 440 |
| duration | float | 持续时间 (秒) | 1.0 |
| sample_rate | int | 采样率 | 44100 |

**示例**：
```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 生成 A4 音调 (440Hz)，持续 2 秒
mc.audio.generate_tone(player, "beep", 440, 2.0)

# 生成高音 C (523Hz)，持续 1 秒
mc.audio.generate_tone(player, "high_c", 523, 1.0)

# 对所有玩家生成音调
mc.audio.generate_tone("@a", "alarm", 880, 3.0)

# 对第一个玩家生成
mc.audio.generate_tone("", "tone", 440, 1.0)
```

**常用频率参考**：
| 音符 | 频率 (Hz) |
|------|-----------|
| C4 (中央C) | 262 |
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

**功能**：加载 WAV 音频文件

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| target | str | 玩家名 |
| audio_id | str | 音频唯一标识符 |
| filepath | str | WAV 文件路径 |

**支持格式**：
- 8位 或 16位 PCM
- 单声道 或 立体声（自动转单声道）
- 任意采样率

**示例**：
```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 加载音乐文件
mc.audio.load_wav(player, "bgm", "music/background.wav")

# 加载音效
mc.audio.load_wav(player, "explosion", "sounds/boom.wav")

# 加载到所有玩家
mc.audio.load_wav("@a", "victory", "sounds/win.wav")
```

**注意**：
- 大文件会自动分块传输
- 加载后需要等待一会再播放（建议 `time.sleep(1.0)`）

---

### `load_raw(target, audio_id, pcm_data, sample_rate)`

**功能**：加载原始 PCM 音频数据

**参数**：
| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| target | str | 玩家名 | 必填 |
| audio_id | str | 音频唯一标识符 | 必填 |
| pcm_data | bytes | 16位有符号整数 PCM 数据 | 必填 |
| sample_rate | int | 采样率 | 44100 |

**示例**：
```python
from mc import Minecraft
import struct
import math

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 手动生成音频数据
sample_rate = 44100
duration = 2.0
frequency = 440

samples = []
for i in range(int(sample_rate * duration)):
    t = i / sample_rate
    value = int(32767 * math.sin(2 * math.pi * frequency * t))
    samples.append(value)

pcm_data = struct.pack(f'<{len(samples)}h', *samples)
mc.audio.load_raw(player, "custom", pcm_data, sample_rate)
```

---

## ▶️ 播放控制函数

### `play(target, audio_id, volume, loop)`

**功能**：普通播放（无 3D 效果）

**参数**：
| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| target | str | 玩家名 | 必填 |
| audio_id | str | 已加载的音频 ID | 必填 |
| volume | float | 音量 (0.0 - 1.0) | 1.0 |
| loop | bool | 是否循环 | False |

**示例**：
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 加载音频
mc.audio.generate_tone(player, "beep", 440, 2.0)
time.sleep(1.0)

# 普通播放
mc.audio.play(player, "beep")

# 设置音量
mc.audio.play(player, "beep", volume=0.5)

# 循环播放
mc.audio.play(player, "beep", volume=0.8, loop=True)

# 3秒后停止
time.sleep(3)
mc.audio.stop(player, "beep")
```

---

### `play_3d(target, audio_id, x, y, z, volume, rolloff)`

**功能**：3D 空间音效播放

**参数**：
| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| target | str | 玩家名 | 必填 |
| audio_id | str | 已加载的音频 ID | 必填 |
| x, y, z | float | 音源在世界中的坐标 | 必填 |
| volume | float | 音量 (0.0 - 1.0) | 1.0 |
| rolloff | float | 距离衰减系数 | 1.0 |

**rolloff 参数说明**：
| 值 | 效果 |
|-----|------|
| 0.5 | 慢衰减，远处也能听清 |
| 1.0 | 正常衰减 |
| 2.0 | 快衰减，只有近处能听到 |

**示例**：
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 获取玩家位置
pos = mc.getPlayerPos(player)

# 在玩家前方 10 格播放
mc.audio.generate_tone(player, "front", 500, 5.0)
time.sleep(1.0)

front = pos.forward(10)
mc.audio.play_3d(player, "front", front.x, front.y, front.z, 1.0, 1.0)

# 在固定坐标播放
mc.audio.play_3d(player, "sound", 100, 64, 200, 0.8, 0.5)

# 慢衰减（远处也能听到）
mc.audio.play_3d(player, "ambient", x, y, z, 1.0, 0.3)

# 快衰减（只有近处能听到）
mc.audio.play_3d(player, "whisper", x, y, z, 1.0, 2.0)
```

---

### `pause(target, audio_id)`

**功能**：暂停播放

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| target | str | 玩家名 |
| audio_id | str | 音频 ID |

**示例**：
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

mc.audio.play(player, "music", loop=True)
time.sleep(5)

# 暂停
mc.audio.pause(player, "music")
time.sleep(2)

# 继续播放
mc.audio.play(player, "music")
```

---

### `stop(target, audio_id)`

**功能**：停止播放

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| target | str | 玩家名 |
| audio_id | str | 音频 ID |

**示例**：
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

mc.audio.play(player, "music", loop=True)
time.sleep(10)

# 停止
mc.audio.stop(player, "music")
```

---

## 🎛️ 音频属性函数

### `set_volume(target, audio_id, volume)`

**功能**：动态调整音量

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| target | str | 玩家名 |
| audio_id | str | 音频 ID |
| volume | float | 音量 (0.0 - 1.0) |

**示例**：
```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

mc.audio.play(player, "music")

# 音量渐变
for v in [1.0, 0.8, 0.6, 0.4, 0.2, 0.0]:
    mc.audio.set_volume(player, "music", v)
    time.sleep(0.5)
```

---

### `set_position(target, audio_id, x, y, z)` (需通过 _send 调用)

**功能**：更新 3D 音源位置（用于移动的音源）

**示例**：
```python
from mc import Minecraft
import math

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]
pos = mc.getPlayerPos(player)

# 播放 3D 音效
mc.audio.play_3d(player, "moving", pos.x + 10, pos.y, pos.z, 1.0, 0.5)

# 让音源绕玩家转圈
for i in range(60):
    angle = i * 0.1
    x = pos.x + 10 * math.cos(angle)
    z = pos.z + 10 * math.sin(angle)
    mc._send(f"audio.position({player},moving,{x},{pos.y},{z})")
    time.sleep(0.1)
```

---

### `unload(target, audio_id)`

**功能**：卸载音频，释放内存

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| target | str | 玩家名 |
| audio_id | str | 音频 ID |

**示例**：
```python
from mc import Minecraft

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 使用完毕后卸载
mc.audio.stop(player, "music")
mc.audio.unload(player, "music")

# 批量卸载
audio_ids = ["sound1", "sound2", "sound3"]
for aid in audio_ids:
    mc.audio.unload(player, aid)
```

---

## 📖 完整使用示例

### 示例 1：简单音效

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 生成并播放
mc.audio.generate_tone(player, "beep", 440, 2.0)
time.sleep(1.0)
mc.audio.play(player, "beep")
time.sleep(2.5)
mc.audio.unload(player, "beep")
```

---

### 示例 2：背景音乐

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# 加载 WAV 文件
mc.audio.load_wav(player, "bgm", "music.wav")
time.sleep(2.0)

# 循环播放
mc.audio.play(player, "bgm", volume=0.6, loop=True)

# 播放 30 秒
time.sleep(30)

# 渐弱结束
for v in [0.5, 0.4, 0.3, 0.2, 0.1, 0.0]:
    mc.audio.set_volume(player, "bgm", v)
    time.sleep(0.3)

mc.audio.stop(player, "bgm")
mc.audio.unload(player, "bgm")
```

---

### 示例 3：3D 音效

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]
pos = mc.getPlayerPos(player)

# 生成音效
mc.audio.generate_tone(player, "alert", 600, 3.0)
time.sleep(1.0)

# 在玩家前方 10 格播放
front = pos.forward(10)
mc.audio.play_3d(player, "alert", front.x, front.y, front.z, 1.0, 1.0)

mc.postToChat("转动视角感受 3D 音效!")
time.sleep(4)

mc.audio.unload(player, "alert")
```

---

### 示例 4：环绕音效

```python
from mc import Minecraft
import time
import math

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]
pos = mc.getPlayerPos(player)

# 生成长音频
mc.audio.generate_tone(player, "surround", 500, 15.0)
time.sleep(1.5)

# 开始播放
mc.audio.play_3d(player, "surround", pos.x + 10, pos.y, pos.z, 1.0, 0.3)

mc.postToChat("声音绕着你转圈!")

# 绕圈
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

### 示例 5：多音源和弦

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]

# C 大调和弦: C E G
mc.audio.generate_tone(player, "c", 262, 3.0)
mc.audio.generate_tone(player, "e", 330, 3.0)
mc.audio.generate_tone(player, "g", 392, 3.0)
time.sleep(1.5)

mc.postToChat("C 大调和弦!")

# 同时播放
mc.audio.play(player, "c", volume=0.6)
mc.audio.play(player, "e", volume=0.6)
mc.audio.play(player, "g", volume=0.6)

time.sleep(4)

# 清理
for note in ["c", "e", "g"]:
    mc.audio.unload(player, note)
```

---

### 示例 6：距离衰减效果

```python
from mc import Minecraft
import time

mc = Minecraft()
player = mc.getOnlinePlayers()[0]["name"]
pos = mc.getPlayerPos(player)

mc.audio.generate_tone(player, "approach", 440, 10.0)
time.sleep(1.0)

# 从远处开始
start_dist = 50
end_dist = 3

mc.audio.play_3d(player, "approach", pos.x, pos.y, pos.z + start_dist, 1.0, 1.0)

mc.postToChat("声音靠近中...")

# 靠近
for i in range(30):
    dist = start_dist - (start_dist - end_dist) * (i / 30)
    mc._send(f"audio.position({player},approach,{pos.x},{pos.y},{pos.z + dist})")
    time.sleep(0.1)

mc.postToChat("最近点!")
time.sleep(1)

mc.postToChat("声音远去中...")

# 远去
for i in range(30):
    dist = end_dist + (start_dist - end_dist) * (i / 30)
    mc._send(f"audio.position({player},approach,{pos.x},{pos.y},{pos.z + dist})")
    time.sleep(0.1)

mc.audio.stop(player, "approach")
mc.audio.unload(player, "approach")
```

---

## ⚠️ 注意事项

1. **加载等待**：音频加载需要时间，大文件需要更长等待
   ```python
   mc.audio.generate_tone(player, "tone", 440, 5.0)
   time.sleep(1.0)  # 等待加载
   mc.audio.play(player, "tone")
   ```

2. **资源释放**：用完后记得 `unload`
   ```python
   mc.audio.stop(player, "sound")
   mc.audio.unload(player, "sound")
   ```

3. **音频 ID 唯一**：同一个 ID 会覆盖之前的音频

4. **WAV 格式**：只支持 PCM 格式，不支持压缩的 WAV

5. **target 参数**：
   - `"PlayerName"` - 指定玩家
   - `""` - 第一个玩家
   - `"@a"` - 所有玩家