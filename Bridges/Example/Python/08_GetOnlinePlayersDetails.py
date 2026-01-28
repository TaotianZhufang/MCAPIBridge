from mc import Minecraft
import time

mc = Minecraft()

players = mc.getOnlinePlayers()
if players:
    target_id = players[0]['id'] # Get ID of first player
    
    details = mc.getPlayerDetails(target_id)
    
    mc.postToChat(f"Player: {details['name']}")
    mc.postToChat(f"Mode: {details['mode']}")
    mc.postToChat(f"Health: {details['health']} / {details['max_health']}")
    mc.postToChat(f"Held: {details['held_item']}")
