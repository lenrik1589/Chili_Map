@file:OptIn(ExperimentalUnsignedTypes::class)

package here.lenrik.chili_map.map

import com.google.common.collect.Iterables
import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import com.google.common.collect.Multisets
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.MapColor
import net.minecraft.block.MapColor.Brightness
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.*
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.floor

fun AreaMap(pos: Vec3d): AreaMap {
	return AreaMap(AreaMap.toMapPosAtZoomLevel(pos, 0), ByteArray(AreaMap.pixelCount))
}

class AreaMap(val pos: Vec3i, var colors: ByteArray) {
	companion object {
		const val pixelCount = 1 shl 14

		fun toMapPosAtZoomLevel(pos: Vec3d, zoomLevel: Int): Vec3i {

			val sideLength = 128 * (1 shl zoomLevel)
			return Vec3i(
				(floor(pos.x + 64) / sideLength).toInt(),
				(floor(pos.z + 64) / sideLength).toInt(),
				zoomLevel
			)
		}

		fun toTopLeftCorner(pos: Vec3i): Vec3d {
			val sideLength = 128 * (1 shl pos.z)
			return Vec3d(pos.x * sideLength - 64.0, 64.0, pos.z * sideLength - 64.0)
		}
	}

	private var updateTracker = 0

	private val borderRadii = 2

	fun isInsideMap(pos: Vec3d): Boolean {
		val topLeftCorner = toTopLeftCorner(this.pos)
		return Box(
			topLeftCorner.add(0.0, Double.NEGATIVE_INFINITY, 0.0),
			topLeftCorner.add(128.0 * (1 shl this.pos.z), Double.POSITIVE_INFINITY, 128.0 * (1 shl this.pos.z))
		).contains(pos)
	}

	fun updateColors(world: World, entity: Entity) {
		if (/*world.registryKey === state.dimension*/ true && entity is PlayerEntity) {
			val scale = 1 shl pos.z
			val mapCenter = toTopLeftCorner(pos) + Vec3d(64.0, 64.0, 0.0)
			val playerXRelativeMapCenter = MathHelper.floor(entity.getX() - mapCenter.x) / scale + 64
			val playerYRelativeMapCenter = MathHelper.floor(entity.getZ() - mapCenter.y) / scale + 64
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
							val insideBorder = pixelDX * pixelDX + pixelDY * pixelDY > (renderDistance - borderRadii) * (renderDistance - borderRadii)
							val pixelBlockX = ((mapCenter.x / scale + mapPixelX - 64) * scale).toInt()
							val pixelBlockZ = ((mapCenter.y / scale + mapPixelY - 64) * scale).toInt()
							val multiset: Multiset<MapColor> = LinkedHashMultiset.create()
							val chunk = world.getWorldChunk(BlockPos(pixelBlockX, 0, pixelBlockZ))
							if (!chunk.isEmpty) {
								val chunkPos = chunk.pos
								val chunkX = pixelBlockX and 15
								val chunkZ = pixelBlockZ and 15
								var w = 0
								var e = 0.0
								if (world.dimension.hasCeiling()) {
									var random = pixelBlockX + pixelBlockZ * 231871
									random = random * random * 31287121 + random * 11
									if (random shr 20 and 1 == 0) {
										multiset.add(Blocks.DIRT.defaultState.getMapColor(world, BlockPos.ORIGIN), 10)
									} else {
										multiset.add(Blocks.STONE.defaultState.getMapColor(world, BlockPos.ORIGIN), 100)
									}
									e = 100.0
								} else {
									val blockPos1 = BlockPos.Mutable()
									val blockPos2 = BlockPos.Mutable()
									for (oX in 0 until scale) {
										for (oZ in 0 until scale) {
											var highest = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, oX + chunkX, oZ + chunkZ) + 1
											var blockState: BlockState
											if (highest <= world.bottomY + 1) {
												blockState = Blocks.BEDROCK.defaultState
											} else {
												do {
													--highest
													blockPos1.set(chunkPos.startX + oX + chunkX, highest, chunkPos.startZ + oZ + chunkZ)
													blockState = chunk.getBlockState(blockPos1)
												} while (blockState.getMapColor(world, blockPos1) === MapColor.CLEAR && highest > world.bottomY)
												if (highest > world.bottomY && !blockState.fluidState.isEmpty) {
													var ab = highest - 1
													blockPos2.set(blockPos1)
													var blockState2: BlockState
													do {
														blockPos2.y = ab--
														blockState2 = chunk.getBlockState(blockPos2)
														++w
													} while (ab > world.bottomY && !blockState2.fluidState.isEmpty)
													blockState = this.getFluidStateIfVisible(world, blockState, blockPos1)!!
												}
											}
//											state.removeBanner(world, chunkPos.startX + oX + chunkX, chunkPos.startZ + oZ + chunkZ)
											e += highest / (scale * scale)
											multiset.add(blockState.getMapColor(world, blockPos1))
										}
									}
								}
								w /= scale * scale
								val color = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR)
								"https://open.spotify.com/track/45NoQeZGKVdNGN5FsX7im1?si=4587f908ae0a4ca1"
								val brightness = if (color === MapColor.WATER_BLUE) {
									val y = w * 0.1 + (mapPixelX + mapPixelY and 1) * 0.2
									if (y < 0.5) {
										Brightness.HIGH
									} else if (y > 0.9) {
										Brightness.LOW
									} else {
										Brightness.NORMAL
									}
								} else {
									val y = (e - d) * 4.0 / (scale + 4) + ((mapPixelX + mapPixelY and 1) - 0.5) * 0.4
									if (y > 0.6) {
										Brightness.HIGH
									} else if (y < -0.6) {
										Brightness.LOW
									} else {
										Brightness.NORMAL
									}
								}
								d = e
								if (mapPixelY >= 0 && pixelDX * pixelDX + pixelDY * pixelDY < renderDistance * renderDistance && (!insideBorder || mapPixelX + mapPixelY and 1 != 0)) {
									updatedLastPixel = updatedLastPixel or putColor(mapPixelX, mapPixelY, color.getRenderColorByte(brightness))
								}
							}
						}
					}
				}
			}
		}
	}

	private fun getFluidStateIfVisible(world: World, state: BlockState, pos: BlockPos): BlockState? {
		val fluidState = state.fluidState
		return if (!fluidState.isEmpty && !state.isSideSolidFullSquare(
				world,
				pos,
				Direction.UP
			)
		) fluidState.blockState else state
	}

	/**
	 * Sets the color at the specified coordinates if the current color is different.
	 *
	 * @return `true` if the color has been updated, else `false`
	 */
	fun putColor(x: Int, z: Int, color: Byte): Boolean {
		val b = colors[x + z * 128]
		return if (b != color) {
			this.setColor(x, z, color)
			true
		} else {
			false
		}
	}

	fun setColor(x: Int, z: Int, color: Byte) {
		colors[x + z * 128] = color
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
		return colors.reduce(fun(c: Byte, b: Byte) = c or b) and 0b111111_00.toByte() == 0.toByte()
	}
}

private operator fun Vec3d.plus(other: Vec3d): Vec3d {
	return add(other)
}
