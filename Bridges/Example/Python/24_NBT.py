from mc import Minecraft
import time

mc = Minecraft()
pos = mc.getPlayerPos()

print("Spawning Boss...")
zombie_id = mc.spawnEntity(pos.x + 5, pos.y, pos.z, "zombie")

# Modify NBT to make it a BOSS
# In 1.20.6, scale is an Attribute, not a simple tag.
# We also set it to be huge,glowing, silent, and have 1000 health.
nbt_data = """{Glowing: 1b,Silent: 1b,Attributes:[{Name:"minecraft:generic.scale", Base:30.0d},{Name:"minecraft:generic.max_health", Base:1000.0d}],Health: 1000.0f}"""
print(nbt_data)
mc.setEntityNbt(zombie_id, nbt_data)

mc.postToChat("§c§lWARNING: A Giant Zombie has appeared!")
