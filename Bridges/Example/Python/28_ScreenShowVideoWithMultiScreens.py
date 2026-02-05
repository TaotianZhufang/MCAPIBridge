# You should install all the extra packages for example 26-29!!!
# pip install opencv-python numpy pillow imageio-ffmpeg pydub

from mc import Minecraft
import cv2
import base64
import time
import os
import subprocess
import imageio_ffmpeg

# Config
SCREEN_ID = 1
VIDEO_FILE = "bad_apple.mp4"
AUDIO_BASE = "movie_snd"
TARGET_W, TARGET_H = 1920, 1080 # Lower resolution for better performance
#Only support fps <= 30 because of the TPS limit of Minecraft

mc = Minecraft()
if not mc.getOnlinePlayers(): exit()

# 1. Auto-detect screen locations
print(f"🔍 Searching for screens with ID {SCREEN_ID}...")
locations = mc.getScreenLocations(SCREEN_ID)

if not locations:
    print("❌ No screens found. Build one in-game first!")
    exit()

print(f"✅ Found {len(locations)} screen locations.")

# 2. Extract Audio
temp_wav = "temp_ex3.wav"
print("🎵 Extracting audio...")
subprocess.run([
    imageio_ffmpeg.get_ffmpeg_exe(), '-i', VIDEO_FILE, 
    '-vn', '-ac', '1', '-ar', '44100', '-acodec', 'pcm_s16le', 
    '-f', 'wav', '-y', temp_wav
], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

# 3. Setup Audio Sources
print("🔊 Setting up audio sources...")
audio_list = []

# Upload master audio
mc.audio.loadWav("@a", AUDIO_BASE, temp_wav)
time.sleep(1.0)
try: os.remove(temp_wav)
except: pass

# Create audio source for EACH screen location
for i, loc in enumerate(locations):
    aid = f"{AUDIO_BASE}_{i}"
    
    # Clone source (if supported) or reuse master
    if i == 0:
        curr_id = AUDIO_BASE
    else:
        curr_id = aid
        # Tell server to clone audio data (saves bandwidth)
        mc._send(f"audio.clone(@a,{AUDIO_BASE},{curr_id})")
        
    mc.audio.play3d("@a", curr_id, loc.x, loc.y, loc.z, 1.0, 1.0)
    audio_list.append(curr_id)

# 4. Video Loop
print("🎬 Playing...")
cap = cv2.VideoCapture(VIDEO_FILE)
fps = cap.get(cv2.CAP_PROP_FPS)
start_t = time.time()
idx = 0

try:
    while cap.isOpened():
        # Sync logic
        cur = time.time() - start_t
        tgt = idx / fps
        
        if cur < tgt: 
            time.sleep(tgt - cur)
        elif cur > tgt + 0.2: 
            # Skip frames if lagging
            cap.set(cv2.CAP_PROP_POS_FRAMES, int(cur * fps))
            idx = int(cur * fps)
            continue

        ret, frame = cap.read()
        if not ret: break
        
        # Resize
        frame = cv2.resize(frame, (TARGET_W, TARGET_H))
        
        # Encode (JPG , quality=80)
        _, buf = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 80])
        b64 = base64.b64encode(buf).decode()
        mc.updateScreen(SCREEN_ID, b64)
        idx += 1

except KeyboardInterrupt:
    pass
finally:
    cap.release()
    for aid in audio_list:
        mc.audio.stop("@a", aid)
        mc.audio.unload("@a", aid)
