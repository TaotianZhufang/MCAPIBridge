from mc import Minecraft, Vec3
import time
import math

mc = Minecraft()

print("Railgun Script Loaded.")
mc.postToChat("§c[Weapon] §fRailgun Active! Right-click to shoot.")

while True:
    hits = mc.pollBlockHits()
    
    for hit in hits:
        if hit.action == 2:
            
            p_pos = mc.getPlayerPos(mc.getPlayerName(hit.entityId))
            
            spawn_pos = p_pos.forward(1.5) 
            spawn_pos.y += 1.6 
            
            direction = mc.getDirectionVector()
            force = 5.0
            
            arrow_id = mc.spawnEntity(
                spawn_pos.x, spawn_pos.y, spawn_pos.z, 
                "arrow", 
                yaw=p_pos.yaw, pitch=p_pos.pitch
            )
            
            if arrow_id != -1:
                mc.setEntityNoGravity(arrow_id, True)
                
                mc.setEntityVelocity(
                    arrow_id, 
                    direction.x * force, 
                    direction.y * force, 
                    direction.z * force
                )
                
                mc.spawnParticle(spawn_pos.x, spawn_pos.y, spawn_pos.z, "flash", count=1)
                mc.spawnParticle(spawn_pos.x, spawn_pos.y, spawn_pos.z, "sonic_boom", count=1)
                
                print(f"Shot fired by ID {hit.entityId}")

    time.sleep(0.05)
