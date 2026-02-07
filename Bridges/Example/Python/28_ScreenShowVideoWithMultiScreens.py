# You should install all the extra packages for example 26-29!!!
# pip install opencv-python numpy pillow imageio-ffmpeg pydub

from mc import Minecraft
import cv2
import base64
import time
import os
import subprocess
import imageio_ffmpeg

# ============================================================
# Config
# ============================================================
SCREEN_ID = 1
VIDEO_FILE = "bad_apple.mp4"
AUDIO_BASE = "movie_snd"
TARGET_W, TARGET_H = 1920, 1080 # 1080p requires good network & CPU

# ============================================================
# Connect & Setup
# ============================================================
mc = Minecraft()
if not mc.getOnlinePlayers(): 
    print("❌ 未连接")
    exit()

# 1. Reset Audio System (Crucial for state consistency)
print("🧹 Resetting audio system...")
mc.audio.reset()
time.sleep(0.5)

# 2. Auto-detect screen locations
print(f"🔍 Searching for screens with ID {SCREEN_ID}...")
locations = mc.getScreenLocations(SCREEN_ID)

if not locations:
    print("❌ No screens found. Build one in-game first!")
    exit()

print(f"✅ Found {len(locations)} screen locations.")
for loc in locations:
    print(f"   📍 {loc.x:.1f}, {loc.y:.1f}, {loc.z:.1f} @ {loc.dimension}")

# 3. Extract Audio
temp_wav = "temp_ex3.wav"
if not os.path.exists(VIDEO_FILE):
    print(f"❌ Video file not found: {VIDEO_FILE}")
    exit()

print("🎵 Extracting audio...")
try:
    subprocess.run([
        imageio_ffmpeg.get_ffmpeg_exe(), '-i', VIDEO_FILE, 
        '-vn', '-ac', '1', '-ar', '44100', '-acodec', 'pcm_s16le', 
        '-f', 'wav', '-y', temp_wav
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
except Exception as e:
    print(f"❌ FFmpeg error: {e}")
    exit()

# 4. Setup Audio Sources
print("🔊 Setting up audio sources...")
audio_list = []

# Upload master audio
print("📤 Uploading audio data...")
mc.audio.loadWav("@a", AUDIO_BASE, temp_wav)

# Wait for upload & cache (Large files need more time)
time.sleep(2.0) 
try: os.remove(temp_wav)
except: pass

# Create audio source for EACH screen location
for i, loc in enumerate(locations):
    aid = f"{AUDIO_BASE}_{i}"
    
    # Strategy: Use master for the first one, clones for others
    if i == 0:
        curr_id = AUDIO_BASE
    else:
        curr_id = aid
        # Clone audio source (Zero bandwidth)
        mc.audio.clone("@a", AUDIO_BASE, curr_id)
        
    # Play 3D audio with specific dimension
    # Offset defaults to 0.0
    mc.audio.play3d("@a", curr_id, loc.x, loc.y, loc.z, 
                    volume=1.0, rolloff=1.0, dimension=loc.dimension)
    
    audio_list.append(curr_id)

# 5. Video Loop
print(f"🎬 Playing at {TARGET_W}x{TARGET_H}...")
cap = cv2.VideoCapture(VIDEO_FILE)
fps = cap.get(cv2.CAP_PROP_FPS)
delay = 1.0 / fps
start_t = time.time()
idx = 0

try:
    while cap.isOpened():
        # Sync logic (Skip frames if lagging)
        cur = time.time() - start_t
        tgt = idx * delay
        
        if cur < tgt: 
            time.sleep(tgt - cur)
        elif cur > tgt + 0.2: 
            # Skip frames
            new_idx = int(cur * fps)
            cap.set(cv2.CAP_PROP_POS_FRAMES, new_idx)
            idx = new_idx
            continue

        ret, frame = cap.read()
        if not ret: break
        
        # Resize
        frame = cv2.resize(frame, (TARGET_W, TARGET_H))
        
        # Encode (JPG is mandatory for 1080p streaming)
        # Quality 80 offers good balance between visual and bandwidth
        _, buf = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 80])
        b64 = base64.b64encode(buf).decode()
        mc.updateScreen(SCREEN_ID, b64)
        
        # ★ Sync Progress (Fix audio drift for long videos)
        # Every ~1 second (30 frames), update server timestamp
        if idx % 30 == 0:
            current_sec = idx / fps
            for aid in audio_list:
                mc.audio.syncProgress(aid, current_sec)
        
        idx += 1

except KeyboardInterrupt:
    print("Stopped.")
finally:
    cap.release()
    # Stop clones
    for aid in audio_list:
        mc.audio.stop("@a", aid)
        mc.audio.unload("@a", aid)
    # Stop master if it wasn't in the list (e.g. single screen case)
    if AUDIO_BASE not in audio_list:
        mc.audio.stop("@a", AUDIO_BASE)
        mc.audio.unload("@a", AUDIO_BASE)
        
    print("Cleaned up.")
