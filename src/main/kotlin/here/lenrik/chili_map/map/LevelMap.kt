package here.lenrik.chili_map.map

import com.google.common.collect.Iterables
import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import com.google.common.collect.Multisets
import here.lenrik.chili_map.Vec2i
import here.lenrik.chili_map.client.ChilliMapClient
import here.lenrik.chili_map.client.ChilliMapClient.Companion.renderMode
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.MapColor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import java.nio.file.Path
import kotlin.math.floor

private operator fun Vec3d.minus(other: Vec3d): Vec3d = Vec3d(this.x - other.x, this.y - other.y, this.z - other.z)

data class LevelMap(val levelId: Identifier, val name: Text) {
	companion object {
		val boundary = Vec2i(8192, 8192)
	}

	private var updateTracker: Int = 0
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
		for((pos, region) in regions) {
			val regionCompound = NbtCompound()
			regionCompound["x"] = pos.x
			regionCompound["y"] = pos.y
			val areas = NbtCompound()
			for((areaPos, area) in region.areas) if(!area.isEmpty()) {
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
					for(arePos in regionData!!.getCompound("areas").keys) {
						region.addAreaMap(
							with(arePos.split("_")) { Vec3i(this[0].toInt(), this[1].toInt(), this[2].toInt()) },
							regionData.getCompound("areas").getByteArray(arePos)
						)
					}
//					println("region contains ${region.areas.size} maps")
					if(region.areas.size == 0) {
						false
					} else {
						regions[pos] = region
						true
					}
				}?.size
			} of ${path.toFile().listFiles()?.size} files (other region files are empty LOL)"
		)
	}

	fun updateMaps(world: World, player: Entity) {
		world as ClientWorld
		updateTracker++
		for(z in 0..4) {
			val scale = 1 shl z
			val strips = 16 shr z
			when (renderMode) {
				ChilliMapClient.RenderMode.Vanilla -> {
					val centerPixelX = 1
					val centerPixelZ = 1
//					for ()
					TODO("Need to figure out tear-free rendering")
				}
				ChilliMapClient.RenderMode.All -> {
					val center = player.chunkPos
					val chunkDistance = MinecraftClient.getInstance().options.viewDistance

					for(chunkX in center.x - chunkDistance..center.x + chunkDistance) {
						if((updateTracker xor chunkX) and 0xf == 0) {
							val d = Array(strips) { Double.NaN }
							for(chunkZ in center.z - chunkDistance..center.z + chunkDistance) {
								val multiset: Multiset<MapColor> = LinkedHashMultiset.create()

								(world.chunkManager.getChunk(chunkX, chunkZ) as Chunk?)?.let { chunk ->
									val chunkPos = chunk.pos
									val map = getMap(Vec3d(chunkPos.startX + .0, .0, chunkPos.startZ + .0), z)

									for(cX in 0 until strips) {
										for(cZ in 0 until strips) {
											var topFluidCount = 0
											var currentPixelHeight = 0.0
											val highestPos = BlockPos.Mutable()
											val fluidWalker = BlockPos.Mutable()
											val mapPixelX = ((chunkX * strips % 128 + cX + 4 * strips) + 128 * scale) % 128
											val mapPixelY = ((chunkZ * strips % 128 + cZ + 4 * strips) + 128 * scale) % 128

											for(oX in cX * scale until (cX + 1) * scale) {
												for(oZ in cZ * scale until (cZ + 1) * scale) {
													var highest = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, oX, oZ) + 1
													var blockState: BlockState
													if(highest <= world.bottomY + 1) {
														blockState = Blocks.BEDROCK.defaultState
													} else {
														do {
															--highest
															highestPos.set(chunkPos.startX + oX, highest, chunkPos.startZ + oZ)
															blockState = chunk.getBlockState(highestPos)
														} while(blockState.getMapColor(
																world,
																highestPos
															) === MapColor.CLEAR && highest > world.bottomY
														)
														if(highest > world.bottomY && !blockState.fluidState.isEmpty) {
															var lowest = highest - 1
															fluidWalker.set(highestPos)
															var blockState2: BlockState
															do {
																fluidWalker.y = lowest--
																blockState2 = chunk.getBlockState(fluidWalker)
																++topFluidCount
															} while(lowest > world.bottomY && !blockState2.fluidState.isEmpty)
															blockState = AreaMap.getFluidStateIfVisible(world, blockState, highestPos)!!
														}
													}
													currentPixelHeight += highest / (scale * scale)
													if(!world.dimension.hasCeiling() || world.registryKey === World.NETHER && blockState.block != Blocks.BEDROCK) {
														multiset.add(blockState.getMapColor(world, highestPos))
													} else {
														var randomNumber: Int = chunkPos.startX + oX + (chunkPos.startZ + oZ) * 231871
														randomNumber = randomNumber * randomNumber * 31287121 + randomNumber * 11
														if(randomNumber shr 20 and 1 == 0) {
															multiset.add(Blocks.DIRT.defaultState.getMapColor(world, BlockPos.ORIGIN), 10)
														} else {
															multiset.add(Blocks.STONE.defaultState.getMapColor(world, BlockPos.ORIGIN), 100)
														}
													}
												}
											}
											topFluidCount /= scale * scale
											@Suppress("UnstableApiUsage") val color = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR)
											multiset.clear()
											val brightness = if(color === MapColor.WATER_BLUE) {
												val y = topFluidCount * 0.1 + (mapPixelX + mapPixelY and 1) * 0.2
												when {
													y < 0.5 -> MapColor.Brightness.HIGH
													y > 0.9 -> MapColor.Brightness.LOW
													else -> MapColor.Brightness.NORMAL
												}
											} else {
												if(d[cX] == (Double.NaN as Number)) {
													d[cX] = currentPixelHeight
													continue
												}
												val y = (currentPixelHeight - d[cX]) * 4.0 / (scale + 4) + ((mapPixelX + mapPixelY and 1) - 0.5) * 0.4
												when {
													y > 0.6 -> MapColor.Brightness.HIGH
													y < -0.6 -> MapColor.Brightness.LOW
													else -> MapColor.Brightness.NORMAL
												}
											}
											d[cX] = currentPixelHeight
											map.hasUpdated = map.hasUpdated or map.putColor(mapPixelX, mapPixelY, color.getRenderColorByte(brightness))
										}
									}

								} ?: d.fill(Double.NaN, 0, strips)
							}
						}
					}
				}
			}
