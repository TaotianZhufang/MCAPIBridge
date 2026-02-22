import 'dart:io';
import 'dart:convert';
import 'dart:async';
import 'dart:math';
import 'dart:typed_data';
import 'dart:collection';

// ========== Data Classes ==========

class Vec3 {
  final double x, y, z;
  const Vec3(this.x, this.y, this.z);

  Vec3 operator +(Vec3 o) => Vec3(x + o.x, y + o.y, z + o.z);
  Vec3 operator -(Vec3 o) => Vec3(x - o.x, y - o.y, z - o.z);
  Vec3 operator *(double s) => Vec3(x * s, y * s, z * s);
  double dot(Vec3 o) => x * o.x + y * o.y + z * o.z;
  double length() => sqrt(x * x + y * y + z * z);
  double distanceTo(Vec3 o) => (this - o).length();
  Vec3 normalized() {
    final l = length();
    if (l == 0) return Vec3(0, 0, 0);
    return Vec3(x / l, y / l, z / l);
  }

  Map<String, double> toMap() => {'x': x, 'y': y, 'z': z};

  @override
  String toString() =>
      'Vec3(${x.toStringAsFixed(2)}, ${y.toStringAsFixed(2)}, ${z.toStringAsFixed(2)})';

  @override
  bool operator ==(Object other) =>
      other is Vec3 && x == other.x && y == other.y && z == other.z;

  @override
  int get hashCode => Object.hash(x, y, z);
}

class PlayerPos extends Vec3 {
  final double yaw, pitch;
  const PlayerPos(super.x, super.y, super.z, this.yaw, this.pitch);

  Vec3 get direction {
    final yawRad = yaw * pi / 180;
    final pitchRad = pitch * pi / 180;
    return Vec3(
      -sin(yawRad) * cos(pitchRad),
      -sin(pitchRad),
      cos(yawRad) * cos(pitchRad),
    );
  }

  Vec3 forward(double distance) {
    final dir = direction;
    return Vec3(x + dir.x * distance, y + dir.y * distance, z + dir.z * distance);
  }

  @override
  String toString() =>
      'PlayerPos(x=${x.toStringAsFixed(1)}, y=${y.toStringAsFixed(1)}, '
      'z=${z.toStringAsFixed(1)}, yaw=${yaw.toStringAsFixed(1)}, '
      'pitch=${pitch.toStringAsFixed(1)})';
}

class BlockHit {
  final Vec3 pos;
  final int face, entityId, action;
  final String type;

  BlockHit(int x, int y, int z, this.face, this.entityId, this.action)
      : pos = Vec3(x.toDouble(), y.toDouble(), z.toDouble()),
        type = _actionToType(action);

  static String _actionToType(int action) {
    switch (action) {
      case 1:
        return 'LEFT_CLICK';
      case 2:
        return 'RIGHT_CLICK';
      default:
        if (action > 100) return 'KEY_MACRO_${action - 100}';
        return 'UNKNOWN';
    }
  }

  @override
  String toString() =>
      'BlockHit(${pos.x.toInt()},${pos.y.toInt()},${pos.z.toInt()} '
      '$type player=$entityId)';
}

class ChatPost {
  final String name, message;
  const ChatPost(this.name, this.message);

  @override
  String toString() => '[$name]: $message';
}

class ScreenLocation {
  final double x, y, z;
  final String dimension;
  const ScreenLocation(this.x, this.y, this.z,
      [this.dimension = 'minecraft:overworld']);

  @override
  String toString() =>
      'Loc(${x.toStringAsFixed(1)}, ${y.toStringAsFixed(1)}, '
      '${z.toStringAsFixed(1)}, $dimension)';
}

// ========== Connection Exception ==========

class MCConnectionException implements Exception {
  final String message;
  MCConnectionException(this.message);

  @override
  String toString() => 'MCConnectionException: $message';
}

// ========== IO Manager ==========

class IOManager {
  final Minecraft _mc;
  IOManager._(this._mc);

  Future<void> write(int channelId, dynamic value) async {
    int power;
    if (value is bool) {
      power = value ? 15 : 0;
    } else if (value is int) {
      power = value.clamp(0, 15);
    } else {
      power = 0;
    }
    _mc._send('io.write($channelId,$power)');
  }

  Future<int> read(int channelId) async {
    return int.tryParse(await _mc._sendAndRecv('io.read($channelId)')) ?? 0;
  }

