package here.lenrik.chili_map

import java.util.*

data class Vec2i(val x: Int, val y: Int) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || javaClass != other.javaClass) return false
		val vec2i = other as Vec2i
		return x == vec2i.x && y == vec2i.y
	}

	override fun hashCode(): Int {
		return Objects.hash(x, y)
	}

	override fun toString(): String {
		return "Vec2i{x=$x, y=$y}"
	}

	operator fun plus(other: Vec2i): Vec2i {
		return Vec2i(x + other.x, y + other.y)
	}

	fun plus(otherX: Int, otherY: Int): Vec2i {
		return Vec2i(x + otherX, y + otherY)
	}

	operator fun minus(other: Vec2i): Vec2i {
		return this + (other * -1)
	}

	operator fun times(other: Vec2i): Vec2i {
		return Vec2i(x * other.x, y * other.y)
	}

	operator fun times(scale: Int): Vec2i {
		return Vec2i(x * scale, y * scale)
	}

	operator fun div(other: Any?) = when(other){
		is Vec2i -> Vec2i(this.x / other.x, this.y / other.y)
		is Int -> Vec2i(this.x / other, this.y / other)
		else -> TODO("")
	}

	fun toShortString(): String {
		return "$x, $y"
	}

}