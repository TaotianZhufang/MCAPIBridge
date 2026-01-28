from mc import Minecraft
import time

mc = Minecraft()

pos = mc.getPlayerPos()

pig_id = mc.spawnEntity(pos.x + 5, pos.y, pos.z, "pig")

time.sleep(1)
mc.teleportEntity(pig_id, pos.x, pos.y, pos.z)
mc.postToChat("Pig here.")
