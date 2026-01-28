from mc import Minecraft
import time

mc = Minecraft()

pos = mc.getPlayerPos()

creeper_id = mc.spawnEntity(pos.x + 3, pos.y, pos.z, "creeper")
print(f"Spawn a creeper,ID: {creeper_id}")

for i in range(5):
    mc.spawnParticle(pos.x + 3, pos.y + 2, pos.z, "heart", count=5)
    time.sleep(0.5)