  Future<bool> isHigh(int channelId, {int threshold = 7}) async {
    return (await read(channelId)) > threshold;
  }

  Future<bool> isLow(int channelId, {int threshold = 7}) async {
    return (await read(channelId)) <= threshold;
  }

  Future<void> config(int x, int y, int z, int channelId, dynamic mode,
      [String dimension = '']) async {
    String modeBool;
    if (mode is bool) {
      modeBool = mode ? 'true' : 'false';
    } else if (mode is String) {
      final lower = mode.toLowerCase();
      modeBool =
          (lower == 'out' || lower == 'output') ? 'true' : 'false';
    } else {
      modeBool = 'false';
    }
    _mc._send('io.config($x,$y,$z,$channelId,$modeBool,$dimension)');
  }
}

// ========== Audio Manager ==========

class AudioManager {
  final Minecraft _mc;
  AudioManager._(this._mc);

  Future<void> loadWav(String target, String audioId, String filepath) async {
    final file = File(filepath);
    if (!await file.exists()) {
      throw FileSystemException('WAV file not found', filepath);
    }

    final bytes = await file.readAsBytes();
    if (bytes.length < 44) throw FormatException('File too small to be WAV');

    final data = ByteData.sublistView(Uint8List.fromList(bytes));
    final riff = String.fromCharCodes(bytes.sublist(0, 4));
    if (riff != 'RIFF') throw FormatException('Not a WAV file: missing RIFF header');

    final wave = String.fromCharCodes(bytes.sublist(8, 12));
    if (wave != 'WAVE') throw FormatException('Not a WAV file: missing WAVE format');

    int channels = data.getUint16(22, Endian.little);
    int sampleRate = data.getUint32(24, Endian.little);
    int bitsPerSample = data.getUint16(34, Endian.little);
    int sampleWidth = bitsPerSample ~/ 8;

    int dataOffset = 12;
    int dataSize = 0;
    while (dataOffset < bytes.length - 8) {
      final chunkId =
          String.fromCharCodes(bytes.sublist(dataOffset, dataOffset + 4));
      final chunkSize = data.getUint32(dataOffset + 4, Endian.little);
      if (chunkId == 'data') {
        dataOffset += 8;
        dataSize = min(chunkSize, bytes.length - dataOffset);
        break;
      }
      dataOffset += 8 + chunkSize;
      if (dataOffset % 2 != 0) dataOffset++;
    }

    if (dataSize == 0) throw FormatException('No data chunk found in WAV');

    Uint8List frames = bytes.sublist(dataOffset, dataOffset + dataSize);

    if (channels == 2 && sampleWidth == 2) {
      final sampleCount = frames.length ~/ (sampleWidth * channels);
      final frameData = ByteData.sublistView(frames);
      final monoSamples = Int16List(sampleCount);

      for (int i = 0; i < sampleCount; i++) {
        final left = frameData.getInt16(i * 4, Endian.little);
        final right = frameData.getInt16(i * 4 + 2, Endian.little);
        monoSamples[i] = ((left + right) ~/ 2);
      }
      frames = Uint8List.view(monoSamples.buffer);
    } else if (channels == 2 && sampleWidth == 1) {
      final sampleCount = frames.length ~/ 2;
      final monoBytes = Uint8List(sampleCount);
      for (int i = 0; i < sampleCount; i++) {
        monoBytes[i] = ((frames[i * 2] + frames[i * 2 + 1]) ~/ 2);
      }
      frames = monoBytes;
    }

    if (sampleWidth == 1) {
      final samples = Int16List(frames.length);
      for (int i = 0; i < frames.length; i++) {
        samples[i] = ((frames[i] - 128) * 256);
      }
      frames = Uint8List.view(samples.buffer);
    }

    await _uploadPcm(target, audioId, sampleRate, frames);
    print('[Audio] Loaded $filepath: ${frames.length} bytes, ${sampleRate}Hz');
  }

  Future<void> loadRaw(String target, String audioId, Uint8List pcmData,
      {int sampleRate = 44100}) async {
    await _uploadPcm(target, audioId, sampleRate, pcmData);
  }

  Future<void> generateTone(String target, String audioId, {
    double frequency = 440,
    double duration = 1.0,
    int sampleRate = 44100,
    double volume = 1.0,
  }) async {
    final numSamples = (sampleRate * duration).toInt();
    final samples = Int16List(numSamples);
    final amplitude = (32767 * volume.clamp(0.0, 1.0)).toInt();

    for (int i = 0; i < numSamples; i++) {
      final t = i / sampleRate;
      samples[i] = (amplitude * sin(2 * pi * frequency * t)).toInt();
    }

    await loadRaw(target, audioId, Uint8List.view(samples.buffer),
        sampleRate: sampleRate);
  }

