"""
MCAPIBridge IO Block Demo
Demonstrates hardware-like interaction using the IO Port block.
"""

from mc import Minecraft
import time

# ============================================================
# CONFIGURATION
# ============================================================

# 1. Place an IO Block in-game.
# 2. Right-click to set Mode to "Input (Py -> MC)" and ID to 1.
# 3. Connect a Redstone Lamp to it.
INPUT_CH = 1

# 1. Place another IO Block.
# 2. Right-click to set Mode to "Output (MC -> Py)" and ID to 2.
# 3. Place a Lever next to it.
OUTPUT_CH = 2

# ============================================================
# MAIN LOGIC
# ============================================================

mc = Minecraft()
if not mc.getOnlinePlayers(): 
    print("❌ Not connected to Minecraft")
    exit()

print(f"✅ Connected. Testing IO Channels {INPUT_CH} (In) and {OUTPUT_CH} (Out)")

try:
    # --------------------------------------------------------
    # Part 1: Write Test (Control Redstone)
    # --------------------------------------------------------
    print("\n--- 1. Write Test (Python -> Redstone) ---")
    print("Blinking Redstone Lamp...")
    
    for i in range(3):
        print(f"  [{i+1}/3] ON")
        mc.io.write(INPUT_CH, True) # High (15)
        time.sleep(0.5)
        
        print(f"  [{i+1}/3] OFF")
        mc.io.write(INPUT_CH, False) # Low (0)
        time.sleep(0.5)
        
    # Analog signal test
    print("  Analog Fade In...")
    for power in range(16):
        mc.io.write(INPUT_CH, power)
        time.sleep(0.1)
    mc.io.write(INPUT_CH, 0)

    # --------------------------------------------------------
    # Part 2: Read Test (Monitor Redstone)
    # --------------------------------------------------------
    print(f"\n--- 2. Read Test (Redstone -> Python) ---")
    print(f"Please toggle the lever connected to Channel {OUTPUT_CH}...")
    
    start_time = time.time()
    last_state = -1
    
    # Monitor for 20 seconds
    while time.time() - start_time < 20:
        # Read current power level
        power = mc.io.read(OUTPUT_CH)
        
        if power != last_state:
            state_str = "HIGH" if mc.io.isHigh(OUTPUT_CH) else "LOW"
            print(f"📡 Signal Changed: Power={power} ({state_str})")
            last_state = power
            
        time.sleep(0.1) # 10Hz polling rate

    print("\n✅ Demo Complete")

except KeyboardInterrupt:
    print("\nStopped by user")
finally:
    # Reset
    mc.io.write(INPUT_CH, 0)
