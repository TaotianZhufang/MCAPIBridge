import 'dart:io';
import 'dart:convert';
import 'dart:async';
import 'dart:math';
import 'dart:typed_data';
import 'dart:collection';

// ========== Data Classes ==========

/// Represents a 3D vector or coordinate.
class Vec3 {
  /// X coordinate.
  final double x;

  /// Y coordinate.
  final double y;

  /// Z coordinate.
  final double z;

  /// Creates a new [Vec3] with the given coordinates.
  const Vec3(this.x, this.y, this.z);

  /// Adds two vectors.
  Vec3 operator +(Vec3 o) => Vec3(x + o.x, y + o.y, z + o.z);

  /// Subtracts two vectors.
  Vec3 operator -(Vec3 o) => Vec3(x - o.x, y - o.y, z - o.z);

  /// Multiplies vector by a scalar.
  Vec3 operator *(double s) => Vec3(x * s, y * s, z * s);

  /// Calculates dot product with another vector.
  double dot(Vec3 o) => x * o.x + y * o.y + z * o.z;

  /// Returns the length (magnitude) of the vector.
  double length() => sqrt(x * x + y * y + z * z);

  /// Returns the distance to another vector.
  double distanceTo(Vec3 o) => (this - o).length();

  /// Returns a normalized (unit length) version of this vector.
  Vec3 normalized() {
    final l = length();
    if (l == 0) return Vec3(0, 0, 0);
    return Vec3(x / l, y / l, z / l);
  }

  /// Converts to a Map representation.
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

/// Represents player position with rotation angles.
///
/// Extends [Vec3] with [yaw] (horizontal rotation) and [pitch] (vertical rotation).
class PlayerPos extends Vec3 {
  /// Horizontal rotation in degrees.
  final double yaw;

  /// Vertical rotation in degrees.
  final double pitch;

  /// Creates a new [PlayerPos].
  const PlayerPos(super.x, super.y, super.z, this.yaw, this.pitch);

  /// Returns the normalized direction vector based on yaw and pitch.
  Vec3 get direction {
    final yawRad = yaw * pi / 180;
    final pitchRad = pitch * pi / 180;
    return Vec3(
      -sin(yawRad) * cos(pitchRad),
      -sin(pitchRad),
      cos(yawRad) * cos(pitchRad),
    );
  }

  /// Returns a position [distance] blocks ahead of the player's view.
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

/// Represents a block hit event from player interaction.
class BlockHit {
  /// The position of the block that was hit.
  final Vec3 pos;

  /// The face of the block that was hit (0-5).
  final int face;

  /// The entity ID of the player who hit the block.
  final int entityId;

  /// The raw action code (1=left, 2=right, 101-105=key macros).
  final int action;

  /// The action type as a string ('LEFT_CLICK', 'RIGHT_CLICK', 'KEY_MACRO_N').
  final String type;

  /// Creates a new [BlockHit] event.
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

/// Represents a chat message posted by a player.
class ChatPost {
  /// The name of the player who sent the message.
  final String name;

  /// The message content.
  final String message;

  /// Creates a new [ChatPost].
  const ChatPost(this.name, this.message);

  @override
  String toString() => '[$name]: $message';
}

/// Represents the location of a screen block with dimension info.
class ScreenLocation {
  /// X coordinate.
  final double x;

  /// Y coordinate.
  final double y;

  /// Z coordinate.
  final double z;

  /// The dimension ID (e.g., 'minecraft:overworld').
  final String dimension;

  /// Creates a new [ScreenLocation].
  const ScreenLocation(this.x, this.y, this.z,
      [this.dimension = 'minecraft:overworld']);

  @override
  String toString() =>
      'Loc(${x.toStringAsFixed(1)}, ${y.toStringAsFixed(1)}, '
      '${z.toStringAsFixed(1)}, $dimension)';
}

// ========== Connection Exception ==========

/// Exception thrown when connection to Minecraft server fails.
class MCConnectionException implements Exception {
  /// The error message.
  final String message;

  /// Creates a new [MCConnectionException].
  MCConnectionException(this.message);

  @override
  String toString() => 'MCConnectionException: $message';
}

// ========== IO Manager ==========

/// Manages IO block (redstone signal) operations.
///
/// Access via [Minecraft.io].
class IOManager {
  final Minecraft _mc;
  IOManager._(this._mc);

  /// Writes a signal value to an IO channel.
  ///
  /// [channelId] is the IO channel ID.
  /// [value] can be a [bool] (true=15, false=0) or [int] (0-15).
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

