package here.lenrik.chili_map.map

import net.minecraft.util.math.Vec3i

data class MapMarker(var type: Type = Type.BLUE_MARKER, var pos: Vec3i, val rotation: Float) {


	enum class Type(val saveable: Boolean) {
		PLAYER(false),
		FRAME(false),
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
