from mc import Minecraft
import time

mc = Minecraft()

pos = mc.getPlayerPos()

entities = mc.getEntities(pos.x, pos.y, pos.z, radius=20)

mc.postToChat(f"Found {len(entities)} entities:")
for e in entities:
    mc.postToChat(f" - ID: {e['id']} | Type: {e['type']} | Position: {e['pos']}")