  Future<void> _uploadPcm(
      String target, String audioId, int sampleRate, Uint8List frames) async {
    final b64Data = base64Encode(frames);
    const chunkSize = 40000;

    for (int i = 0; i < b64Data.length; i += chunkSize) {
      final end = min(i + chunkSize, b64Data.length);
      final chunk = b64Data.substring(i, end);

      if (i == 0) {
        _mc._send('audio.load($target,$audioId,$sampleRate,$chunk)');
      } else {
        _mc._send('audio.stream($target,$audioId,$sampleRate,$chunk)');
      }
      await Future.delayed(const Duration(milliseconds: 5));
    }

    _mc._send('audio.finishLoad($target,$audioId)');
  }

  void play(String target, String audioId,
      {double volume = 1.0, bool loop = false}) {
    _mc._send(
        'audio.play($target,$audioId,$volume,${loop ? "true" : "false"})');
  }

  void play3d(String target, String audioId, double x, double y, double z, {
    double volume = 1.0,
    double rolloff = 1.0,
    bool loop = false,
    String dimension = '',
    double offset = 0.0,
  }) {
    _mc._send('audio.play3d($target,$audioId,$x,$y,$z,$volume,$rolloff,'
        '${loop ? "true" : "false"},$dimension,$offset)');
  }

  void pause(String target, String audioId) =>
      _mc._send('audio.pause($target,$audioId)');

  void stop(String target, String audioId) =>
      _mc._send('audio.stop($target,$audioId)');

  void unload(String target, String audioId) =>
      _mc._send('audio.unload($target,$audioId)');

  void setVolume(String target, String audioId, double volume) =>
      _mc._send('audio.volume($target,$audioId,$volume)');

  void setPosition(String target, String audioId, double x, double y, double z) =>
      _mc._send('audio.position($target,$audioId,$x,$y,$z)');

  void clone(String target, String sourceId, String newId) =>
      _mc._send('audio.clone($target,$sourceId,$newId)');

  void reset() => _mc._send('audio.reset(@a)');

  void playAt(String audioId, double x, double y, double z,{double radius = 32, double volume = 1.0})
      {_mc._send('audio.playAt($audioId,$x,$y,$z,$radius,$volume)');}

  void playOnScreen(String audioId, int screenId,
      {double volume = 1.0, bool loop = false}) {
    _mc._send('audio.playScreen(@a,$audioId,$screenId,$volume,'
        '${loop ? "true" : "false"})');
  }

  void syncProgress(String audioId, double progress) =>
      _mc._send('audio.syncProgress(@a,$audioId,$progress)');
}

// ========== Main Minecraft Class ==========

class Minecraft {
  final String host;
  final int port;

  Socket? _socket;
  StreamSubscription? _subscription;
  bool connected = false;
  final Queue<Completer<String>> _responseQueue = Queue();
  //String _buffer = '';

  late final AudioManager audio;
  late final IOManager io;

  Minecraft._internal(this.host, this.port) {
    audio = AudioManager._(this);
    io = IOManager._(this);
  }

  static Future<Minecraft> connect({
    String host = 'localhost',
    int port = 4711,
    Duration timeout = const Duration(seconds: 5),
  }) async {
    final mc = Minecraft._internal(host, port);
    await mc._connectAsync(timeout);
    return mc;
  }

  Future<void> _connectAsync(Duration timeout) async {
    try {
      _socket = await Socket.connect(host, port).timeout(timeout);
      connected = true;
      _setupListener();
      print('[MCAPI] Connected ($host:$port).');
    } on SocketException catch (e) {
      connected = false;
      throw MCConnectionException('Cannot connect to $host:$port: $e');
    } on TimeoutException {
      connected = false;
      throw MCConnectionException('Connection to $host:$port timed out');
    }
  }

  Future<void> reconnect({Duration timeout = const Duration(seconds: 5)}) async {
    await disconnect();
    await _connectAsync(timeout);
  }