  /// Reads the current signal strength from an IO channel.
  ///
  /// Returns a value between 0-15.
  Future<int> read(int channelId) async {
    return int.tryParse(await _mc._sendAndRecv('io.read($channelId)')) ?? 0;
  }

  /// Checks if the signal on a channel is high (above threshold).
  ///
  /// Default [threshold] is 7.
  Future<bool> isHigh(int channelId, {int threshold = 7}) async {
    return (await read(channelId)) > threshold;
  }

  /// Checks if the signal on a channel is low (at or below threshold).
  ///
  /// Default [threshold] is 7.
  Future<bool> isLow(int channelId, {int threshold = 7}) async {
    return (await read(channelId)) <= threshold;
  }

  /// Configures an IO block at the specified position.
  ///
  /// [mode] can be:
  /// - [bool]: true = output, false = input
  /// - [String]: 'out'/'output' = output, 'in'/'input' = input
  Future<void> config(int x, int y, int z, int channelId, dynamic mode,
      [String dimension = '']) async {
    String modeBool;
    if (mode is bool) {
      modeBool = mode ? 'true' : 'false';
    } else if (mode is String) {
      final lower = mode.toLowerCase();
      modeBool = (lower == 'out' || lower == 'output') ? 'true' : 'false';
    } else {
      modeBool = 'false';
    }
    _mc._send('io.config($x,$y,$z,$channelId,$modeBool,$dimension)');
  }
}

// ========== Audio Manager ==========

/// Manages audio playback for players.
///
/// Access via [Minecraft.audio].
class AudioManager {
  final Minecraft _mc;
  AudioManager._(this._mc);

  /// Loads a WAV file and sends it to the target player(s).
  ///
  /// Automatically converts stereo to mono and 8-bit to 16-bit.
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

  /// Loads raw PCM data (16-bit signed mono).
  ///
  /// [sampleRate] defaults to 44100.
  Future<void> loadRaw(String target, String audioId, Uint8List pcmData,
      {int sampleRate = 44100}) async {
    await _uploadPcm(target, audioId, sampleRate, pcmData);
  }

  /// Generates and loads a sine wave tone.
  ///
  /// [frequency] in Hz (default 440).
  /// [duration] in seconds (default 1.0).
  /// [volume] from 0.0 to 1.0 (default 1.0).
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

  /// Plays audio in 2D (non-positional).
  ///
  /// [volume] from 0.0 to 1.0.
  /// [loop] whether to loop the audio.
  void play(String target, String audioId,
      {double volume = 1.0, bool loop = false}) {
    _mc._send(
        'audio.play($target,$audioId,$volume,${loop ? "true" : "false"})');
  }

  /// Plays audio with 3D spatial positioning.
  ///
  /// [rolloff] controls volume falloff with distance.
  /// [offset] is the starting position in seconds.
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

  /// Pauses audio playback.
  void pause(String target, String audioId) =>
      _mc._send('audio.pause($target,$audioId)');

  /// Stops audio playback.
  void stop(String target, String audioId) =>
      _mc._send('audio.stop($target,$audioId)');

  /// Unloads audio from memory.
  void unload(String target, String audioId) =>
      _mc._send('audio.unload($target,$audioId)');

  /// Sets the volume of playing audio.
  void setVolume(String target, String audioId, double volume) =>
      _mc._send('audio.volume($target,$audioId,$volume)');

  /// Updates the 3D position of playing audio.
  void setPosition(String target, String audioId, double x, double y, double z) =>
      _mc._send('audio.position($target,$audioId,$x,$y,$z)');

  /// Clones an audio instance for simultaneous playback.
  void clone(String target, String sourceId, String newId) =>
      _mc._send('audio.clone($target,$sourceId,$newId)');

  /// Resets all audio for all players.
  void reset() => _mc._send('audio.reset(@a)');

  /// Plays audio at a world position, heard by players within radius.
  ///
  /// [radius] is the hearing distance (default 32).
  /// [volume] from 0.0 to 1.0 (default 1.0).
  void playAt(String audioId, double x, double y, double z,
      {double radius = 32, double volume = 1.0}) {
    _mc._send('audio.playAt($audioId,$x,$y,$z,$radius,$volume)');
  }

  /// Plays audio associated with a screen.
  void playOnScreen(String audioId, int screenId,
      {double volume = 1.0, bool loop = false}) {
    _mc._send('audio.playScreen(@a,$audioId,$screenId,$volume,'
        '${loop ? "true" : "false"})');
  }

