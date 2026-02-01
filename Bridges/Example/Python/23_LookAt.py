from mc import Minecraft, Vec3
import time
import math

mc = Minecraft()
pos = mc.getPlayerPos()

# Spawn the "Angel" (Armor Stand)
angel_id = mc.spawnEntity(pos.x + 10, pos.y, pos.z + 10, "armor_stand")
mc.setEntityNbt(angel_id, "{ShowArms:1b, NoBasePlate:1b}") # Visuals

print("Don't blink...")

while True:
    # 1. Get positions
    me_pos = mc.getPlayerPos() # Includes yaw/pitch
    angel_pos = mc.getEntities(me_pos.x, me_pos.y, me_pos.z, radius=20)
    
    # Find our angel in the list
    angel = next((e for e in angel_pos if e['id'] == angel_id), None)
    if not angel: break # Angel died/despawned

    # 2. Check if player is looking at the angel
    # Calculate vector to angel
    dx = angel['pos'].x - me_pos.x
    dy = angel['pos'].y - me_pos.y
    dz = angel['pos'].z - me_pos.z
    dist = math.sqrt(dx*dx + dy*dy + dz*dz)
    
    # Normalize
    dx /= dist; dy /= dist; dz /= dist
    
    # Get player view vector
    view = mc.getDirectionVector()
    
    # Dot product: 1.0 = looking directly at it, -1.0 = looking away
    dot = dx*view.x + dy*view.y + dz*view.z
    
    is_looking = dot > 0.5 # 60 degree cone
    
    # 3. Logic
    if is_looking:
        # Freeze! Force angel to look back at player for creepy effect
        mc.lookAt(angel_id, me_pos.x, me_pos.y + 1.6, me_pos.z)
    else:
        # Move closer!
        # Teleport 0.5 blocks towards player
        new_x = angel['pos'].x - (dx * 0.5)
        new_z = angel['pos'].z - (dz * 0.5)
        mc.teleportEntity(angel_id, new_x, angel['pos'].y, new_z)
        
        # Force player to look at the statue (Jumpscare!)
        if dist < 2:
             mc.lookAt(mc.getOnlinePlayers()[0]['name'], angel['pos'].x, angel['pos'].y + 1.5, angel['pos'].z)
             mc.postToChat("§cCAUGHT YOU!")
             break

    time.sleep(0.1)