  void _setupListener() {
    _subscription = _socket!
        .cast<List<int>>()
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .listen(
      (line) {
        if (_responseQueue.isNotEmpty) {
          final completer = _responseQueue.removeFirst();
          if (!completer.isCompleted) {
            completer.complete(line.trim());
          }
        }
      },
      onError: (error) {
        print('[MCAPI] Socket error: $error');
        _failAllPending('Socket error');
        connected = false;
      },
      onDone: () {
        print('[MCAPI] Connection closed.');
        _failAllPending('Connection closed');
        connected = false;
      },
    );
  }

  void _failAllPending(String reason) {
    while (_responseQueue.isNotEmpty) {
      final c = _responseQueue.removeFirst();
      if (!c.isCompleted) {
        c.completeError(MCConnectionException(reason));
      }
    }
  }

  void _ensureConnected() {
    if (!connected || _socket == null) {
      throw MCConnectionException('Not connected to server');
    }
  }

  void _send(String cmd) {
    _ensureConnected();
    try {
      _socket!.write('$cmd\n');
    } catch (e) {
      connected = false;
      throw MCConnectionException('Send failed: $e');
    }
  }

  Future<String> _sendAndRecv(String cmd, {Duration timeout = const Duration(seconds: 5)}) async {
    _ensureConnected();
    final completer = Completer<String>();
    _responseQueue.addLast(completer);

    try {
      _socket!.write('$cmd\n');
    } catch (e) {
      _responseQueue.removeLast();
      connected = false;
      throw MCConnectionException('Send failed: $e');
    }

    try {
      return await completer.future.timeout(timeout);
    } on TimeoutException {
      return '';
    }
  }

  // ========== Chat ==========

  void postToChat(String msg) => _send('chat.post($msg)');

  void runCommand(String cmd) {
    if (cmd.startsWith('/')) cmd = cmd.substring(1);
    _send('server.runCommand($cmd)');
  }

  // ========== World ==========

  void setBlock(int x, int y, int z, String blockId, [String? dimension]) {
    final dimPart = dimension != null ? ',$dimension' : '';
    _send('world.setBlock($x,$y,$z,$blockId$dimPart)');
  }

  Future<String> getBlock(int x, int y, int z, [String? dimension]) async {
    final dimPart = dimension != null ? ',$dimension' : '';
    return _sendAndRecv('world.getBlock($x,$y,$z$dimPart)');
  }

  Future<int> spawnEntity(double x, double y, double z, String entityId, {
    double yaw = 0,
    double pitch = 0,
    String? dimension,
  }) async {
    final dimPart = dimension != null ? ',$dimension' : '';
    final resp = await _sendAndRecv(
        'world.spawnEntity($x,$y,$z,$entityId,$yaw,$pitch$dimPart)');
    return int.tryParse(resp) ?? -1;
  }

  void spawnParticle(double x, double y, double z, String particleId, {
    int count = 10,
    double dx = 0,
    double dy = 0,
    double dz = 0,
    double speed = 0,
    String? dimension,
  }) {
    var cmd = '$x,$y,$z,$particleId,$count,$dx,$dy,$dz,$speed';
    if (dimension != null) cmd += ',$dimension';
    _send('world.spawnParticle($cmd)');
  }

  Future<List<Map<String, dynamic>>> getEntities(
      double x, double y, double z, double radius,
      [String? dimension]) async {
    final dimPart = dimension != null ? ',$dimension' : '';
    final data =
        await _sendAndRecv('world.getEntities($x,$y,$z,$radius$dimPart)');
    if (data.isEmpty) return [];

    return data.split('|').map((item) {
      final p = item.split(',');
      if (p.length >= 5) {
        return {
          'id': int.parse(p[0]),
          'type': p[1],
          'pos': Vec3(double.parse(p[2]), double.parse(p[3]), double.parse(p[4])),
        };
      }
      return <String, dynamic>{};
    }).where((e) => e.isNotEmpty).toList();
  }

  // ========== Entity ==========

  void setEntityVelocity(int entityId, double vx, double vy, double vz) =>
      _send('entity.setVelocity($entityId,$vx,$vy,$vz)');

  void setEntityNoGravity(int entityId, {bool enable = true}) =>
      _send('entity.setNoGravity($entityId,${enable ? "true" : "false"})');

  void teleportEntity(int entityId, double x, double y, double z) =>
      _send('entity.teleport($entityId,$x,$y,$z)');

  void setEntityNbt(int entityId, String nbtString) =>
      _send('entity.setNbt($entityId,$nbtString)');

  // ========== Player ==========

