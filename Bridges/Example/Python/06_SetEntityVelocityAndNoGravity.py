from mc import Minecraft
import time

mc = Minecraft()

pos = mc.getPlayerPos()
tnt_id = mc.spawnEntity(pos.x, pos.y + 2, pos.z, "tnt")

mc.setEntityNoGravity(tnt_id, True)
mc.postToChat("TNT")
time.sleep(2)

mc.postToChat("Shoot!")
direction = mc.getDirectionVector() 
speed = 2.0
mc.setEntityVelocity(tnt_id, direction.x * speed, direction.y * speed, direction.z * speed)
