# MCAPIBridge Python Library

MCAPIBridge is a mod for Minecraft loaded with Fabric.This libary offers some ways to connect Minecraft with this mod in Python.

## QuickStart

### Install
Ensure your mod loaded.

**Please ensure you are using the latest libary.Now version is 0.4.0.**

Use pip to install this.
```
pip install mcapibridge
```
This is a simple example to connect.
```
from mc import Minecraft
import time

# Default ip is localhost and port is 4711
# You can use mc = Minecraft(host=YOURIP,port=YOURPORT) to change
mc = Minecraft()

# Show a message
mc.postToChat("§aHello, Minecraft! Python is here.")
```

---

## Classes

### class `Minecraft`

#### Basic methods

##### `Minecraft.postToChat(msg)`
Send a message to chat screen.
*   **msg**: String message.Support `§` .You can check it on Minecraft wiki.

##### `Minecraft.runCommand(cmd)`
Run commands as Server.
*   **cmd**: Srting command without '/'

Ex: `Minecraft.runCommand("time set day")`

---

#### World methods

##### `Minecraft.setBlock(x, y, z, block_id, dimension=None)`
Set a block at the point given.
*   **x, y, z**: Int positions.
*   **block_id**: String ID.If it is Minecraft vanilla,the ID can be written without `Minecraft:`. Ex:`"stone"`, `"diamond_block"`.Support other namespaces.
*	**dimension**: Optional String target dimension/player name.

##### `Minecraft.getBlock(x, y, z, dimension=None)`
Gets the block ID at the specified coordinates.
*   **x, y, z**: Int positions.
*   **dimension**: Optional String target dimension/player name.
*   **return**: String block ID.Ex.`"minecraft:grass_block"`.

##### `Minecraft.spawnEntity(x, y, z, entity_id, yaw=0.0, pitch=0.0, dimension=None)`
Spawn an entity at the point given.
*   **entity_id**: String ID.If it is Minecraft vanilla,the ID can be written without `Minecraft:`. Ex:`"zombie"`, `"pig"`, `"lightning_bolt"`.Support other namespaces.
*   **yaw**: Optional Int degree.Horizontal degree.
*   **pitch**: Optional Int degree.Vertical degree.
*   **dimension**: Optional String target dimension/player name.

##### `Minecraft.setEntityVelocity(entity_id, vx, vy, vz)`
Sets the velocity of an entity.
*   **entity_id**: Int ID.
*   **vx, vy, vz**: Double velocity components.

##### `Minecraft.setEntityNoGravity(entity_id, enable=True)`
Enables or disables gravity for an entity.
*   **entity_id**: Int ID.

##### `Minecraft.getEntities(x, y, z, radius=10, dimension=None)`
Gets the block ID at the specified coordinates.
*   **x, y, z**: Double positions.
*   **radius**: Double search radius.
*   **dimension**: Optional String target dimension/player name.
*   **return**: List of Dicts: [{'id': 123, 'type': 'minecraft:zombie', 'pos': Vec3}, ...]

##### `Minecraft.spawnParticle(x, y, z, particle_id, count=10, dx=0.0, dy=0.0, dz=0.0, speed=0.0, dimension=None)`
Spawn particle at the point given.
*   **particle_id**: String ID.If it is Minecraft vanilla,the ID can be written without `Minecraft:`. Ex:`"flame"`, `"heart"`.Support other namespaces.
*   **count**: Int count.
*   **dx, dy, dz**: Optional double diffusion ranges.(when count=0, it represents the direction vector)
*   **speed**: Optional double speed.
*   **dimension**: Optional String target dimension/player name.

##### `Minecraft.setSign(x, y, z, line1="", line2="", line3="", line4="", dimension=None)`
Sets the text on a sign block.
*   **x, y, z**: Int positions.
*   **line1-4**: String text for each line.
*   **dimension**: Optional String target dimension/player name.

**The block at the position must already be a sign.**

##### `Minecraft.lookAt(target, x, y, z)`
Forces a player or entity to look at a specific coordinate.
*   **target**: String player name or Entity ID.
*   **x, y, z**: The coordinate to look at.
*   *Example*: `mc.lookAt("Steve", 0, 100, 0)` forces Steve to look up.

