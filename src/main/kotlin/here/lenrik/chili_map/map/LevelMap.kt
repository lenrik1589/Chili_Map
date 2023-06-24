package here.lenrik.chili_map.map

import com.google.common.collect.*
import here.lenrik.chili_map.ChiliMap
import here.lenrik.chili_map.Vec2i
import here.lenrik.chili_map.client.ChiliMapClient.Companion.config
import here.lenrik.chili_map.client.config.ChiliMapClientConfig
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.MapColor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.ChunkStatus
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.Double.Companion.NaN
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.roundToInt

operator fun Vec3d.minus(other: Vec3d): Vec3d = Vec3d(this.x - other.x, this.y - other.y, this.z - other.z)

data class LevelMap(val levelId: Identifier, val name: Text) {
	companion object {
		val boundary = Vec2i(8192, 8192)
	}

	private val markers = mutableListOf<MapMarker>()
	var stop: Boolean = false
		get() {
			when (Thread.currentThread()) {
				workerThread -> {
				}

				else         -> {
					ChiliMap.LOGGER.info("accessed stop")
					field = true
					workerThread.join()
				}
			}
			return field
		}
	val dirtColor: MapColor = Blocks.DIRT.defaultState.getMapColor(null, BlockPos.ORIGIN)
	val stoneColor: MapColor = Blocks.STONE.defaultState.getMapColor(null, BlockPos.ORIGIN)
	val queue: ConcurrentLinkedQueue<Pair<World, PlayerEntity>> = Queues.newConcurrentLinkedQueue()
	val workerThread = thread(start = true, isDaemon = true, name = "Map Update Thread") {
		while (!stop) {
			when (queue.size) {
				0    -> Thread.sleep(25)
				else -> {
					val e = queue.peek()
					updateMaps(e.first, e.second)
					queue.poll()
				}
			}
		}
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
		val markerList = NbtList()
		markers.filter { it.type.saveable }.forEach { marker ->
			markerList.add(marker.asNbt())
		}
		NbtIo.write(NbtCompound().apply { set("markers", markerList) }, path.resolve("markers.nbt").toFile())
	}

