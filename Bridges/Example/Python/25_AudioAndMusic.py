"""
MCAPIBridge Audio Feature Demo
Demonstrates all audio capabilities of the MCAPIBridge API.
"""

from mc import Minecraft
import time
import math
import os

# ============================================================
# SETUP
# ============================================================

mc = Minecraft()

players = mc.getOnlinePlayers()
if not players:
    print("No players online!")
    exit()

player = players[0]["name"]

# WAV file path (change to your file)
WAV_FILE = "music.wav"

def chat(msg):
    mc.postToChat(msg)
    time.sleep(0.1)

def section(title):
    chat("§a" + "=" * 35)
    chat(f"§b§l{title}")
    chat("§a" + "=" * 35)
    time.sleep(1)

# ============================================================
# START DEMO
# ============================================================

section("Audio Feature Demo")
chat(f"§ePlayer: §f{player}")
time.sleep(2)

# ============================================================
# 1. BASIC PLAYBACK
# ============================================================

section("1. Basic Playback")

chat("§7Generating 440Hz tone (A4)...")
mc.audio.generateTone(player, "beep", 440, 2.0)
time.sleep(1.0)

chat("§7Playing...")
mc.audio.play(player, "beep")
time.sleep(2.5)

mc.audio.unload(player, "beep")

# ============================================================
# 2. VOLUME CONTROL
# ============================================================

section("2. Volume Control")

mc.audio.generateTone(player, "volTest", 440, 8.0)
time.sleep(1.0)

chat("§7Fade: 100% → 10% → 100%")
mc.audio.play(player, "volTest")

for v in [1.0, 0.7, 0.4, 0.1, 0.4, 0.7, 1.0]:
    mc.audio.setVolume(player, "volTest", v)
    chat(f"§e  {int(v * 100)}%")
    time.sleep(0.7)

mc.audio.stop(player, "volTest")
mc.audio.unload(player, "volTest")

# ============================================================
# 3. LOOP PLAYBACK
# ============================================================

section("3. Loop Playback")

mc.audio.generateTone(player, "loopTest", 330, 0.5)
time.sleep(0.8)

chat("§7Looping for 3 seconds...")
mc.audio.play(player, "loopTest", volume=0.6, loop=True)
time.sleep(3.0)

chat("§cStopping...")
mc.audio.stop(player, "loopTest")
mc.audio.unload(player, "loopTest")

# ============================================================
# 4. CUSTOM WAV FILE
# ============================================================

section("4. Custom WAV File")

if os.path.exists(WAV_FILE):
    chat(f"§7Loading: §f{WAV_FILE}")
    mc.audio.loadWav(player, "music", WAV_FILE)
    time.sleep(2.0)
    
    chat("§7Playing WAV file...")
    mc.audio.play(player, "music", volume=0.8)
    time.sleep(5.0)
    
    chat("§7Adjusting volume...")
    for v in [0.8, 0.5, 0.3, 0.5, 0.8]:
        mc.audio.setVolume(player, "music", v)
        chat(f"§e  {int(v * 100)}%")
        time.sleep(1.0)
    
    mc.audio.stop(player, "music")
    mc.audio.unload(player, "music")
else:
    chat(f"§c'{WAV_FILE}' not found!")
    chat("§7Place a .wav file in script directory")

time.sleep(1)

# ============================================================
# 5. WAV 3D SPATIAL
# ============================================================

section("5. WAV 3D Spatial")

if os.path.exists(WAV_FILE):
    pos = mc.getPlayerPos(player)
    
    chat("§7Loading WAV for 3D...")
    mc.audio.loadWav(player, "music3d", WAV_FILE)
    time.sleep(2.0)
    
    chat("§7Playing in front of you...")
    chat("§d§lTurn around to feel it!")
    front = pos.forward(10)
    mc.audio.play3d(player, "music3d", front.x, front.y, front.z, 1.0, 0.5)
    time.sleep(5.0)
    
    mc.audio.stop(player, "music3d")
    mc.audio.unload(player, "music3d")
else:
    chat("§7Skipping (no WAV file)")

time.sleep(1)

# ============================================================
# 6. WAV SURROUND
# ============================================================

section("6. WAV Surround")

if os.path.exists(WAV_FILE):
    pos = mc.getPlayerPos(player)
    
    chat("§7Loading WAV for surround...")
    mc.audio.loadWav(player, "surroundWav", WAV_FILE)
    time.sleep(2.0)
    
    chat("§d§lSound circling around you!")
    mc.audio.play3d(player, "surroundWav", pos.x + 10, pos.y, pos.z, 1.0, 0.3)
    
    for i in range(40):
        angle = i * 0.2
        x = pos.x + 10 * math.cos(angle)
        z = pos.z + 10 * math.sin(angle)
        mc.audio.setPosition(player, "surroundWav", x, pos.y, z)
        time.sleep(0.15)
    
    mc.audio.stop(player, "surroundWav")
    mc.audio.unload(player, "surroundWav")
