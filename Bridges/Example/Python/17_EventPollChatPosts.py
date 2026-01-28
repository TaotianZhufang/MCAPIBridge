from mc import Minecraft
import time

mc = Minecraft()

while True:
    chats = mc.pollChatPosts()
    for chat in chats:
        mc.postToChat(f"Recieve: {chat.name} say: {chat.message}")
        
        if "hello" in chat.message:
            mc.postToChat(f"Hello {chat.name},I'm Python robot.")
            
    time.sleep(0.5)
