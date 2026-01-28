from mc import Minecraft
import time

mc = Minecraft()

pos = mc.getPlayerPos()
x, y, z = int(pos.x + 1), int(pos.y), int(pos.z)

mc.setBlock(x, y, z, "oak_sign")

mc.setSign(x, y, z, "§cMCAPIBridge", "Python", "Libary", "Exsample")
