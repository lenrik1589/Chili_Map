package here.lenrik.chili_map.map

import here.lenrik.chili_map.Vec2i
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.nio.file.Path
import java.time.Instant
import java.util.*

data class MapContainer(var name: Text, val savePath: Path) {
	constructor(name: String, savePath: Path) : this(Text.of(name), savePath)

	private val levels: HashMap<Identifier, LevelMap> = HashMap(4)
	private val markers = mutableListOf<MapMarker>()
	private val createdAt: Long

	init {
		val data = getProperties()
		createdAt = if (data.getLong("createTime") == 0L) data.getLong("saveTime") else data.getLong("createTime")
		load(data)
	}

	fun getLevel(value: Identifier): LevelMap {
		return levels.compute(
			value
		) { identifier, levelMap -> levelMap ?: LevelMap(identifier, name.copy() + "/" + identifier.toString()) }!!
	}

	fun save(autoSave: Boolean = false) {
		val path = if (autoSave) savePath.resolve("autoSave") else savePath
		path.toFile().mkdirs()
		val propertiesNbt = NbtCompound()
		propertiesNbt["createTime"] = createdAt
		propertiesNbt["saveTime"] = Instant.now().toEpochMilli()
		propertiesNbt["name"] = name
		propertiesNbt["autoSave"] = autoSave
		val levelsList = NbtList()
		for ((id, level) in levels) {
			val resolved = Path.of(id.toString().replace(":", "/"))
			level.save(path.resolve(resolved))
			val levelNbt = NbtCompound()
			levelNbt["id"] = id.toString()
			levelNbt["path"] = resolved.toString()
			levelNbt["name"] = level.name
			levelsList.add(levelNbt)
		}
		propertiesNbt["levels"] = levelsList
		NbtIo.write(propertiesNbt, path.resolve("properties.nbt").toFile())
	}

	fun load(data: NbtCompound = getProperties()) {
		if ("levels" in data) {
			for (levelNbt in data["levels"] as NbtList) {
				levelNbt as NbtCompound
				LevelMap(Identifier(levelNbt.getString("id")), Text.of(levelNbt.getString("name"))).apply {
					load(
						savePath.resolve(if (data.getBoolean("autoSave")) "autoSave" else "").resolve(levelNbt.getString("path"))
					)
				}.also { levels[it.levelId] = it }
			}
		}
	}

	private fun getProperties(): NbtCompound {
		val autoSaveData = NbtIo.read(savePath.resolve("autoSave").resolve("properties.nbt").toFile())
		val saveData = NbtIo.read(savePath.resolve("properties.nbt").toFile())
		return when {
			saveData == null && autoSaveData == null -> NbtCompound().apply {
				this["createTime"] = Instant.now().toEpochMilli()
				this["name"] = name
			}
			(saveData == null) xor (autoSaveData == null) -> (saveData ?: autoSaveData)!!
			else -> (if (autoSaveData!!.getLong("saveTime") > saveData!!.getLong("saveTime")) autoSaveData else saveData)
		}
	}

	fun getMarkers(from: Vec2i, to: Vec2i): MutableList<MapMarker> {
		return markers.filter {
			from.x <= it.pos.x && it.pos.x < to.x && from.y <= it.pos.y && it.pos.y < to.y
		}.toMutableList()
	}

}

operator fun NbtCompound.set(key: String, value: Any) {
	when (value) {
		is Long -> putLong(key, value)
		is Int -> putInt(key, value)
		is Short -> putShort(key, value)
		is Byte -> putByte(key, value)
		is Boolean -> putBoolean(key, value)
		is String -> putString(key, value)
		is UUID -> putUuid(key, value)
		is LongArray -> putLongArray(key, value)
		is IntArray -> putIntArray(key, value)
		is ByteArray -> putByteArray(key, value)
		is Float -> putFloat(key, value)
		is Double -> putDouble(key, value)
		is NbtElement -> put(key, value)
		is Text -> putString(key, with(value) {
			val builder = StringBuilder()
			this.visit<Any> { value ->
				builder.append(value)
				Optional.empty()
			}
			builder.toString()
		})
		else -> TODO("Need to figure out how to (de)serialize \"stuff\" ${value.javaClass}")
	}
}

private operator fun MutableText.plus(next: String): MutableText {
	return append(next)
}
