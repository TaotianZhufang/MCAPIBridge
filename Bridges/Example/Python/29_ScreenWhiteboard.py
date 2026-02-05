# You should install all the extra packages for example 26-29!!!
# pip install opencv-python numpy pillow imageio-ffmpeg pydub

from mc import Minecraft
import cv2
import numpy as np
import base64
import time

# Config
SCREEN_ID = 1
W, H = 1280, 720 # Canvas size

mc = Minecraft()

# Create white canvas
canvas = np.ones((H, W, 3), dtype=np.uint8) * 255
drawing = False
last_point = (-1, -1)

# Mouse callback function
def draw_event(event, x, y, flags, param):
    global drawing, last_point, canvas
    
    if event == cv2.EVENT_LBUTTONDOWN:
        drawing = True
        last_point = (x, y)
    elif event == cv2.EVENT_MOUSEMOVE:
        if drawing:
            # Draw line (Black, thickness 2)
            cv2.line(canvas, last_point, (x, y), (0, 0, 0), 2)
            last_point = (x, y)
    elif event == cv2.EVENT_LBUTTONUP:
        drawing = False

# Init Window
cv2.namedWindow("MC Whiteboard")
cv2.setMouseCallback("MC Whiteboard", draw_event)

print("🎨 Whiteboard started! Draw in the window.")
print("Press 'C' to clear, 'Q' to quit.")

try:
    while True:
        # Show local window
        cv2.imshow("MC Whiteboard", canvas)
        
        # Encode & Send (JPG Quality 50 for low latency)
        _, buf = cv2.imencode('.jpg', canvas, [cv2.IMWRITE_JPEG_QUALITY, 50])
        b64 = base64.b64encode(buf).decode()
        mc.updateScreen(SCREEN_ID, b64)
        
        key = cv2.waitKey(33) & 0xFF # ~30 FPS
        if key == ord('q'):
            break
        elif key == ord('c'):
            canvas[:] = 255 # Clear to white

except KeyboardInterrupt:
    pass
finally:
    cv2.destroyAllWindows()
