from mc import Minecraft
import time

mc = Minecraft()

me = mc.getOnlinePlayers()[0]['id']

items = mc.getInventory(me)

for item in items:
    mc.postToChat(f"Slot {item['slot']}: {item['id']} x{item['count']}")
