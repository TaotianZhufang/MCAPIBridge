from mc import Minecraft
import time

mc = Minecraft()
mc.postToChat("§fKeyBoardHits On")


while True:
    hits = mc.pollBlockHits()
    
    for hit in hits:

        if hit.action >= 101 and hit.action <= 105:
            key_num = hit.action - 100
            player_name = mc.getOnlinePlayers()[0]['name']
            
            msg = f"§b{player_name} §fpressed §6key {key_num}"
            print(msg)
            mc.postToChat(msg)
            
            pos = mc.getPlayerPos()
            mc.spawnParticle(pos.x, pos.y+2, pos.z, "note", count=1)

    time.sleep(0.05)
