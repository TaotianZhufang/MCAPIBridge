import 'package:mcapibridge/mcapibridge.dart';

void main() async {
  final mc = await Minecraft.connect(host: 'localhost', port: 4711);
  
  mc.postToChat('Hello from Dart!');
  
  final pos = await mc.getPlayerPos();
  print('Player at: $pos');
  print('Looking direction: ${pos.direction}');
  
  final ahead = pos.forward(3);
  mc.setBlock(ahead.x.toInt(), ahead.y.toInt(), ahead.z.toInt(), 'minecraft:diamond_block');
  
  await mc.audio.loadWav('@a', 'bgm', 'music.wav');
  mc.audio.play('@a', 'bgm', volume: 0.8, loop: true);
  
  while (mc.connected) {
    final hits = await mc.pollBlockHits();
    for (final hit in hits) {
      print(hit);
      mc.postToChat('Block hit at ${hit.pos}');
    }
    
    final chats = await mc.pollChatPosts();
    for (final chat in chats) {
        final p = await mc.getPlayerPos(chat.name);
        print(chat.name+":"+chat.message);
        mc.postToChat('${chat.name} is at $p');
    }
    
    await mc.delay(50);
  }
  
  await mc.disconnect();
}