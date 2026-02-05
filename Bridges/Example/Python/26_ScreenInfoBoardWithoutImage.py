# You should install all the extra packages for example 26-29!!!
# pip install opencv-python numpy pillow imageio-ffmpeg pydub

from mc import Minecraft
import cv2
import numpy as np
import base64
import time
from PIL import Image, ImageDraw, ImageFont

# Config
SCREEN_ID = 1
WIDTH, HEIGHT = 960, 540  # Screen resolution

mc = Minecraft()
if not mc.getOnlinePlayers(): exit()

print(f"📡 Pushing info board to Screen ID {SCREEN_ID}...")

try:
    while True:
        start = time.time()

        # 1. Create dark background (RGB)
        img_pil = Image.new('RGB', (WIDTH, HEIGHT), color=(20, 20, 30))
        draw = ImageDraw.Draw(img_pil)

        # 2. Draw Graphics
        # Outer border
        draw.rectangle([10, 10, WIDTH-10, HEIGHT-10], outline=(0, 255, 255), width=3)
        # Title bar background
        draw.rectangle([10, 10, WIDTH-10, 60], fill=(0, 100, 100))

        # 3. Draw Text
        # Load fonts (fallback to default if not found)
        try:
            font_title = ImageFont.truetype("arial.ttf", 40)
            font_time = ImageFont.truetype("arial.ttf", 80)
            font_info = ImageFont.truetype("arial.ttf", 20)
        except:
            font_title = ImageFont.load_default()
            font_time = ImageFont.load_default()
            font_info = ImageFont.load_default()

        # Title
        draw.text((WIDTH//2, 35), "SERVER NOTICE", font=font_title, fill="white", anchor="mm")

        # Time
        curr_time = time.strftime("%H:%M:%S")
        draw.text((WIDTH//2, HEIGHT//2), curr_time, font=font_time, fill=(0, 255, 0), anchor="mm")

        # Footer message
        draw.text((WIDTH//2, HEIGHT-40), "Welcome to MCAPIBridge Demo", font=font_info, fill="yellow", anchor="mm")

        # 4. Convert PIL (RGB) -> OpenCV (BGR)
        frame = cv2.cvtColor(np.array(img_pil), cv2.COLOR_RGB2BGR)

        # 5. Encode & Send (PNG Compression 1 for speed)
        _, buffer = cv2.imencode('.png', frame, [cv2.IMWRITE_PNG_COMPRESSION, 1])
        b64 = base64.b64encode(buffer).decode()
        mc.updateScreen(SCREEN_ID, b64)

        # 6. FPS Control (1 FPS is enough for clock)
        time.sleep(1.0 - (time.time() - start))

except KeyboardInterrupt:
    print("Stopped.")
