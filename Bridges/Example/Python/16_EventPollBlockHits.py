from mc import Minecraft
import time

mc = Minecraft()

mc.postToChat("Left or right click with item held.")

while True:
    hits = mc.pollBlockHits()
    for hit in hits:
        if hit.action == 2:
            mc.postToChat(f"Right click at: {hit.pos}")
            mc.spawnEntity(hit.pos.x, hit.pos.y, hit.pos.z, "lightning_bolt")
        elif hit.action == 1:
            mc.postToChat(f"Left click at: {hit.pos}")
    
    time.sleep(0.1)