  Future<PlayerPos> getPlayerPos([String target = '']) async {
    final data = await _sendAndRecv('player.getPos($target)');
    if (data.isEmpty) return const PlayerPos(0, 0, 0, 0, 0);
    final parts = data.split(',');
    if (parts.length >= 5) {
      return PlayerPos(
        double.parse(parts[0]),
        double.parse(parts[1]),
        double.parse(parts[2]),
        double.parse(parts[3]),
        double.parse(parts[4]),
      );
    }
    return const PlayerPos(0, 0, 0, 0, 0);
  }

  Future<Vec3> getDirectionVector([String target = '']) async {
    final pos = await getPlayerPos(target);
    return pos.direction;
  }

  Future<List<Map<String, dynamic>>> getOnlinePlayers() async {
    final d = await _sendAndRecv('world.getPlayers()');
    if (d.isEmpty) return [];
    return d.split('|').map((item) {
      final parts = item.split(',');
      if (parts.length == 2) {
        return {'name': parts[0], 'id': int.parse(parts[1])};
      }
      return <String, dynamic>{};
    }).where((e) => e.isNotEmpty).toList();
  }

  Future<Map<String, dynamic>?> getPlayerDetails([String target = '']) async {
    final d = await _sendAndRecv('player.getDetails($target)');
    if (d.isEmpty || d.contains('Error')) return null;
    final p = d.split(',');
    if (p.length < 8) return null;
    return {
      'name': p[0],
      'id': int.parse(p[1]),
      'mode': p[2],
      'health': double.parse(p[3]),
      'max_health': double.parse(p[4]),
      'food': int.parse(p[5]),
      'held_item': p[6],
      'held_count': int.parse(p[7]),
    };
  }

  Future<int?> getPlayerEntityId(String name) async {
    final players = await getOnlinePlayers();
    for (final p in players) {
      if (p['name'] == name) return p['id'] as int;
    }
    return null;
  }

  Future<String?> getPlayerName(int entityId) async {
    final players = await getOnlinePlayers();
    for (final p in players) {
      if (p['id'] == entityId) return p['name'] as String;
    }
    return null;
  }

  Future<List<Map<String, dynamic>>> getInventory([String target = '']) async {
    final d = await _sendAndRecv('player.getInventory($target)');
    if (d.isEmpty || d.contains('EMPTY') || d.contains('ERROR')) return [];

    return d.split('|').map((itemStr) {
      final parts = itemStr.split(':');
      if (parts.length == 4) {
        return {
          'slot': int.parse(parts[0]),
          'id': '${parts[1]}:${parts[2]}',
          'count': int.parse(parts[3]),
        };
      } else if (parts.length == 3) {
        return {
          'slot': int.parse(parts[0]),
          'id': parts[1],
          'count': int.parse(parts[2]),
        };
      }
      return <String, dynamic>{};
    }).where((e) => e.isNotEmpty).toList();
  }

  void setHealth(String target, double amount) =>
      _send('player.setHealth($target,$amount)');

  void setFood(String target, int amount) =>
      _send('player.setFood($target,$amount)');

  void give(String target, String itemId, {int count = 1}) =>
    _send('player.give($target,$itemId,$count)');

  void clearInventory(String target, [String itemId = '']) =>
      _send('player.clear($target,$itemId)');

  void giveEffect(String target, String effectName,
      {int durationSec = 30, int amplifier = 1}) {
    _send('player.effect($target,$effectName,$durationSec,$amplifier)');
  }

  void teleport(double x, double y, double z, [String target = '']) {
    if (target.isNotEmpty) {
      _send('player.teleport($target,$x,$y,$z)');
    } else {
      _send('player.teleport($x,$y,$z)');
    }
  }

  void setFlying(String target,
      {bool allowFlight = true, bool isFlying = true}) {
    _send('player.setFlying($target,${allowFlight ? "true" : "false"},'
        '${isFlying ? "true" : "false"})');
  }

  void setFlySpeed(String target, {double speed = 0.05}) =>
      _send('player.setSpeed($target,true,$speed)');

  void setWalkSpeed(String target, {double speed = 0.1}) =>
      _send('player.setSpeed($target,false,$speed)');

  void setGodMode(String target, {bool enable = true}) =>
      _send('player.setGod($target,${enable ? "true" : "false"})');

  void lookAt(String target, double x, double y, double z) =>
      _send('player.lookAt($target,$x,$y,$z)');

  // ========== Events ==========