  /// Synchronizes audio playback progress.
  ///
  /// [progress] is the position in seconds.
  void syncProgress(String audioId, double progress) =>
      _mc._send('audio.syncProgress(@a,$audioId,$progress)');
}

// ========== Main Minecraft Class ==========

/// Main class for communicating with the MCAPIBridge Minecraft mod.
///
/// Example:
/// ```dart
/// final mc = await Minecraft.connect(host: 'localhost', port: 4711);
/// mc.postToChat('Hello from Dart!');
/// await mc.disconnect();
/// ```
class Minecraft {
  /// The server host address.
  final String host;

  /// The server port.
  final int port;

  Socket? _socket;
  StreamSubscription? _subscription;

  /// Whether the client is currently connected.
  bool connected = false;

  final Queue<Completer<String>> _responseQueue = Queue();

  /// Audio manager for playing sounds.
  late final AudioManager audio;

  /// IO manager for redstone signal control.
  late final IOManager io;

  Minecraft._internal(this.host, this.port) {
    audio = AudioManager._(this);
    io = IOManager._(this);
  }

  /// Connects to a Minecraft server running the MCAPIBridge mod.
  ///
  /// [host] defaults to 'localhost'.
  /// [port] defaults to 4711.
  /// [timeout] defaults to 5 seconds.
  ///
  /// Throws [MCConnectionException] if connection fails.
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

  /// Reconnects to the server.
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

