@file:OptIn(ExperimentalUnsignedTypes::class)

package here.lenrik.chili_map.map

import com.google.common.collect.Iterables
import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multisets
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.MapColor
import net.minecraft.block.MapColor.Brightness
import net.minecraft.entity.Entity
import net.minecraft.util.math.*
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.floor

fun AreaMap(pos: Vec3d, zoomLevel: Int): AreaMap {
	return AreaMap(AreaMap.toMapPosAtZoomLevel(pos, zoomLevel), ByteArray(AreaMap.pixelCount))
}

class AreaMap(val pos: Vec3i, var colors: ByteArray) {
	companion object {
		const val pixelCount = 1 shl 14


		fun toMapPosAtZoomLevel(pos: Vec3d, zoomLevel: Int): Vec3i {

			val sideLength = 128 * (1 shl zoomLevel)
			return Vec3i(
				floor((pos.x + 64) / sideLength).toInt(),
				floor((pos.z + 64) / sideLength).toInt(),
				zoomLevel
			)
		}

		fun toTopLeftCorner(pos: Vec3i): Vec3d {
			val sideLength = 128 * (1 shl pos.z)
			return Vec3d(pos.x * sideLength - 64.0, 64.0, pos.y * sideLength - 64.0)
		}

		fun getFluidStateIfVisible(world: World, state: BlockState, pos: BlockPos): BlockState? {
			val fluidState = state.fluidState
			return if (!fluidState.isEmpty && !state.isSideSolidFullSquare(
					world,
					pos,
					Direction.UP
				)
			) fluidState.blockState else state
		}
	}

	private var updateTracker = 0

	var hasUpdated: Boolean = true

	fun isInsideMap(pos: Vec3d): Boolean {
		val sideLength = (1 shl this.pos.z) * 128
		val l = this.pos.x * sideLength - 64 <= pos.x
		val t = this.pos.y * sideLength - 64 <= pos.z
		val r = (this.pos.x + 1) * sideLength - 64 >= pos.x
		val b = (this.pos.y + 1) * sideLength - 64 >= pos.z
		return l && t && r && b
	}