##### `Minecraft.setEntityNbt(entity_id, nbt_string)`
Modifies the NBT data of an entity directly using JSON format.
*   **entity_id**: Integer Entity ID.
*   **nbt_string**: Valid SNBT string (e.g., `"{NoAI:1b, Glowing:1b}"`).
*   *Note*: Useful for setting attributes like Scale in 1.20.6 (`{Attributes:[{Name:"generic.scale",Base:2.0d}]}`).

##### `Minecraft.setBlockNbt(x, y, z, nbt_string, dimension=None)`
Modifies the NBT data of a block entity (Tile Entity).
*   **x, y, z**: Integer coordinates.
*   **nbt_string**: Valid SNBT string.


---

#### Class audio

##### [Audio Doc](https://github.com/TaotianZhufang/MCAPIBridge/blob/main/Bridges/Python/PyAudio.MD)

---

#### Screen methods

##### `Minecraft.updateScreen(screen_id, image_data)`
Update the content of a Custom Screen block.
*   **screen_id**: Int ID of the target screen (configured in-game via Stick).
*   **image_data**: String Base64 encoded image data (JPG/PNG).

##### `Minecraft.getScreenLocations(screen_id)`
Get world coordinates for all screen blocks with the specified ID.
*   **screen_id**: Int ID of the screen.
*   **return**: List of `ScreenLocation` objects. Each object contains `x`, `y`, `z` coordinates and a `dimension` property.

##### `Minecraft.registerScreen(screen_id, x, y, z, dimension="")`
Manually register a screen location (used for programmatic screen creation).
*   **screen_id**: Int ID of the screen.
*   **x, y, z**: Double coordinates of the screen center.
*   **dimension**: (Optional) String dimension ID (e.g., `"minecraft:the_nether"`). Defaults to empty string (context-dependent).

##### `Minecraft.createScreenWall(start_x, start_y, start_z, width, height, axis='x', screen_id=1, dimension="")`
Automatically build a screen wall and register it.
*   **start_x, start_y, start_z**: Int starting coordinates.
*   **width**: Int width of the screen (in blocks).
*   **height**: Int height of the screen (in blocks).
*   **axis**: String `'x'` (East-West wall) or `'z'` (North-South wall).
*   **screen_id**: Int ID to assign to the screen.
*   **dimension**: (Optional) String dimension ID. Defaults to empty string.

---

#### Info methods

##### `Minecraft.getOnlinePlayers()`
Get players' name online.
*   **return**: Dict:  `[{'name': 'Steve', 'id': 123}, ...]`.

##### `Minecraft.getPlayerPos(target="")`
Get player's position and yaw and pitch.
*   **target**: String player name.
*   **return**: Int x,y,z and Double yaw,pitch.

##### `Minecraft.getPlayerEntityId(name)`
Get player's ID.
*   **name**: String player name.
*   **return**: Int ID.

##### `Minecraft.getPlayerName(entity_id)`
Get player's ID.
*   **entity_id**: Int ID.
*   **return**: String player name.

##### `Minecraft.getPlayerDetails(target="")`
Get player's details.
*   **target**: String player name.
*   **return**: Dict including
    *   `name`: String player name
    *   `id`: Int ID
    *   `mode`: String gamemode
    *   `health`: Double health
    *   `max_health`: Double max health
    *   `food`: Int food
    *   `held_item`: String item held
    *   `held_count`: Int item held count

---

#### State methods

##### `Minecraft.setHealth(target, amount)`
Set player's health.
*   **target**: String player name.
*   **amount**: Double health.

##### `Minecraft.setFood(target, amount)`
Set player's food.
*   **target**: String player name.
*   **amount**: Int food(0-20).

##### `Minecraft.giveEffect(target, effect_name, duration_sec=30, amplifier=1)`
Effect player.
*   **effect_name**: String effect ID.Ex:`"speed"`,`"night_vision"`.
*   **duration_sec**: Int seconds.
*   **amplifier**: Int amplifier.

##### `Minecraft.setFlying(target, allow_flight=True, is_flying=True)`
Enable player to fly in survival mode.
*   **target**: String player name.