	fun load(path: Path) {
		println(
			"loaded ${
				path.toFile().listFiles { file ->
					when (file.nameWithoutExtension) {
						"markers" -> {
							val list = NbtIo.read(file)?.get("markers")
							for (marker in list as NbtList) {
								markers.add(MapMarker.fromNbt(marker as NbtCompound))
							}
							true
						}

						else      -> {
							val pos = with(file.nameWithoutExtension.split("_")) { Vec2i(this[0].toInt(), this[1].toInt()) }
							val regionData = NbtIo.read(file)
							val region = MapRegion(pos)
							for (arePos in regionData!!.getCompound("areas").keys) {
								region.addAreaMap(
									with(arePos.split("_")) { Vec3i(this[0].toInt(), this[1].toInt(), this[2].toInt()) },
									regionData.getCompound("areas").getByteArray(arePos)
								)
							}
							!(region.areas.size == 0 || false.also { regions[pos] = region })
						}
					}
//					if (region.areas.size == 0) {
//						false
//					} else {
//						regions[pos] = region
//						true
//					}
				}?.size
			} of ${path.toFile().listFiles()?.size} files (other region files are empty LOL)"
		)
	}

	fun updateMaps(world: World, player: Entity) {
		if (Thread.currentThread() != workerThread) {
			if (queue.isEmpty()) {
				queue.add(world to player as PlayerEntity)
			}
		} else {
			world as ClientWorld
			updateTracker++
			for (z in 0..4) {
				val scale = 1 shl z
				val strips = 16 shr z
				val scaleSquare = scale * scale
				val topFluidLimit = config.waterMode.limit * scaleSquare
				when (config.mappingMode) {
					ChiliMapClientConfig.MappingMode.Vanilla -> {
						val chunkSelector = BlockPos.Mutable()
						val centerPixelX = player.x.roundToInt() / scale
						val centerPixelY = player.z.roundToInt() / scale
						val renderDistance = config.baseRenderDistance / scale / if (world.dimension.hasCeiling()) 2 else 1

						var updatedLastPixel = false
						exit@ for (mapPixelX in centerPixelX - renderDistance + 1 until centerPixelX + renderDistance) {
							if ((mapPixelX xor updateTracker) and 0xf == 0) {
								val pixelDX = mapPixelX - centerPixelX
								var d = NaN
								for (mapPixelY in centerPixelY - renderDistance - 1 until centerPixelY + renderDistance) {
									val pixelDY = mapPixelY - centerPixelY
									val insideBorder =
										pixelDY * pixelDY + pixelDX * pixelDX > (renderDistance - config.borderRadius) * (renderDistance - config.borderRadius)
									val pixelBlockX = mapPixelX * scale
									val pixelBlockZ = mapPixelY * scale
									val multiset = LinkedHashMultiset.create<MapColor>()
									val chunk = world.getWorldChunk(chunkSelector.set(pixelBlockX, 0, pixelBlockZ))
									if (/*mapPixelY >= 0 && pixelDX * pixelDX + pixelDY * pixelDY < renderDistance * renderDistance && */!chunk.isEmpty) {
										val chunkPos = chunk.pos
										val chunkX = pixelBlockX and 15
										val chunkZ = pixelBlockZ and 15
										var topFluidCount = 0
										var currentPixelHeight = 0.0
										val highestPos = BlockPos.Mutable()
										val fluidWalker = BlockPos.Mutable()

										for (oX in 0 until scale) {
											for (oZ in 0 until scale) {
												if (stop) break@exit
												var highest = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, oX + chunkX, oZ + chunkZ) + 1
												var blockState: BlockState
												if (highest <= world.bottomY + 1) {
													blockState = Blocks.BEDROCK.defaultState
												} else {
													do {
														if (stop) break@exit
														--highest
														highestPos.set(pixelBlockX + oX, highest, pixelBlockZ + oZ)
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
															if (stop) break@exit
															fluidWalker.y = lowest--
															blockState2 = chunk.getBlockState(fluidWalker)
															++topFluidCount
														} while (topFluidCount < topFluidLimit && lowest > world.bottomY && !blockState2.fluidState.isEmpty)
														blockState = AreaMap.getFluidStateIfVisible(world, blockState, highestPos)!!
													}
												}
												currentPixelHeight += highest
												if (!world.dimension.hasCeiling() || world.registryKey === World.NETHER && blockState.block != Blocks.BEDROCK) {
													multiset.add(blockState.getMapColor(world, highestPos))
												} else {
													var randomNumber: Int = pixelBlockX + (pixelBlockZ) * 231871
													randomNumber = randomNumber * randomNumber * 31287121 + randomNumber * 11
													if (randomNumber shr 20 and 1 == 0) {
														multiset.add(dirtColor, 10)
													} else {
														multiset.add(stoneColor, 100)
													}
												}
											}
										}
										topFluidCount /= scaleSquare
										@Suppress("UnstableApiUsage") val color =
											Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR)
										multiset.clear()
										currentPixelHeight /= (scaleSquare)
										val brightness = if (color === MapColor.WATER_BLUE) {
											val y = topFluidCount * 0.1 + (mapPixelX + mapPixelY and 1) * 0.2
											when {
												y < 0.5 -> MapColor.Brightness.HIGH
												y > 0.9 -> MapColor.Brightness.LOW
												else    -> MapColor.Brightness.NORMAL
											}
										} else {
											if (d == (NaN as Number)) {
												d = currentPixelHeight
												continue
											}
											val y = (currentPixelHeight - d) * 4.0 / (scale + 4) + ((mapPixelX + mapPixelY and 1) - 0.5) * 0.4
											when {
												y > 0.6  -> MapColor.Brightness.HIGH
												y < -0.6 -> MapColor.Brightness.LOW
												else     -> MapColor.Brightness.NORMAL
											}
										}
										d = currentPixelHeight
										if (/*mapPixelY >= 0 &&*/ pixelDX * pixelDX + pixelDY * pixelDY < renderDistance * renderDistance && (!insideBorder || (mapPixelX + mapPixelY) and 1 == 0)) {
											val map = getMap(Vec3d(chunkPos.startX + .0, .0, chunkPos.startZ + .0), z)
											val x = mapPixelX - map.pos.x * 128 + 64 / scale
											val y = mapPixelY - map.pos.y * 128 + 64 / scale
											updatedLastPixel = updatedLastPixel or map.putColor(x, y, color.getRenderColorByte(brightness))
											map.hasUpdated = map.hasUpdated or updatedLastPixel
										}

									}
								}
							}
						}

					}

					ChiliMapClientConfig.MappingMode.All     -> {
						val center = player.chunkPos
						val chunkDistance = MinecraftClient.getInstance().options.viewDistance.get()

						exit@ for (chunkX in center.x - chunkDistance..center.x + chunkDistance) {
							if ((updateTracker xor chunkX) and 0xf == 0) {
								val d = Array(strips) { NaN }
								for (chunkZ in center.z - chunkDistance..center.z + chunkDistance) {
									val multiset: Multiset<MapColor> = LinkedHashMultiset.create()
									if (stop) break@exit
									(world.chunkManager.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false) as Chunk?)?.let { chunk ->
										val chunkPos = chunk.pos
										val map = getMap(Vec3d(chunkPos.startX + .0, .0, chunkPos.startZ + .0), z)

										for (cX in 0 until strips) {
											for (cZ in 0 until strips) {
												var topFluidCount = 0
												var currentPixelHeight = 0.0
												val highestPos = BlockPos.Mutable()
												val fluidWalker = BlockPos.Mutable()
												val mapPixelX = ((chunkX * strips % 128 + cX + 4 * strips) + 128 * scale) % 128
												val mapPixelY = ((chunkZ * strips % 128 + cZ + 4 * strips) + 128 * scale) % 128

												for (oX in cX * scale until (cX + 1) * scale) {
													for (oZ in cZ * scale until (cZ + 1) * scale) {
														if (stop) return@let
														var highest = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, oX, oZ) + 1
														var blockState: BlockState
														if (highest <= world.bottomY + 1) {
															blockState = Blocks.BEDROCK.defaultState
														} else {
															do {
																if (stop) return@let
																--highest
																highestPos.set(chunkPos.startX + oX, highest, chunkPos.startZ + oZ)
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
																	if (stop) return@let
																	fluidWalker.y = lowest--
																	blockState2 = chunk.getBlockState(fluidWalker)
																	++topFluidCount
																} while (topFluidCount < topFluidLimit && lowest > world.bottomY && !blockState2.fluidState.isEmpty)
																blockState = AreaMap.getFluidStateIfVisible(world, blockState, highestPos)!!
															}
														}
														currentPixelHeight += highest
														if (!world.dimension.hasCeiling() || world.registryKey === World.NETHER && blockState.block != Blocks.BEDROCK) {
															multiset.add(blockState.getMapColor(world, highestPos))
														} else {
															var randomNumber: Int = chunkPos.startX + oX + (chunkPos.startZ + oZ) * 231871
															randomNumber = randomNumber * randomNumber * 31287121 + randomNumber * 11
															if (randomNumber shr 20 and 1 == 0) {
																multiset.add(dirtColor, 10)
															} else {
																multiset.add(stoneColor, 100)
															}
														}
													}
												}
												topFluidCount /= scaleSquare
												@Suppress("UnstableApiUsage") val color =
													Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR)
												multiset.clear()
												currentPixelHeight /= (scaleSquare)
												val brightness = if (color === MapColor.WATER_BLUE) {
													val y = topFluidCount * 0.1 + (mapPixelX + mapPixelY and 1) * 0.2
													when {
														y < 0.5 -> MapColor.Brightness.HIGH
														y > 0.9 -> MapColor.Brightness.LOW
														else    -> MapColor.Brightness.NORMAL
													}
												} else {
													if (d[cX] == (NaN as Number)) {
														d[cX] = currentPixelHeight
														continue
													}
													val y =
														(currentPixelHeight - d[cX]) * 4.0 / (scale + 4) + ((mapPixelX + mapPixelY and 1) - 0.5) * 0.4
													when {
														y > 0.6  -> MapColor.Brightness.HIGH
														y < -0.6 -> MapColor.Brightness.LOW
														else     -> MapColor.Brightness.NORMAL
													}
												}
												d[cX] = currentPixelHeight
												map.hasUpdated =
													map.hasUpdated or map.putColor(mapPixelX, mapPixelY, color.getRenderColorByte(brightness))
											}
										}

									} ?: d.fill(NaN, 0, strips)
								}
							}
						}
					}
				}
			}
		}
	}

	fun getMarkers(from: Vec2i, to: Vec2i, entities: Iterable<Entity> = listOf(), remapPos: Boolean = true): MutableList<MapMarker> {
		return (entities.filter {
			it is LivingEntity && it.pos in (from to to)
		}.sortedWith { e1, e2 -> (if(e2 is PlayerEntity) 1 else 0) - if(e1 is PlayerEntity) 1 else 0 }.map {
			val rotation = ((it.yaw + (if(it.yaw < 0) -8 else +8)) / 22.5).toInt()
			MapMarker(
				when (it) {
					MinecraftClient.getInstance().player!! -> MapMarker.Type.SELF
					is PlayerEntity                        -> MapMarker.Type.OTHER
					is HostileEntity                       -> MapMarker.Type.RED_MARKER
					else                                   -> MapMarker.Type.BLUE_MARKER
				},
				if(remapPos) Vec3d((it.x - from.x) * 128 / (to.x - from.x) + from.x, it.y, (it.z - from.y) * 128 / (to.y - from.y) + from.y + 1) else it.pos,
				rotation.toFloat(),
				(if(it.hasCustomName()) it.customName else it.name)?.copy()
			)
		} + markers.filter {
			it.pos in (from to to)
		}).toMutableList()
	}

	inner class MapRegion(val pos: Vec2i) {
		val areas = HashMap<Vec3i, AreaMap>()

		fun getMap(mapPos: Vec3i): AreaMap {
			return synchronized(areas) {
				areas.compute(
					mapPos
				) { vec3i, map -> map ?: AreaMap(vec3i, ByteArray(AreaMap.pixelCount)) }!!
			}
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

		fun isEmpty() = synchronized(areas) {
			when (areas.values.filter { !it.isEmpty() }) {
				emptyList<AreaMap>() -> true; else -> false
			}
		}

	}
}

operator fun Pair<Vec2i, Vec2i>.contains(pos: Vec3d): Boolean = first.x <= pos.x && pos.x < second.x && first.y <= pos.z && pos.z < second.y