//			val renderDistance = ChilliMapClient.baseRenderDistance / scale
//			val mapsTopLeft     = AreaMap.toTopLeftCorner(AreaMap.toMapPosAtZoomLevel(player.pos.add(.0 - renderDistance, .0, .0 - renderDistance), z))
//			val mapsBottomRight = AreaMap.toTopLeftCorner(AreaMap.toMapPosAtZoomLevel(player.pos.add(.0 + renderDistance, .0, .0 + renderDistance), z))
//			val size = mapsBottomRight - mapsTopLeft;
//			val playerXRelativeMap0_0 = MathHelper.floor(player.x) / scale
//			val playerYRelativeMap0_0 = MathHelper.floor(player.z) / scale
//			var updatedLastPixel = false
//			for(mapPixelX in mapsTopLeft.x.toInt() .. mapsBottomRight.x.toInt()){
//				if (mapPixelX and 15 == updateTracker and 15 || updatedLastPixel) {
//					for(mapPixelY in mapsTopLeft.z.toInt()..mapsBottomRight.z.toInt()) {
//						if((playerXRelativeMap0_0 - mapPixelX) * (playerXRelativeMap0_0 - mapPixelX) + (playerXRelativeMap0_0 - mapPixelX) * (playerXRelativeMap0_0 - mapPixelX) < (renderDistance - borderRadii) * (renderDistance - borderRadii)){
//
//						}
//					}
//				}
//			}
		}
