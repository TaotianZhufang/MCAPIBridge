import 'package:mcapibridge/mcapibridge.dart';

void main() async {
  final mc = await Minecraft.connect(
    host: 'localhost',
    port: 4711,
  );

  mc.postToChat('Hello from Dart!');

  final pos = await mc.getPlayerPos();
  print('Player position: $pos');

  mc.setBlock(
    pos.x.toInt(),
    pos.y.toInt() - 1,
    pos.z.toInt(),
    'minecraft:diamond_block',
  );

  await mc.disconnect();
}