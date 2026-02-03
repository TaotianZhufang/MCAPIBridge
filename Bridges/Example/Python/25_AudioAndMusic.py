from mc import Minecraft
import time
import math
import os

# ============================================================
# SETUP
# ============================================================

# Connect to Minecraft server
mc = Minecraft()

# Get first online player
players = mc.getOnlinePlayers()
if not players:
    print("No players online!")
    exit()

player = players[0]["name"]

# WAV file path (change to your file)
WAV_FILE = "music.wav"

def chat(msg):
    """Send message to game chat"""
    mc.postToChat(msg)
    time.sleep(0.1)

def section(title):
    """Display section header in chat"""
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
# 1. BASIC TONE PLAYBACK
# ============================================================

section("1. Basic Playback")

# generate_tone(target, audio_id, frequency, duration, sample_rate)
# - target: player name, "" for first player, "@a" for all
# - audio_id: unique identifier for this audio
# - frequency: tone frequency in Hz (default: 440)
# - duration: length in seconds (default: 1.0)
# - sample_rate: audio sample rate (default: 44100)

chat("§7Generating 440Hz tone (A4)...")
mc.audio.generate_tone(player, "beep", 440, 2.0)
time.sleep(1.0)  # Wait for audio to load

# play(target, audio_id, volume, loop)
# - volume: 0.0 to 1.0 (default: 1.0)
# - loop: True/False (default: False)

chat("§7Playing...")
mc.audio.play(player, "beep")
time.sleep(2.5)

# unload(target, audio_id)
# Releases audio from memory - always call when done

mc.audio.unload(player, "beep")

# ============================================================
# 2. VOLUME CONTROL
# ============================================================

section("2. Volume Control")

mc.audio.generate_tone(player, "vol", 440, 8.0)
time.sleep(1.0)

chat("§7Fade: 100% → 10% → 100%")
mc.audio.play(player, "vol")

# set_volume(target, audio_id, volume)
# Dynamically adjust volume during playback

for v in [1.0, 0.7, 0.4, 0.1, 0.4, 0.7, 1.0]:
    mc.audio.set_volume(player, "vol", v)
    chat(f"§e  {int(v * 100)}%")
    time.sleep(0.7)

# stop(target, audio_id)
# Stop playback without unloading

mc.audio.stop(player, "vol")
mc.audio.unload(player, "vol")

# ============================================================
# 3. LOOP PLAYBACK
# ============================================================

section("3. Loop Playback")

# Short audio that will loop
mc.audio.generate_tone(player, "loop", 330, 0.5)
time.sleep(0.8)

# Enable looping with loop=True
chat("§7Looping for 3 seconds...")
mc.audio.play(player, "loop", volume=0.6, loop=True)
time.sleep(3.0)

chat("§cStopping...")
mc.audio.stop(player, "loop")
mc.audio.unload(player, "loop")

# ============================================================
# 4. CUSTOM WAV FILE PLAYBACK
# ============================================================

section("4. Custom WAV File")

# load_wav(target, audio_id, filepath)
# Supported formats:
# - 8-bit or 16-bit PCM
# - Mono or Stereo (auto-converts to mono)
# - Any sample rate

if os.path.exists(WAV_FILE):
    chat(f"§7Loading: §f{WAV_FILE}")
    mc.audio.load_wav(player, "music", WAV_FILE)
    time.sleep(2.0)  # Wait for large files to load
    
    chat("§7Playing WAV file...")
    mc.audio.play(player, "music", volume=0.8)
    time.sleep(5.0)
    
    # Volume control works with WAV too
    chat("§7Adjusting volume...")
    for v in [0.8, 0.5, 0.3, 0.5, 0.8]:
        mc.audio.set_volume(player, "music", v)
        chat(f"§e  {int(v * 100)}%")
        time.sleep(1.0)
    
    mc.audio.stop(player, "music")
    mc.audio.unload(player, "music")
else:
    chat(f"§c'{WAV_FILE}' not found!")
    chat("§7Place a .wav file in script directory")

time.sleep(1)

# ============================================================
# 5. WAV 3D SPATIAL PLAYBACK
# ============================================================

section("5. WAV 3D Spatial")

if os.path.exists(WAV_FILE):
    pos = mc.getPlayerPos(player)
    
    chat("§7Loading WAV for 3D...")
    mc.audio.load_wav(player, "music3d", WAV_FILE)
    time.sleep(2.0)
    
    # play_3d(target, audio_id, x, y, z, volume, rolloff)
    # - x, y, z: world coordinates of sound source
    # - rolloff: distance attenuation factor
    #   - 0.5 = slow falloff (audible from far)
    #   - 1.0 = normal falloff
    #   - 2.0 = fast falloff (only audible nearby)
    
    chat("§7Playing in front of you...")
    chat("§d§lTurn around to feel it!")
    front = pos.forward(10)
    mc.audio.play_3d(player, "music3d", front.x, front.y, front.z, 1.0, 0.5)
    time.sleep(5.0)
    
    mc.audio.stop(player, "music3d")
    mc.audio.unload(player, "music3d")
else:
    chat("§7Skipping (no WAV file)")

time.sleep(1)

# ============================================================
# 6. WAV SURROUND PLAYBACK
# ============================================================

section("6. WAV Surround")

