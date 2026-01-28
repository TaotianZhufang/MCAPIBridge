from mc import Minecraft
import time

mc = Minecraft()

def apply_class(player_id, class_name):
    mc.postToChat(f"§b[RPG] §fSwitching to class: §6{class_name.upper()}")
    
    mc.clearInventory(player_id)
    mc.setHealth(player_id, 20)
    mc.setFood(player_id, 20)
    mc.runCommand(f"effect clear @a") 
    print(class_name)
    if class_name == "tank":
        # Tank: High Health, Heavy Armor, Slow
        mc.setHealth(player_id, 40) # 20 Hearts
        mc.give(player_id, "netherite_sword", 1)
        mc.give(player_id, "netherite_chestplate", 1)
        mc.give(player_id, "shield", 1)
        mc.giveEffect(player_id, "slowness", 9999, 1)
        mc.giveEffect(player_id, "resistance", 9999, 1)

    elif class_name == "archer":
        # Archer: Speed, Bow, Leather Armor
        mc.setHealth(player_id, 16) # 8 Hearts
        mc.give(player_id, "bow", 1)
        mc.give(player_id, "arrow", 64)
        mc.give(player_id, "leather_boots", 1)
        mc.giveEffect(player_id, "speed", 9999, 2)
        mc.giveEffect(player_id, "jump_boost", 9999, 1)

    elif class_name == "mage":
        # Mage: Low Health, Potions, Fire/Magic items
        mc.setHealth(player_id, 10) # 5 Hearts
        mc.give(player_id, "blaze_rod", 1) # Wand
        mc.give(player_id, "ender_pearl", 16)
        mc.give(player_id, "splash_potion", 5) # Generic potion
        mc.giveEffect(player_id, "invisibility", 9999, 0)
        mc.giveEffect(player_id, "night_vision", 9999, 0)
        # Enable flight for Mage
        mc.setFlying(player_id, True, True)

print("RPG System Started. Type 'tank', 'archer', or 'mage' in game chat.")
mc.postToChat("§e[RPG] §fSystem Ready. Type §atank§f, §aarcher§f, or §amage§f in chat.")

while True:
    chats = mc.pollChatPosts()
    
    for chat in chats:
        msg = chat.message.lower()
        player_name = chat.name
        print(msg)
        player_id = None
        online_players = mc.getOnlinePlayers()
        for p in online_players:
            if p['name'] == player_name:
                player_id = p['id']
                break
        
        if not player_id:
            print(f"Can't find {player_name} ID.")
            continue

        if "tank" in msg:
            apply_class(player_id, "tank")
        elif "archer" in msg:
            apply_class(player_id, "archer")
        elif "mage" in msg:
            apply_class(player_id, "mage")

    time.sleep(0.5)