//		val mapCenter = AreaMap.toTopLeftCorner(pos) + Vec3d(64.0 * scale, 0.0, 64.0 * scale)
//		val playerXRelativeMapCenter = MathHelper.floor(player.getX() - mapCenter.x) / scale + 64
//		val playerYRelativeMapCenter = MathHelper.floor(player.getZ() - mapCenter.z) / scale + 64
//		if(world.dimension.hasCeiling()) {
//			renderDistance /= 2
//		}
//
//		++updateTracker
//		var updatedLastPixel = false
//		for(mapPixelX in playerXRelativeMapCenter - renderDistance + 1 until playerXRelativeMapCenter + renderDistance) {
//			if(mapPixelX and 15 == updateTracker and 15 || updatedLastPixel) {
//				updatedLastPixel = false
//				var d = 0.0
//				for(mapPixelY in playerYRelativeMapCenter - renderDistance - 1 until playerYRelativeMapCenter + renderDistance) {
//					if(mapPixelX >= 0 && mapPixelY >= -1 && mapPixelX < 128 && mapPixelY < 128) {
//						val pixelDX = mapPixelX - playerXRelativeMapCenter
//						val pixelDY = mapPixelY - playerYRelativeMapCenter
//						val insideBorder =
//							pixelDX * pixelDX + pixelDY * pixelDY > (renderDistance - borderRadii) * (renderDistance - borderRadii)
//						val pixelBlockX = ((mapCenter.x / scale + mapPixelX - 64) * scale).toInt()
//						val pixelBlockZ = ((mapCenter.z / scale + mapPixelY - 64) * scale).toInt()
//						val multiset: Multiset<MapColor> = LinkedHashMultiset.create()
//						val chunk = world.getWorldChunk(BlockPos(pixelBlockX, 0, pixelBlockZ))
//						if(!chunk.isEmpty) {
//							val chunkPos = chunk.pos
//							val chunkX = pixelBlockX and 15
//							val chunkZ = pixelBlockZ and 15
//							var topFluidCount = 0
//							var currentPixelHeight = 0.0
//							val highestPos = BlockPos.Mutable()
//							val fluidWalker = BlockPos.Mutable()
//							for(oX in 0 until scale) {
//								for(oZ in 0 until scale) {
//									var highest = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, oX + chunkX, oZ + chunkZ) + 1
//									var blockState: BlockState
//									if(highest <= world.bottomY + 1) {
//										blockState = Blocks.BEDROCK.defaultState
//									} else {
//										do {
//											--highest
//											highestPos.set(chunkPos.startX + oX + chunkX, highest, chunkPos.startZ + oZ + chunkZ)
//											blockState = chunk.getBlockState(highestPos)
//										} while(blockState.getMapColor(
//												world,
//												highestPos
//											) === MapColor.CLEAR && highest > world.bottomY
//										)
//										if(highest > world.bottomY && !blockState.fluidState.isEmpty) {
//											var lowest = highest - 1
//											fluidWalker.set(highestPos)
//											var blockState2: BlockState
//											do {
//												fluidWalker.y = lowest--
//												blockState2 = chunk.getBlockState(fluidWalker)
//												++topFluidCount
//											} while(lowest > world.bottomY && !blockState2.fluidState.isEmpty)
//											blockState = AreaMap.getFluidStateIfVisible(world, blockState, highestPos)!!
//										}
//									}
//									currentPixelHeight += highest / (scale * scale)
//									if(!world.dimension.hasCeiling() || world.registryKey === World.NETHER && blockState.block != Blocks.BEDROCK) {
//										multiset.add(blockState.getMapColor(world, highestPos))
//									} else {
//										var randomNumber: Int = pixelBlockX + pixelBlockZ * 231871
//										randomNumber = randomNumber * randomNumber * 31287121 + randomNumber * 11
//										if(randomNumber shr 20 and 1 == 0) {
//											multiset.add(Blocks.DIRT.defaultState.getMapColor(world, BlockPos.ORIGIN), 10)
//										} else {
//											multiset.add(Blocks.STONE.defaultState.getMapColor(world, BlockPos.ORIGIN), 100)
//										}
//									}
//								}
//							}
//							topFluidCount /= scale * scale
//							@Suppress("UnstableApiUsage") val color = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR)
////								"https://open.spotify.com/track/45NoQeZGKVdNGN5FsX7im1?si=4587f908ae0a4ca1"/**/
//							val brightness = if(color === MapColor.WATER_BLUE) {
//								val y = topFluidCount * 0.1 + (mapPixelX + mapPixelY and 1) * 0.2
//								if(y < 0.5) {
//									MapColor.Brightness.HIGH
//								} else if(y > 0.9) {
//									MapColor.Brightness.LOW
//								} else {
//									MapColor.Brightness.NORMAL
//								}
//							} else {
//								val y = (currentPixelHeight - d) * 4.0 / (scale + 4) + ((mapPixelX + mapPixelY and 1) - 0.5) * 0.4
//								if(y > 0.6) {
//									MapColor.Brightness.HIGH
//								} else if(y < -0.6) {
//									MapColor.Brightness.LOW
//								} else {
//									MapColor.Brightness.NORMAL
//								}
//							}
//							d = currentPixelHeight
//							if(mapPixelY >= 0 && pixelDX * pixelDX + pixelDY * pixelDY < renderDistance * renderDistance && (!insideBorder || mapPixelX + mapPixelY and 1 != 0)) {
//								updatedLastPixel = putColor(mapPixelX, mapPixelY, color.getRenderColorByte(brightness)) or updatedLastPixel
//								hasUpdated = hasUpdated or updatedLastPixel
//							}
//						}
//					}
//				}
//			}
//		}
//		TODO("Not yet implemented")
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
			if(!isMapInside(pos)) throw IndexOutOfBoundsException("map with coordinates ${pos.toShortString()} is out of bounds for ${this.pos.toShortString()}")
			areas[pos] = AreaMap(pos, colors)
		}

		fun isEmpty() = when (areas.values.filter { !it.isEmpty() }) {
			listOf<AreaMap>() -> true; else -> false
		}

	}
}
