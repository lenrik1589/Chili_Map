package here.lenrik.chili_map.map

import here.lenrik.chili_map.Vec2i
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import java.nio.file.Path
import kotlin.math.floor

data class LevelMap(val levelId: Identifier, val name: Text) {
	companion object {
		val boundary = Vec2i(8192, 8192)
	}

	val regions = HashMap<Vec2i, MapRegion>()

	fun getRegion(mapPos: Vec3i): MapRegion {
		return regions.compute(
			Vec2i(
				floor(mapPos.x.toDouble() * ((1 shl mapPos.z) * 128) / boundary.x).toInt(),
				floor(mapPos.y.toDouble() * ((1 shl mapPos.z) * 128) / boundary.y).toInt()
			)
		) { vec2, region -> region ?: MapRegion(vec2) }!!
	}

	fun getMap(pos: Vec3d, zoomLevel: Int): AreaMap {
		val mapPos = AreaMap.toMapPosAtZoomLevel(pos, zoomLevel)
		return getMap(mapPos)
	}

	fun getMap(pos: Vec3i): AreaMap {
		return getRegion(pos).getMap(pos)
	}

	fun save(path: Path) {
		path.toFile().mkdirs()
		for ((pos, region) in regions) {
			val regionCompound = NbtCompound()
			regionCompound["x"] = pos.x
			regionCompound["y"] = pos.y
			val areas = NbtCompound()
			for ((areaPos, area) in region.areas) if (!area.isEmpty()) {
				areas["${areaPos.x}_${areaPos.y}_${areaPos.z}"] = area.colors
			}
			regionCompound["areas"] = areas
			NbtIo.write(regionCompound, path.resolve("${pos.x}_${pos.y}.nbt").toFile())
		}
	}

	fun load(path: Path) {
		println(
			"loaded ${
				path.toFile().listFiles { file ->
//					println(file.absolutePath)
					val pos = with(file.nameWithoutExtension.split("_")) { Vec2i(this[0].toInt(), this[1].toInt()) }
					val regionData = NbtIo.read(file)
					val region = MapRegion(pos)
					for (arePos in regionData!!.getCompound("areas").keys) {
						region.addAreaMap(
							with(arePos.split("_")) { Vec3i(this[0].toInt(), this[1].toInt(), this[2].toInt()) },
							regionData.getCompound("areas").getByteArray(arePos)
						)
					}
//					println("region contains ${region.areas.size} maps")
					if(region.areas.size == 0){
						false
					}	else {
						regions[pos] = region
						true
					}
				}?.size
			} of ${path.toFile().listFiles()?.size} files (other region files are empty LOL)"
		)
	}

	inner class MapRegion(val pos: Vec2i) {
		val areas = HashMap<Vec3i, AreaMap>()

		fun getMap(mapPos: Vec3i): AreaMap {
			return areas.compute(
				mapPos
			) { vec3i, map -> map ?: AreaMap(vec3i, ByteArray(AreaMap.pixelCount)) }!!
		}

		@Suppress("unused")
		fun isMapInside(map: AreaMap): Boolean {
			return isMapInside(map.pos)
		}

		fun isMapInside(pos: Vec3i): Boolean {
			val sideLength = (1 shl pos.z) * 128
			return this.pos.x * boundary.x <= pos.x * sideLength &&
					this.pos.y * boundary.y <= pos.y * sideLength &&
					(this.pos.x + 1) * boundary.x >= (pos.x + 1) * sideLength &&
					(this.pos.y + 1) * boundary.y >= (pos.y + 1) * sideLength
		}

		fun addAreaMap(pos: Vec3i, colors: ByteArray) {
			if (!isMapInside(pos)) throw IndexOutOfBoundsException("map with coordinates ${pos.toShortString()} is out of bounds for ${this.pos.toShortString()}")
			areas[pos] = AreaMap(pos, colors)
		}

	}
}
