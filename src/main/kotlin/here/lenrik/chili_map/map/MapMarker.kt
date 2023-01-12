package here.lenrik.chili_map.map

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

data class MapMarker(
	var type: Type = Type.BLUE_MARKER,
	var pos: Vec3d,
	val rotation: Float,
	var name: MutableText? = Text.of("").copy()
) {
	companion object {
		fun fromNbt(nbt: NbtCompound): MapMarker {
			return MapMarker(
				Type.values()[nbt.getByte("type").toInt()],
				nbt.getLongArray("pos").map(Double.Companion::fromBits).let { Vec3d(it[0], it[1], it[2]) },
				nbt.getFloat("rot"),
				Text.Serializer.fromJson(nbt.getString("name"))
			)
		}
	}

	fun asNbt(): NbtElement {
		val nbt = NbtCompound()
		nbt.putByte("type", type.ordinal.toByte())
		nbt.putLongArray("pos", listOf(pos.x, pos.y, pos.z).map(Double::toRawBits))
		nbt.putFloat("rot", rotation)
		nbt.putString("name", Text.Serializer.toJson(name))
		return nbt
	}


	enum class Type(val saveable: Boolean) {
		SELF(false),
		OTHER(false),
		RED_MARKER(true),
		BLUE_MARKER(true),
		TARGET_X(true),
		TARGET_POINT(true),
		PLAYER_OFF_MAP(false),
		PLAYER_OFF_LIMITS(false),
		MANSION(true),
		MONUMENT(true),
		BANNER_WHITE(true),
		BANNER_ORANGE(true),
		BANNER_MAGENTA(true),
		BANNER_LIGHT_BLUE(true),
		BANNER_YELLOW(true),
		BANNER_LIME(true),
		BANNER_PINK(true),
		BANNER_GRAY(true),
		BANNER_LIGHT_GRAY(true),
		BANNER_CYAN(true),
		BANNER_PURPLE(true),
		BANNER_BLUE(true),
		BANNER_BROWN(true),
		BANNER_GREEN(true),
		BANNER_RED(true),
		BANNER_BLACK(true),
		RED_X(true);

		var id = -1

		init {
			id = ordinal
		}
	}
}
