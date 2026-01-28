from mc import Minecraft
import time

mc = Minecraft()

pos = mc.getPlayerPos()

mc.teleport(pos.x, pos.y + 50, pos.z)
mc.postToChat("§eBig Jump!")