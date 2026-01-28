from mc import Minecraft
import time

mc = Minecraft()

me = mc.getOnlinePlayers()[0]['id']

mc.setHealth(me, 1.0)
mc.postToChat("§cYou get hurt!")
time.sleep(2)

mc.setHealth(me, 40.0)
mc.setFood(me, 20)
mc.postToChat("§aSuper heal!")
