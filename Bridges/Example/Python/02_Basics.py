from mc import Minecraft
import time

mc = Minecraft()

mc.postToChat("§e[System] §fPython script is controling your game.")

mc.runCommand("time set noon")

mc.runCommand("weather clear")