  Future<List<BlockHit>> pollBlockHits() async {
    final d = await _sendAndRecv('events.block.hits()');
    if (d.isEmpty) return [];
    return d.split('|').map((item) {
      final p = item.split(',');
      if (p.length >= 6) {
        return BlockHit(
          int.parse(p[0]),
          int.parse(p[1]),
          int.parse(p[2]),
          int.parse(p[3]),
          int.parse(p[4]),
          int.parse(p[5]),
        );
      }
      return null;
    }).whereType<BlockHit>().toList();
  }

  Future<List<ChatPost>> pollChatPosts() async {
    final data = await _sendAndRecv('events.chat.posts()');
    if (data.isEmpty) return [];
    return data.split('|').map((item) {
      final idx = item.indexOf(',');
      if (idx > 0) {
        return ChatPost(item.substring(0, idx), item.substring(idx + 1));
      }
      return null;
    }).whereType<ChatPost>().toList();
  }

  // ========== Block NBT ==========

  void setBlockNbt(int x, int y, int z, String nbtString,
      [String? dimension]) {
    final dimPart = dimension != null ? ',$dimension' : '';
    _send('block.setNbt($x,$y,$z,$nbtString$dimPart)');
  }

  // ========== Sign ==========

  void setSign(int x, int y, int z, {
    String l1 = '',
    String l2 = '',
    String l3 = '',
    String l4 = '',
    String? dimension,
  }) {
    final lines = [l1, l2, l3, l4].map((l) => l.replaceAll(',', '，')).toList();
    var cmd = 'world.setSign($x,$y,$z,${lines.join(",")})';
    if (dimension != null) cmd += ',$dimension';
    _send(cmd);
  }

  // ========== Screen ==========

  void updateScreen(int screenId, String base64Data,
      [String target = '@a']) {
    _send('screen.update($target,$screenId,$base64Data)');
  }

  Future<List<ScreenLocation>> getScreenLocations(int screenId) async {
    final resp = await _sendAndRecv('screen.getPos($screenId)');
    if (resp.isEmpty || resp.contains('ERROR')) return [];

    try {
      return resp.split('|').map((part) {
        final c = part.split(',');
        if (c.length >= 4) {
          return ScreenLocation(
            double.parse(c[0]),
            double.parse(c[1]),
            double.parse(c[2]),
            c[3],
          );
        } else if (c.length == 3) {
          return ScreenLocation(
            double.parse(c[0]),
            double.parse(c[1]),
            double.parse(c[2]),
          );
        }
        return null;
      }).whereType<ScreenLocation>().toList();
    } catch (_) {
      return [];
    }
  }

  void registerScreen(int screenId, double x, double y, double z,
      [String dimension = '']) {
    _send('screen.register($screenId,$x,$y,$z,$dimension)');
  }

  void createScreenWall(
    int startX,
    int startY,
    int startZ,
    int width,
    int height, {
    String axis = 'x',
    int screenId = 1,
    String dimension = '',
  }) {
    print('Building ${width}x$height screen (ID $screenId)...');
    final facing = axis == 'x' ? 'south' : 'east';

    for (int gy = 0; gy < height; gy++) {
      for (int gx = 0; gx < width; gx++) {
        final y = startY + gy;
        int x, z;
        if (axis == 'x') {
          x = startX + gx;
          z = startZ;
        } else {
          x = startX;
          z = startZ + (width - 1 - gx);
        }

        setBlock(x, y, z, 'mcapibridge:screen[facing=$facing]');
        setBlockNbt(x, y, z,
            '{ScreenId:$screenId,W:$width,H:$height,GX:$gx,GY:$gy}');
      }
    }

    double cx, cy, cz;
    if (axis == 'x') {
      cx = startX + width / 2.0;
      cy = startY + height / 2.0;
      cz = startZ + 1.0;
    } else {
      cx = startX + 1.0;
      cy = startY + height / 2.0;
      cz = startZ + width / 2.0;
    }

    registerScreen(screenId, cx, cy, cz, dimension);
    print('Screen registered at '
        '(${cx.toStringAsFixed(1)}, ${cy.toStringAsFixed(1)}, '
        '${cz.toStringAsFixed(1)})');
  }

  // ========== Utility ==========

  Future<void> delay(int ms) => Future.delayed(Duration(milliseconds: ms));

  // ========== Disconnect ==========

  Future<void> disconnect() async {
    _failAllPending('Disconnecting');
    try {
      await _subscription?.cancel();
      await _socket?.close();
    } catch (_) {}
    _socket = null;
    _subscription = null;
    connected = false;
  }
}