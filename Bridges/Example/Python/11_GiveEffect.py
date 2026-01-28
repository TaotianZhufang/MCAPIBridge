from mc import Minecraft
import time

mc = Minecraft()

me = mc.getOnlinePlayers()[0]['id']

mc.giveEffect(me, "night_vision", 60, 1)

mc.giveEffect(me, "speed", 60, 5)