##### `Minecraft.setFlySpeed(target, speed=0.05)`
Set flight speed.
*   **target**: String player name.
*   **speed**: Double speed.Default 0.05.

##### `Minecraft.setWalkSpeed(target, speed=0.1)`
Set walk speed.
*   **target**: String player name.
*   **speed**: Double speed.Default 0.1.

##### `Minecraft.setGodMode(target, enable=True)`
Enable invulnerability.
*   **target**: String player name.

---

#### Inventory methods

##### `Minecraft.getInventory(target="")`
Get player's inventory.
*   **return**: List of Dicts.Every dict has `{'slot': block_ID, 'id': item_ID, 'count': item_count}`.

##### `Minecraft.give(target, item_id, count=1)`
Give player item.
*   **item_id**: String item ID.
*   **count**: Int count.

##### `Minecraft.clearInventory(target, item_id="")`
Clear inventory.
*   **target**: String player name.
*   **item_id**: String item ID.

---

#### TP methods

##### `Minecraft.teleport(x, y, z, target="")`
TP player.
*   **x, y, z**: Int target x,y,z.
*   **target**: String player ID.

##### `Minecraft.teleportEntity(entity_id, x, y, z)`
TP entity.
*   **entity_id**: Int entity id.

---

#### Events methods

##### `Minecraft.pollBlockHits()`
Get click events.
*   **return**: List Class `[BlockHit1,BlockHit2,.....]`
    * **pos**: Class Vec3
		* Double x
		* Double y
		* Double z
	* **face**: Int click face.
	* **entityId**: Int ID of clicking on entity.
	* **action**: Int action type:1--left click,2--right click,101-105--Keyboard pressed.(Bind keys at Minecraft settings)
	* **type**: String action:"LEFT_CLICK" or "RIGHT_CLICK"

##### `Minecraft.pollChatPosts()`
Get player message events.
*   **return**: List Class `[ChatPost1,ChatPost2,.....]`
	* **name**: String player name.
	* **message**: String message.

---

#### Helper methods

##### `Minecraft.getDirectionVector(target="")`
Calculates the direction vector based on a player's rotation. Useful for shooting projectiles.
*   **target**: String player ID.
*   **return**: Class `Vec3` normalized direction vector.

---

### class `Vec3`
Represents a 3D vector/coordinate.
*   **properties**: `x`,`y`,`z`.
*   **Methods**:
    *   **Vec3.length()**: Returns vector length.

---

### class `PlayerPos`
**Inherits Vec3** Represents player position with rotation.
*   **properties**: `x`,`y`,`z`,`yaw`,`pitch`.
*   **Methods**:
    *   **PlayerPos.forward(distance=1.0)**: Returns a new `Vec3` position at `distance` blocks ahead of the player's view.

---

### class `ScreenLocation`
Represents the location of a screen or audio source, including dimension information.
*   **properties**: `x`, `y`, `z`, `dimension`.
*   **Constructor**: `ScreenLocation(x, y, z, dimension="minecraft:overworld")`

---

### Class `IOManager`

Provides interaction with the IO blocks (redstone control). All functions are accessed via `mc.io`.

#### `IOManager.write(channel_id, value)`
Send a signal to an IO block (Input Mode).
*   **channel_id**: Int ID of the target IO channel.
*   **value**: Can be an `int` (0-15) for analog signal strength, or `bool` (True=15, False=0) for digital signal.

#### `IOManager.read(channel_id)`
Read the current signal strength from an IO block (Output Mode).
*   **channel_id**: Int ID of the target IO channel.
*   **return**: Int signal strength (0-15).

#### `IOManager.isHigh(channel_id, threshold=7)`
Check if the signal on a channel is considered "High".
*   **channel_id**: Int ID.
*   **threshold**: Int threshold value (default 7).
*   **return**: Bool True if signal > threshold.

#### `IOManager.isLow(channel_id, threshold=7)`
Check if the signal on a channel is considered "Low".
*   **channel_id**: Int ID.
*   **threshold**: Int threshold value (default 7).
*   **return**: Bool True if signal <= threshold.