	fun updateColors(world: World, entity: Entity) {
		val scale = 1 shl pos.z
		val mapCenter = toTopLeftCorner(pos) + Vec3d(64.0 * scale, 0.0, 64.0 * scale)
		val playerXRelativeMapCenter = MathHelper.floor(entity.x - mapCenter.x) / scale + 64
		val playerYRelativeMapCenter = MathHelper.floor(entity.z - mapCenter.z) / scale + 64
		var renderDistance = 128 / scale
		if (world.dimension.hasCeiling()) {
			renderDistance /= 2
		}

		++updateTracker
		var updatedLastPixel = false
		for (mapPixelX in playerXRelativeMapCenter - renderDistance + 1 until playerXRelativeMapCenter + renderDistance) {
			if (mapPixelX and 15 == updateTracker and 15 || updatedLastPixel) {
				updatedLastPixel = false
				var d = 0.0
				for (mapPixelY in playerYRelativeMapCenter - renderDistance - 1 until playerYRelativeMapCenter + renderDistance) {
					if (mapPixelX >= 0 && mapPixelY >= -1 && mapPixelX < 128 && mapPixelY < 128) {
						val pixelDX = mapPixelX - playerXRelativeMapCenter
						val pixelDY = mapPixelY - playerYRelativeMapCenter
						val insideBorder =
							pixelDX * pixelDX + pixelDY * pixelDY > (renderDistance/* - borderRadii*/) * (renderDistance/* - borderRadii*/)
						val pixelBlockX = ((mapCenter.x / scale + mapPixelX - 64) * scale).toInt()
						val pixelBlockZ = ((mapCenter.z / scale + mapPixelY - 64) * scale).toInt()
						val multiset = LinkedHashMultiset.create<MapColor>()
						val chunk = world.getWorldChunk(BlockPos(pixelBlockX, 0, pixelBlockZ))
						if (!chunk.isEmpty) {
							val chunkPos = chunk.pos
							val chunkX = pixelBlockX and 15
							val chunkZ = pixelBlockZ and 15
							var topFluidCount = 0
							var currentPixelHeight = 0.0
							val highestPos = BlockPos.Mutable()
							val fluidWalker = BlockPos.Mutable()
							for (oX in 0 until scale) {
								for (oZ in 0 until scale) {
									var highest = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, oX + chunkX, oZ + chunkZ) + 1
									var blockState: BlockState
									if (highest <= world.bottomY + 1) {
										blockState = Blocks.BEDROCK.defaultState
									} else {
										do {
											--highest
											highestPos.set(chunkPos.startX + oX + chunkX, highest, chunkPos.startZ + oZ + chunkZ)
											blockState = chunk.getBlockState(highestPos)
										} while (blockState.getMapColor(
												world,
												highestPos
											) === MapColor.CLEAR && highest > world.bottomY
										)
										if (highest > world.bottomY && !blockState.fluidState.isEmpty) {
											var lowest = highest - 1
											fluidWalker.set(highestPos)
											var blockState2: BlockState
											do {
												fluidWalker.y = lowest--
												blockState2 = chunk.getBlockState(fluidWalker)
												++topFluidCount
											} while (lowest > world.bottomY && !blockState2.fluidState.isEmpty)
											blockState = getFluidStateIfVisible(world, blockState, highestPos)!!
										}
									}
									currentPixelHeight += highest / (scale * scale)
									if (!world.dimension.hasCeiling() || world.registryKey === World.NETHER && blockState.block != Blocks.BEDROCK) {
										multiset.add(blockState.getMapColor(world, highestPos))
									} else {
										var randomNumber: Int = pixelBlockX + pixelBlockZ * 231871
										randomNumber = randomNumber * randomNumber * 31287121 + randomNumber * 11
										if (randomNumber shr 20 and 1 == 0) {
											multiset.add(Blocks.DIRT.defaultState.getMapColor(world, BlockPos.ORIGIN), 10)
										} else {
											multiset.add(Blocks.STONE.defaultState.getMapColor(world, BlockPos.ORIGIN), 100)
										}
									}
								}
							}
							topFluidCount /= scale * scale
							@Suppress("UnstableApiUsage") val color =
								Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR)
//								"https://open.spotify.com/track/45NoQeZGKVdNGN5FsX7im1?si=4587f908ae0a4ca1"/**/
							val brightness = if (color === MapColor.WATER_BLUE) {
								val y = topFluidCount * 0.1 + (mapPixelX + mapPixelY and 1) * 0.2
								if (y < 0.5) {
									Brightness.HIGH
								} else if (y > 0.9) {
									Brightness.LOW
								} else {
									Brightness.NORMAL
								}
							} else {
								val y = (currentPixelHeight - d) * 4.0 / (scale + 4) + ((mapPixelX + mapPixelY and 1) - 0.5) * 0.4
								if (y > 0.6) {
									Brightness.HIGH
								} else if (y < -0.6) {
									Brightness.LOW
								} else {
									Brightness.NORMAL
								}
							}
							d = currentPixelHeight
							if (mapPixelY >= 0 && pixelDX * pixelDX + pixelDY * pixelDY < renderDistance * renderDistance && (!insideBorder || mapPixelX + mapPixelY and 1 != 0)) {
								updatedLastPixel =
									putColor(mapPixelX, mapPixelY, color.getRenderColorByte(brightness)) or updatedLastPixel
								hasUpdated = hasUpdated or updatedLastPixel
							}
						}
					}
				}
			}
		}
	}

	override fun toString(): String {
		return "${pos.toShortString()}${if (isEmpty()) ", empty" else ""}"
	}


	fun putColor(x: Int, z: Int, color: Byte): Boolean {
		synchronized(colors) {
			val b = colors[x + z * 128]
			return if (b != color) {
				this.setColor(x, z, color)
				true
			} else {
				false
			}
		}
	}

	fun setColor(x: Int, z: Int, color: Byte) {
		synchronized(colors) {
			colors[x + z * 128] = color
		}
//		this.markDirty(x, z)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as AreaMap

		if (pos != other.pos) return false
		if (!colors.contentEquals(other.colors)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = pos.hashCode()
		result = 31 * result + colors.contentHashCode()
		return result
	}

	fun isEmpty(): Boolean {
		synchronized(colors) {
			return colors.reduce { b, c -> b or c } and 0b11_11_11_00.toByte() == 0.toByte()
		}
	}
}

operator fun Vec3d.plus(other: Vec3d): Vec3d {
	return add(other)
}
