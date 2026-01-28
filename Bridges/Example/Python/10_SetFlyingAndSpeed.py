from mc import Minecraft
import time

mc = Minecraft()

me = mc.getOnlinePlayers()[0]['id']

mc.setFlying(me, True, False)
mc.postToChat("§bFly mode on!")

mc.setFlySpeed(me, 0.2)# Default 0.05
