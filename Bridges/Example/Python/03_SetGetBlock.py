from mc import Minecraft
import time

mc = Minecraft()

pos = mc.getPlayerPos()
x, y, z = int(pos.x + 2), int(pos.y), int(pos.z)

mc.setBlock(x, y, z, "diamond_block")
mc.postToChat(f"At {x},{y},{z} set a diamond block.")

time.sleep(2)

block_id = mc.getBlock(pos.x, pos.y-1, pos.z)
mc.postToChat(f"{block_id} is under player")

mc.setBlock(x, y, z, "air")