  Future<String> _sendAndRecv(String cmd,
      {Duration timeout = const Duration(seconds: 5)}) async {
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

  /// Sends a message to the chat.
  ///
  /// Supports Minecraft color codes with `§`.
  void postToChat(String msg) => _send('chat.post($msg)');

  /// Runs a server command.
  ///
  /// The leading `/` is optional and will be removed if present.
  void runCommand(String cmd) {
    if (cmd.startsWith('/')) cmd = cmd.substring(1);
    _send('server.runCommand($cmd)');
  }

  // ========== World ==========

  /// Sets a block at the specified position.
  ///
  /// [blockId] can be without 'minecraft:' prefix (e.g., 'stone').
  void setBlock(int x, int y, int z, String blockId, [String? dimension]) {
    final dimPart = dimension != null ? ',$dimension' : '';
    _send('world.setBlock($x,$y,$z,$blockId$dimPart)');
  }

  /// Gets the block ID at the specified position.
  ///
  /// Returns a string like 'minecraft:stone'.
  Future<String> getBlock(int x, int y, int z, [String? dimension]) async {
    final dimPart = dimension != null ? ',$dimension' : '';
    return _sendAndRecv('world.getBlock($x,$y,$z$dimPart)');
  }

  /// Spawns an entity at the specified position.
  ///
  /// Returns the entity ID, or -1 if failed.
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

  /// Spawns particles at the specified position.
  ///
  /// [count] is the number of particles.
  /// [dx], [dy], [dz] are the diffusion ranges.
  /// [speed] is the particle speed.
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

  /// Gets entities within a radius of a position.
  ///
  /// Returns a list of maps with 'id', 'type', and 'pos' ([Vec3]).
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

  /// Sets the velocity of an entity.
  void setEntityVelocity(int entityId, double vx, double vy, double vz) =>
      _send('entity.setVelocity($entityId,$vx,$vy,$vz)');

  /// Enables or disables gravity for an entity.
  void setEntityNoGravity(int entityId, {bool enable = true}) =>
      _send('entity.setNoGravity($entityId,${enable ? "true" : "false"})');

  /// Teleports an entity to the specified position.
  void teleportEntity(int entityId, double x, double y, double z) =>
      _send('entity.teleport($entityId,$x,$y,$z)');

  /// Sets NBT data on an entity.
  void setEntityNbt(int entityId, String nbtString) =>
      _send('entity.setNbt($entityId,$nbtString)');

  // ========== Player ==========

  /// Gets the player's position and rotation.
  ///
  /// [target] is optional player name.
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

  /// Gets the player's normalized direction vector.
  Future<Vec3> getDirectionVector([String target = '']) async {
    final pos = await getPlayerPos(target);
    return pos.direction;
  }

  /// Gets a list of all online players.
  ///
  /// Returns a list of maps with 'name' and 'id'.
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

  /// Gets detailed information about a player.
  ///
  /// Returns a map with name, id, mode, health, max_health, food, held_item, held_count.
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

  /// Gets the entity ID of a player by name.
  Future<int?> getPlayerEntityId(String name) async {
    final players = await getOnlinePlayers();
    for (final p in players) {
      if (p['name'] == name) return p['id'] as int;
    }
    return null;
  }

  /// Gets the name of a player by entity ID.
  Future<String?> getPlayerName(int entityId) async {
    final players = await getOnlinePlayers();
    for (final p in players) {
      if (p['id'] == entityId) return p['name'] as String;
    }
    return null;
  }

  /// Gets the player's inventory contents.
  ///
  /// Returns a list of maps with 'slot', 'id', 'count'.
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

  /// Sets the player's health.
  void setHealth(String target, double amount) =>
      _send('player.setHealth($target,$amount)');

  /// Sets the player's food level (0-20).
  void setFood(String target, int amount) =>
      _send('player.setFood($target,$amount)');

  /// Gives an item to the player.
  ///
  /// [count] defaults to 1.
  void give(String target, String itemId, {int count = 1}) =>
      _send('player.give($target,$itemId,$count)');

  /// Clears the player's inventory.
  ///
  /// If [itemId] is specified, only clears that item.
  void clearInventory(String target, [String itemId = '']) =>
      _send('player.clear($target,$itemId)');

  /// Applies a potion effect to the player.
  ///
  /// [durationSec] is the duration in seconds (default 30).
  /// [amplifier] is the effect level (default 1).
  void giveEffect(String target, String effectName,
      {int durationSec = 30, int amplifier = 1}) {
    _send('player.effect($target,$effectName,$durationSec,$amplifier)');
  }

  /// Teleports the player to the specified position.
  void teleport(double x, double y, double z, [String target = '']) {
    if (target.isNotEmpty) {
      _send('player.teleport($target,$x,$y,$z)');
    } else {
      _send('player.teleport($x,$y,$z)');
    }
  }

  /// Enables or disables flight for the player.
  void setFlying(String target,
      {bool allowFlight = true, bool isFlying = true}) {
    _send('player.setFlying($target,${allowFlight ? "true" : "false"},'
        '${isFlying ? "true" : "false"})');
  }

  /// Sets the player's flight speed.
  ///
  /// Default Minecraft value is 0.05.
  void setFlySpeed(String target, {double speed = 0.05}) =>
      _send('player.setSpeed($target,true,$speed)');

  /// Sets the player's walk speed.
  ///
  /// Default Minecraft value is 0.1.
  void setWalkSpeed(String target, {double speed = 0.1}) =>
      _send('player.setSpeed($target,false,$speed)');

  /// Enables or disables god mode (invulnerability) for the player.
  void setGodMode(String target, {bool enable = true}) =>
      _send('player.setGod($target,${enable ? "true" : "false"})');

  /// Forces the player to look at the specified position.
  void lookAt(String target, double x, double y, double z) =>
      _send('player.lookAt($target,$x,$y,$z)');

  // ========== Events ==========

  /// Polls for block hit events.
  ///
  /// Returns a list of [BlockHit] events since the last poll.
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

  /// Polls for chat message events.
  ///
  /// Returns a list of [ChatPost] events since the last poll.
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

  /// Sets NBT data on a block entity.
  void setBlockNbt(int x, int y, int z, String nbtString,
      [String? dimension]) {
    final dimPart = dimension != null ? ',$dimension' : '';
    _send('block.setNbt($x,$y,$z,$nbtString$dimPart)');
  }

  // ========== Sign ==========

  /// Sets text on a sign block.
  ///
  /// The block at the position must already be a sign.
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

  /// Updates the content of a screen block.
  ///
  /// [base64Data] should be a Base64-encoded image (JPG/PNG).
  void updateScreen(int screenId, String base64Data,
      [String target = '@a']) {
    _send('screen.update($target,$screenId,$base64Data)');
  }

  /// Gets the world locations of all screen blocks with the specified ID.
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

  /// Registers a screen location for audio positioning.
  void registerScreen(int screenId, double x, double y, double z,
      [String dimension = '']) {
    _send('screen.register($screenId,$x,$y,$z,$dimension)');
  }

  /// Creates a screen wall and registers it.
  ///
  /// [axis] is 'x' (East-West) or 'z' (North-South).
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

  /// Delays execution for the specified milliseconds.
  Future<void> delay(int ms) => Future.delayed(Duration(milliseconds: ms));

  // ========== Disconnect ==========

  /// Disconnects from the server.
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