else:
    chat("§7Skipping (no WAV file)")

time.sleep(1)

# ============================================================
# 7. 3D SPATIAL (TONE)
# ============================================================

section("7. 3D Spatial (Tone)")

pos = mc.getPlayerPos(player)

mc.audio.generateTone(player, "frontTone", 500, 4.0)
time.sleep(1.0)

chat("§7Playing 10 blocks ahead...")
chat("§d§lTurn around to feel it!")

front = pos.forward(10)
mc.audio.play3d(player, "frontTone", front.x, front.y, front.z, 1.0, 1.0)
time.sleep(4.5)

mc.audio.unload(player, "frontTone")

# ============================================================
# 8. LEFT/RIGHT CHANNELS
# ============================================================

section("8. Left/Right Channels")

pos = mc.getPlayerPos(player)
yawRad = math.radians(pos.yaw)

mc.audio.generateTone(player, "leftTone", 400, 2.0)
mc.audio.generateTone(player, "rightTone", 600, 2.0)
time.sleep(1.0)

leftX = pos.x + 8 * math.sin(yawRad + math.pi/2)
leftZ = pos.z - 8 * math.cos(yawRad + math.pi/2)

rightX = pos.x + 8 * math.sin(yawRad - math.pi/2)
rightZ = pos.z - 8 * math.cos(yawRad - math.pi/2)

chat("§b← Left (400Hz)")
mc.audio.play3d(player, "leftTone", leftX, pos.y, leftZ, 1.0, 0.5)
time.sleep(1.5)

chat("§c→ Right (600Hz)")
mc.audio.play3d(player, "rightTone", rightX, pos.y, rightZ, 1.0, 0.5)
time.sleep(2.5)

mc.audio.unload(player, "leftTone")
mc.audio.unload(player, "rightTone")

# ============================================================
# 9. SURROUND SOUND (TONE)
# ============================================================

section("9. Surround Sound (Tone)")

pos = mc.getPlayerPos(player)

mc.audio.generateTone(player, "surroundTone", 500, 12.0)
time.sleep(1.5)

chat("§d§lSound circling around you!")
mc.audio.play3d(player, "surroundTone", pos.x + 10, pos.y, pos.z, 1.0, 0.3)

radius = 10
for i in range(50):
    angle = i * 0.15
    x = pos.x + radius * math.cos(angle)
    z = pos.z + radius * math.sin(angle)
    mc.audio.setPosition(player, "surroundTone", x, pos.y, z)
    time.sleep(0.1)

mc.audio.stop(player, "surroundTone")
mc.audio.unload(player, "surroundTone")

# ============================================================
# 10. DISTANCE ATTENUATION
# ============================================================

section("10. Distance Attenuation")

pos = mc.getPlayerPos(player)

mc.audio.generateTone(player, "distTone", 440, 10.0)
time.sleep(1.0)

mc.audio.play3d(player, "distTone", pos.x, pos.y, pos.z + 40, 1.0, 1.0)

chat("§a▶ Approaching...")
for i in range(20):
    dist = 40 - 37 * (i / 20)
    mc.audio.setPosition(player, "distTone", pos.x, pos.y, pos.z + dist)
    time.sleep(0.1)

chat("§e§l★ Closest!")
time.sleep(1.0)

chat("§c◀ Moving away...")
for i in range(20):
    dist = 3 + 37 * (i / 20)
    mc.audio.setPosition(player, "distTone", pos.x, pos.y, pos.z + dist)
    time.sleep(0.1)

mc.audio.stop(player, "distTone")
mc.audio.unload(player, "distTone")

# ============================================================
# 11. CHORD (MULTI-SOURCE)
# ============================================================

section("11. Chord (Multi-Source)")

mc.audio.generateTone(player, "noteC", 262, 3.0)
mc.audio.generateTone(player, "noteE", 330, 3.0)
mc.audio.generateTone(player, "noteG", 392, 3.0)
time.sleep(1.5)

chat("§eC Major Chord!")

mc.audio.play(player, "noteC", volume=0.6)
mc.audio.play(player, "noteE", volume=0.6)
mc.audio.play(player, "noteG", volume=0.6)
time.sleep(3.5)

for n in ["noteC", "noteE", "noteG"]:
    mc.audio.unload(player, n)

# ============================================================
# DEMO COMPLETE
# ============================================================

section("Demo Complete!")

chat("§7Features demonstrated:")
chat("§f  1. Basic playback")
chat("§f  2. Volume control")
chat("§f  3. Loop playback")
chat("§f  4. Custom WAV file")
chat("§f  5. WAV 3D spatial")
chat("§f  6. WAV surround")
chat("§f  7. 3D spatial (tone)")
chat("§f  8. Left/Right channels")
chat("§f  9. Surround sound")
chat("§f 10. Distance attenuation")
chat("§f 11. Multi-source chord")
chat("§a" + "=" * 35)
