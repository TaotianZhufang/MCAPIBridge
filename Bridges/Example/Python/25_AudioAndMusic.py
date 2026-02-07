"""
MCAPIBridge Audio Feature Demo
Demonstrates all audio capabilities of the MCAPIBridge API.
Updated for Alpha-0.4.1 (Clone & Dimension Support)
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

# Reset audio system first
mc.audio.reset()
time.sleep(0.5)

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
    mc.audio.play3d(player, "music3d", front.x, front.y, front.z, 1.0, 0.5,dimension=player)
    time.sleep(5.0)
    
    mc.audio.stop(player, "music3d")
    mc.audio.unload(player, "music3d")
else:
    chat("§7Skipping (no WAV file)")

time.sleep(1)

# ============================================================
# 6. WAV SURROUND (CLONING DEMO)
# ============================================================

section("6. WAV Surround (Clone)")

if os.path.exists(WAV_FILE):
    pos = mc.getPlayerPos(player)
    
    chat("§7Loading Master Audio...")
    mc.audio.loadWav(player, "surroundMaster", WAV_FILE)
    time.sleep(2.0)
    
    chat("§d§lSound circling (using Clones)!")
    
    # Clone it to use as a moving source
    mc.audio.clone(player, "surroundMaster", "surroundMover")
    
    mc.audio.play3d(player, "surroundMover", pos.x + 10, pos.y, pos.z, 1.0, 0.3,dimension=player)
    
    for i in range(40):
        angle = i * 0.2
        x = pos.x + 10 * math.cos(angle)
        z = pos.z + 10 * math.sin(angle)
        mc.audio.setPosition(player, "surroundMover", x, pos.y, z)
        time.sleep(0.15)
    
    mc.audio.stop(player, "surroundMover")
    mc.audio.unload(player, "surroundMover") # Unload clone
    mc.audio.unload(player, "surroundMaster") # Unload master
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
mc.audio.play3d(player, "frontTone", front.x, front.y, front.z, 1.0, 1.0,dimension=player)
time.sleep(4.5)

mc.audio.unload(player, "frontTone")

# ============================================================
# 8. LEFT/RIGHT CHANNELS (CLONE OPTIMIZATION)
# ============================================================

section("8. Left/Right Channels")

pos = mc.getPlayerPos(player)
yawRad = math.radians(pos.yaw)

# Generate ONCE, Clone TWICE
mc.audio.generateTone(player, "tone400", 400, 2.0)
mc.audio.generateTone(player, "tone600", 600, 2.0)
time.sleep(1.0)

# Clone for L/R
mc.audio.clone(player, "tone400", "leftTone")
mc.audio.clone(player, "tone600", "rightTone")

leftX = pos.x + 8 * math.sin(yawRad + math.pi/2)
leftZ = pos.z - 8 * math.cos(yawRad + math.pi/2)

rightX = pos.x + 8 * math.sin(yawRad - math.pi/2)
rightZ = pos.z - 8 * math.cos(yawRad - math.pi/2)

chat("§b← Left (400Hz)")
mc.audio.play3d(player, "leftTone", leftX, pos.y, leftZ, 1.0, 0.5,dimension=player)
time.sleep(1.5)

chat("§c→ Right (600Hz)")
mc.audio.play3d(player, "rightTone", rightX, pos.y, rightZ, 1.0, 0.5,dimension=player)
time.sleep(2.5)

# Unload clones and masters
mc.audio.unload(player, "leftTone")
mc.audio.unload(player, "rightTone")
mc.audio.unload(player, "tone400")
mc.audio.unload(player, "tone600")

# ============================================================
# 9. DISTANCE ATTENUATION
# ============================================================

section("9. Distance Attenuation")

pos = mc.getPlayerPos(player)

mc.audio.generateTone(player, "distTone", 440, 10.0)
time.sleep(1.0)

mc.audio.play3d(player, "distTone", pos.x, pos.y, pos.z + 40, 1.0, 1.0,dimension=player)

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
# 10. CHORD (MULTI-SOURCE)
# ============================================================

section("10. Chord (Multi-Source)")

# Generate single notes
mc.audio.generateTone(player, "noteC", 262, 3.0)
mc.audio.generateTone(player, "noteE", 330, 3.0)
mc.audio.generateTone(player, "noteG", 392, 3.0)
time.sleep(1.5)

chat("§eC Major Chord!")

# Play simultaneously
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
chat("§f  6. WAV surround (Clone)")
chat("§f  7. 3D spatial (tone)")
chat("§f  8. Left/Right channels (Clone)")
chat("§f  9. Distance attenuation")
chat("§f 10. Multi-source chord")
chat("§a" + "=" * 35)
