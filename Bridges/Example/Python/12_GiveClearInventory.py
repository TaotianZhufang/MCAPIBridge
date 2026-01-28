from mc import Minecraft
import time

mc = Minecraft()

me = mc.getOnlinePlayers()[0]['id']

mc.clearInventory(me)
mc.postToChat("Clear all.")

mc.give(me, "iron_sword", 1)
mc.give(me, "bread", 16)
mc.give(me, "torch", 32)

