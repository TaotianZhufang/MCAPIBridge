# You should install all the extra packages for example 26-29!!!
# pip install opencv-python numpy pillow imageio-ffmpeg pydub

from mc import Minecraft
import cv2
import base64
import numpy as np
import sys
import os

# Config
SCREEN_ID = 1
IMAGE_FILE = "photo.jpg" # Make sure this file exists
TARGET_W, TARGET_H = 1920, 1080

mc = Minecraft()

def resize_contain(image, width, height):
    """Resize image to fit within box, adding black borders"""
    h, w = image.shape[:2]
    scale = min(width / w, height / h)
    nw, nh = int(w * scale), int(h * scale)
    resized = cv2.resize(image, (nw, nh))
    
    canvas = np.zeros((height, width, 3), dtype=np.uint8)
    dx = (width - nw) // 2
    dy = (height - nh) // 2
    canvas[dy:dy+nh, dx:dx+nw] = resized
    return canvas

if not os.path.exists(IMAGE_FILE):
    print("❌ Image file not found!")
    sys.exit()

# Load
img = cv2.imread(IMAGE_FILE)

# Process
print("🖼️ Processing image...")
final_img = resize_contain(img, TARGET_W, TARGET_H)

# Encode (High quality JPG)
_, buffer = cv2.imencode('.jpg', final_img, [cv2.IMWRITE_JPEG_QUALITY, 90])
b64 = base64.b64encode(buffer).decode()

# Send
print("📤 Sending to screen...")
mc.updateScreen(SCREEN_ID, b64)
print("✅ Done.")
