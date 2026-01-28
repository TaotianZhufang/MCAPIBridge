from mc import Minecraft
import math
import time

mc = Minecraft()

def BuildDnaTower(radius=5, height=100):
    pos = mc.getPlayerPos()
    base_x, base_y, base_z = int(pos.x), int(pos.y), int(pos.z)
    
    mc.postToChat(f"§e[System] §fBuilding DNA Tower at {base_x}, {base_y}, {base_z}...")

    for y in range(height):
        angle = y * 0.2
        
        x1 = base_x + int(radius * math.cos(angle))
        z1 = base_z + int(radius * math.sin(angle))
        mc.setBlock(x1, base_y + y, z1, "lime_stained_glass")
        mc.spawnParticle(x1, base_y + y, z1, "composter", count=5)

        x2 = base_x + int(radius * math.cos(angle + math.pi))
        z2 = base_z + int(radius * math.sin(angle + math.pi))
        mc.setBlock(x2, base_y + y, z2, "pink_stained_glass")

        if y % 5 == 0:
            mc.setBlock(base_x, base_y + y, base_z, "end_rod")
            mc.setBlock(base_x, base_y + y + 1, base_z, "air")

        time.sleep(0.05)

    mc.postToChat("§a[System] §fConstruction Complete!")

if __name__ == "__main__":
    BuildDnaTower()
