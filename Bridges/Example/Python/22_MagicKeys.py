from mc import Minecraft
import time

mc = Minecraft()
mc.postToChat("§fMagic Keys Loaded")
mc.postToChat("§7Key1:Firball | 2:Flash | 3:Heal | 4:Summon | 5:Thunderbolt")

cooldowns = {}

def check_cooldown(pid, skill, wait_time):
    now = time.time()
    if pid not in cooldowns: cooldowns[pid] = {}
    
    last = cooldowns[pid].get(skill, 0)
    if now - last < wait_time:
        remain = int(wait_time - (now - last))
        mc.postToChat(f"§f{skill} Remain {remain}s")
        return False
    
    cooldowns[pid][skill] = now
    return True

while True:
    hits = mc.pollBlockHits()
    
    for hit in hits:
        pid = hit.entityId
        pos = mc.getPlayerPos()
        
        if hit.action == 101:
            if check_cooldown(pid, "fireball", 1.0):
                mc.postToChat("§6>> Fireball!")
                d = mc.getDirectionVector()
                spawn_pos = pos.forward(1.5)
                spawn_pos.y += 1.5
                fid = mc.spawnEntity(spawn_pos.x, spawn_pos.y, spawn_pos.z, "small_fireball")
                mc.setEntityVelocity(fid, d.x*2, d.y*2, d.z*2)

        elif hit.action == 102:
            if check_cooldown(pid, "flash", 3.0):
                mc.postToChat("§b>> Flash!")
                mc.spawnParticle(pos.x, pos.y, pos.z, "poof", count=10)
                target = pos.forward(8.0)
                mc.teleportEntity(pid, target.x, target.y, target.z)

        elif hit.action == 103:
            if check_cooldown(pid, "heal", 10.0):
                mc.postToChat("§a>> Heal")
                mc.giveEffect(pid, "regeneration", 5, 2)
                mc.spawnParticle(pos.x, pos.y, pos.z, "heart", count=10)

        elif hit.action == 104:
            if check_cooldown(pid, "summon", 30.0):
                mc.postToChat("§e>> Summon")
                wolf = mc.spawnEntity(pos.x, pos.y, pos.z, "wolf")

        elif hit.action == 105:
            if check_cooldown(pid, "lightning", 60.0):
                mc.postToChat("§c§l>> Lightning!")
                import random
                for _ in range(10):
                    rx = random.randint(-10, 10)
                    rz = random.randint(-10, 10)
                    mc.spawnEntity(pos.x+rx, pos.y, pos.z+rz, "lightning_bolt")
                    time.sleep(0.1)

    time.sleep(0.05)