if os.path.exists(WAV_FILE):
    pos = mc.getPlayerPos(player)
    
    chat("§7Loading WAV for surround...")
    mc.audio.load_wav(player, "surround_wav", WAV_FILE)
    time.sleep(2.0)
    
    chat("§d§lSound circling around you!")
    mc.audio.play_3d(player, "surround_wav", pos.x + 10, pos.y, pos.z, 1.0, 0.3)
    
    # Update position to create surround effect
    # Use mc._send() for audio.position command
    
    for i in range(40):
        angle = i * 0.2
        x = pos.x + 10 * math.cos(angle)
        z = pos.z + 10 * math.sin(angle)
        mc._send(f"audio.position({player},surround_wav,{x},{pos.y},{z})")
        time.sleep(0.15)
    
    mc.audio.stop(player, "surround_wav")
    mc.audio.unload(player, "surround_wav")
else:
    chat("§7Skipping (no WAV file)")

time.sleep(1)

# ============================================================
# 7. 3D SPATIAL AUDIO (GENERATED TONE)
# ============================================================

section("7. 3D Spatial (Tone)")

pos = mc.getPlayerPos(player)

mc.audio.generate_tone(player, "front", 500, 4.0)
time.sleep(1.0)

chat("§7Playing 10 blocks ahead...")
chat("§d§lTurn around to feel it!")

# pos.forward(distance) returns position ahead of player
front = pos.forward(10)
mc.audio.play_3d(player, "front", front.x, front.y, front.z, 1.0, 1.0)
time.sleep(4.5)

mc.audio.unload(player, "front")

# ============================================================
# 8. LEFT/RIGHT CHANNEL TEST
# ============================================================

section("8. Left/Right Channels")

pos = mc.getPlayerPos(player)
yaw_rad = math.radians(pos.yaw)

# Generate two different tones
mc.audio.generate_tone(player, "left", 400, 2.0)   # Low tone
mc.audio.generate_tone(player, "right", 600, 2.0)  # High tone
time.sleep(1.0)

# Calculate left and right positions based on player facing
left_x = pos.x + 8 * math.sin(yaw_rad + math.pi/2)
left_z = pos.z - 8 * math.cos(yaw_rad + math.pi/2)

right_x = pos.x + 8 * math.sin(yaw_rad - math.pi/2)
right_z = pos.z - 8 * math.cos(yaw_rad - math.pi/2)

chat("§b← Left (400Hz)")
mc.audio.play_3d(player, "left", left_x, pos.y, left_z, 1.0, 0.5)
time.sleep(1.5)

chat("§c→ Right (600Hz)")
mc.audio.play_3d(player, "right", right_x, pos.y, right_z, 1.0, 0.5)
time.sleep(2.5)

mc.audio.unload(player, "left")
mc.audio.unload(player, "right")

# ============================================================
# 9. SURROUND SOUND (GENERATED TONE)
# ============================================================

section("9. Surround Sound (Tone)")

pos = mc.getPlayerPos(player)

# Long duration for circling effect
mc.audio.generate_tone(player, "surround", 500, 12.0)
time.sleep(1.5)

chat("§d§lSound circling around you!")
mc.audio.play_3d(player, "surround", pos.x + 10, pos.y, pos.z, 1.0, 0.3)

# Move sound source in a circle
radius = 10
for i in range(50):
    angle = i * 0.15
    x = pos.x + radius * math.cos(angle)
    z = pos.z + radius * math.sin(angle)
    mc._send(f"audio.position({player},surround,{x},{pos.y},{z})")
    time.sleep(0.1)

mc.audio.stop(player, "surround")
mc.audio.unload(player, "surround")

# ============================================================
# 10. DISTANCE ATTENUATION
# ============================================================

section("10. Distance Attenuation")

pos = mc.getPlayerPos(player)

mc.audio.generate_tone(player, "dist", 440, 10.0)
time.sleep(1.0)

# Start far away
mc.audio.play_3d(player, "dist", pos.x, pos.y, pos.z + 40, 1.0, 1.0)

# Move closer
chat("§a▶ Approaching...")
for i in range(20):
    dist = 40 - 37 * (i / 20)  # 40 -> 3
    mc._send(f"audio.position({player},dist,{pos.x},{pos.y},{pos.z + dist})")
    time.sleep(0.1)

chat("§e§l★ Closest!")
time.sleep(1.0)

# Move away
chat("§c◀ Moving away...")
for i in range(20):
    dist = 3 + 37 * (i / 20)  # 3 -> 40
    mc._send(f"audio.position({player},dist,{pos.x},{pos.y},{pos.z + dist})")
    time.sleep(0.1)

mc.audio.stop(player, "dist")
mc.audio.unload(player, "dist")

# ============================================================
# 11. MULTI-SOURCE CHORD
# ============================================================

section("11. Chord (Multi-Source)")

# C Major chord: C4, E4, G4
mc.audio.generate_tone(player, "c", 262, 3.0)  # C4
mc.audio.generate_tone(player, "e", 330, 3.0)  # E4
mc.audio.generate_tone(player, "g", 392, 3.0)  # G4
time.sleep(1.5)

chat("§eC Major Chord!")

# Play all three simultaneously
mc.audio.play(player, "c", volume=0.6)
mc.audio.play(player, "e", volume=0.6)
mc.audio.play(player, "g", volume=0.6)
time.sleep(3.5)

# Cleanup all
for n in ["c", "e", "g"]:
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
