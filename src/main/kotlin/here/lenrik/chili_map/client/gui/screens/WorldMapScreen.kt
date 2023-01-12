package here.lenrik.chili_map.client.gui.screens

import here.lenrik.chili_map.Vec2i
import here.lenrik.chili_map.client.ChiliMapClient
import here.lenrik.chili_map.client.gui.MAP_ICONS_RENDER_LAYER
import here.lenrik.chili_map.map.AreaMap
import here.lenrik.chili_map.map.LevelMap
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3f
import net.minecraft.util.math.Vec3i
import org.lwjgl.glfw.GLFW
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import here.lenrik.chili_map.client.ChiliMapClient.Companion.client as client_

class WorldMapScreen(text: Text?) : Screen(text) {
	private var xOffset: Float = if (hasPos) lastPos.x.toFloat() else client_.player!!.blockX.toFloat()
		set(value) {
			field = value
			lastPos = Vec3d(value.toDouble(), lastPos.y, lastPos.z)
		}
	private var yOffset: Float = if (hasPos) lastPos.z.toFloat() else client_.player!!.blockZ.toFloat()
		set(value) {
			field = value
			lastPos = Vec3d(lastPos.x, lastPos.y, value.toDouble())
		}

	private var currentMouseX = 0.0
		set(value) {
			lastPos = Vec3d(lastPos.x - (field - value) / scale * client_.window.scaleFactor, lastPos.y, lastPos.z)
			field = value
		}

	private var currentMouseY = 0.0
		set(value) {
			lastPos = Vec3d(lastPos.x, lastPos.y, lastPos.z - (field - value) / scale * client_.window.scaleFactor)
			field = value
		}
	private var initialScale = 2.0
	private var scale = initialScale

	init {
		assert(client_.player != null) { "Player must be present" }
	}

	override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
		val client = client_
		client.profiler.push("chili_map_world_map_render")
		renderBackground(matrices)
		assert(client.player != null && client.world != null) { "call this method in the actual world with a player" }
		val guiScale = client.window.scaleFactor
		super.render(matrices, mouseX, mouseY, delta)
		val vertexConsumers: VertexConsumerProvider.Immediate = client.bufferBuilders.entityVertexConsumers
		val zoom = min(max(log2(2 / scale).toInt(), 0), 4)
		val scaleZoom = 1 shl zoom
		val iconScale = (min(max(guiScale / scale, 1.0 / 4), 8.0)).toFloat()
		client.debugRenderer.toggleShowChunkBorder()
		val showChunkBorders = client.debugRenderer.toggleShowChunkBorder()
		if (client.options.debugEnabled) {
			client.textRenderer.draw(
				matrices,
				Text.of("" + (xOffset + currentMouseX / scale * guiScale).toInt() + ":" + (yOffset + currentMouseY / scale * guiScale).toInt() + "/" + scale.toFloat()),
				0f,
				0f,
				-1
			)
		}
		matrices.push()
		matrices.translate(width / 2 - currentMouseX, height / 2 - currentMouseY, 0.0)
		matrices.scale((scale / guiScale).toFloat(), (scale / guiScale).toFloat(), 1f)
		matrices.translate(-xOffset.toDouble(), -yOffset.toDouble(), 0.0)
		matrices.push()
		var shownMaps = 0
		var shownRegions = 0
		val topLeftScreenCorner = Vec2i(
			((currentMouseX - width / 2) / scale * guiScale + xOffset).toInt(),
			((currentMouseY - height / 2) / scale * guiScale + yOffset).toInt()
		)
		val bottomRightScreenCorner = Vec2i(
			((currentMouseX + width / 2) / scale * guiScale + xOffset).toInt(),
			((currentMouseY + height / 2) / scale * guiScale + yOffset).toInt()
		)
		val lines = mutableListOf<Text>()
		val world = client.world!!
		val level = ChiliMapClient.container!!.getLevel(world.registryKey.value)
		for ((regionPos, region) in level.regions) {
			val topLeftRegionCorner = regionPos * LevelMap.boundary - Vec2i(64, 64)
			val bottomRightRegionCorner = (regionPos + Vec2i(1, 1)) * LevelMap.boundary - Vec2i(64, 64)
			lines += Text.of(buildString {
				append("${region.pos.x pad ("᭼᭼᭼" to 10)}, ${region.pos.y pad ("᭼᭼᭼" to 10)}")
				append(": ")
				append("${topLeftRegionCorner.x pad ("᭼᭼᭼" to 10)}, ${topLeftRegionCorner.y pad ("᭼᭼᭼" to 10)}")
				append(" ◰ ")
				append("${bottomRightRegionCorner.x pad ("᭼᭼᭼" to 10)}, ${bottomRightRegionCorner.y pad ("᭼᭼᭼" to 10)}")
			})
			if (
				topLeftRegionCorner.x > bottomRightScreenCorner.x ||
				topLeftRegionCorner.y > bottomRightScreenCorner.y ||
				topLeftScreenCorner.x > bottomRightRegionCorner.x ||
				topLeftScreenCorner.y > bottomRightRegionCorner.y ||
				region.isEmpty()
			) continue
			++shownRegions
			synchronized(region.areas) {
				for (mapZoomPos in region.areas.keys.stream().sorted(
					Comparator.comparingInt { vec: Vec3i -> (vec.x + vec.y) }
				)) {
					val topLeftAreaCorner = Vec2i(
						(mapZoomPos.x * 128) * scaleZoom - 64,
						(mapZoomPos.y * 128) * scaleZoom - 64
					)
					val bottomRightAreaCorner = Vec2i(
						(mapZoomPos.x + 1) * 128 * scaleZoom - 64,
						(mapZoomPos.y + 1) * 128 * scaleZoom - 64
					)
					val map = region.getMap(mapZoomPos)
					if (
						topLeftAreaCorner.x > bottomRightScreenCorner.x ||
						topLeftAreaCorner.y > bottomRightScreenCorner.y ||
						bottomRightAreaCorner.x < topLeftScreenCorner.x ||
						bottomRightAreaCorner.y < topLeftScreenCorner.y ||
						mapZoomPos.z != zoom || map.isEmpty()
					) continue
					++shownMaps
					matrices.push()
					val texture = ChiliMapClient.renderer.getMapTexture(map)
					matrices.translate(
						(mapZoomPos.x * 128) * scaleZoom - 64.0,
						(mapZoomPos.y * 128) * scaleZoom - 64.0,
						.0
					)
					matrices.scale(scaleZoom.toFloat(), scaleZoom.toFloat(), 1f)
					texture.updateIfNeeded()
					texture.draw(matrices, vertexConsumers)

					if (showChunkBorders && scale >= 0.03125) {
						vertexConsumers.drawCurrentLayer()
						matrices.push()
						matrices.scale(
							(guiScale / scaleZoom / scale).toFloat(),
							(guiScale / scaleZoom / scale).toFloat(),
							1f
						)
						matrices.translate(.0, .0, 600.0)
						with((128 * scale * scaleZoom / guiScale + .1).toInt()) {
							val i = this
//						DrawableHelper.fillGradient(matrices, 0, 0, 1, i, )
							fill(matrices, 0 + 0, 1 + 0, 1 + 0, i + 0, 0x7fFF7f7F)
							fill(matrices, i - 1, 0 + 0, i + 0, i - 1, 0x7fFF7f7F)
							matrices.push()
							matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90f))
							matrices.translate(.0, .0 - i, .0)
							fill(matrices, 0 + 0, 1 + 0, 1 + 0, i + 0, 0x7fFF7f7F)
							fill(matrices, i - 1, 0 + 0, i + 0, i - 1, 0x7fFF7f7F)
							matrices.pop()
							if (scale * scaleZoom > 3.6) {
								for (d in .0..i.toDouble() step 16 * scale / guiScale) { // 16 * scale / guiScale) {
									val j = d.toInt()
									fill(matrices, j + 0x0, 0 + 0x0, j + 0x1, i + 0x0, 0x7f7f7fff)
									fill(matrices, 0 + 0x0, j + 0x0, i + 0x0, j + 0x1, 0x7f7f7fff)
								}
							}
						}
						if (scale * scaleZoom > .25) {
							matrices.scale(.5f, .5f, 1f)
							client.textRenderer.draw(
								matrices, Text.of(mapZoomPos.toShortString()).copy().formatted(
									if (mapZoomPos.equals(
											AreaMap.toMapPosAtZoomLevel(
												client.player!!.pos, zoom
											)
										)
									) Formatting.GREEN else Formatting.WHITE
								), 0f, 0f, -1
							)
						}
						matrices.pop()
					}
					matrices.pop()
				}
			}
			if (showChunkBorders) {
				matrices.push()
				matrices.translate(topLeftRegionCorner.x - .0, topLeftRegionCorner.y - .0, .0)
				matrices.scale((guiScale / scale).toFloat(), (guiScale / scale).toFloat(), (guiScale / scale).toFloat())
				with((8192 / guiScale * scale + .1).toInt()) {
					val i = this
					fillGradient(matrices, 0 + 0, 1 + 0, 1 + 0, i + 0, 0x7f00FF00, 0x7f7fFF7F)
					fillGradient(matrices, i - 1, 0 + 0, i + 0, i - 1, 0x7f7fFF7F, 0x7fFFffFF)
					matrices.push()
					matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90f))
					matrices.translate(.0, .0 - i, .0)
					fillGradient(matrices, 0 + 0, 1 + 0, 1 + 0, i + 0, 0x7f7fFF7F, 0x7f00FF00)
					fillGradient(matrices, i - 1, 0 + 0, i + 0, i - 1, 0x7fFFffFF, 0x7f7fFF7F)
					matrices.pop()
				}
				matrices.pop()
			}
		}
		matrices.pop()
		matrices.translate(topLeftScreenCorner.x.toDouble() * 2, topLeftScreenCorner.y.toDouble() * 2, .0)
		val vertexConsumer: VertexConsumer = vertexConsumers.getBuffer(MAP_ICONS_RENDER_LAYER)
		matrices.push()
//		println(matrices.peek().model)
		level.getMarkers(topLeftScreenCorner, bottomRightScreenCorner, if (scale > 0.09) world.entities else listOf(), false)/*.also { list -> kotlin.runCatching { println(list.first().pos) } }*/.forEach {
			val b: Int = it.type.id
			val g = (b % 16 + 0).toFloat() / 16.0f
			val h = (b / 16 + 0).toFloat() / 16.0f
			val l = (b % 16 + 1).toFloat() / 16.0f
			val m = (b / 16 + 1).toFloat() / 16.0f
			matrices.translate(.0, .0, -0.001)
			matrices.push()
			matrices.translate(
				it.pos.x - (topLeftScreenCorner.x * 1.5 - bottomRightScreenCorner.x / 2) - xOffset - currentMouseX / scale * guiScale,
				it.pos.z - (topLeftScreenCorner.y * 1.5 - bottomRightScreenCorner.y / 2) - yOffset - currentMouseY / scale * guiScale,
//				it.pos.x - xOffset.toDouble() + (topLeftScreenCorner.x - bottomRightScreenCorner.x) / 2,
//				it.pos.z - yOffset.toDouble() + (topLeftScreenCorner.y - bottomRightScreenCorner.y) / 2,
				.0)
			matrices.scale(4.0f, 4.0f, 3.0f)
			matrices.translate(0.125, 0.125, 0.0)
			matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(it.rotation * 22.5f))
			matrices.translate(-0.125, 0.125, 0.0)
			matrices.scale(iconScale, iconScale, iconScale)
			val model = matrices.peek().model
			vertexConsumer.vertex(model, -1f, +1f, 0f).color(255, 255, 255, 255).texture(g, h).light(255).next()
			vertexConsumer.vertex(model, +1f, +1f, 0f).color(255, 255, 255, 255).texture(l, h).light(255).next()
			vertexConsumer.vertex(model, +1f, -1f, 0f).color(255, 255, 255, 255).texture(l, m).light(255).next()
			vertexConsumer.vertex(model, -1f, -1f, 0f).color(255, 255, 255, 255).texture(g, m).light(255).next()
			matrices.pop()
		}
		matrices.pop()
		vertexConsumers.drawCurrentLayer()
		matrices.pop()
		vertexConsumers.drawCurrentLayer()
		if (client.options.debugEnabled) {
			matrices.push()
			matrices.translate(.0, textRenderer.fontHeight.toDouble(), .0)
			client.textRenderer.draw(matrices, Text.of("shown: ${shownMaps}maps ${shownRegions}regions"), 0f, 0f, 0xffffff)
			matrices.translate(.0, textRenderer.fontHeight.toDouble(), .0)
			client.textRenderer.draw(matrices, Text.of(buildString {
				append("${topLeftScreenCorner.x pad ("᭼᭼᭼" to 10)}, ${topLeftScreenCorner.y pad ("᭼᭼᭼" to 10)}")
				append(" ◻ ")
				append("${bottomRightScreenCorner.x pad ("᭼᭼᭼" to 10)}, ${bottomRightScreenCorner.y pad ("᭼᭼᭼" to 10)}")
			}), textRenderer.getWidth("${"" pad ("‥‥" to 20)},:  ") + 0f, 0f, 0xffffff)
			for (line in lines) {
				matrices.translate(.0, textRenderer.fontHeight.toDouble(), .0)
				client.textRenderer.draw(matrices, line, 0f, 0f, -1)
			}
			matrices.pop()
		}
		client.profiler.pop()
	}

	override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
		when (keyCode) {
			GLFW.GLFW_KEY_RIGHT -> xOffset++
			GLFW.GLFW_KEY_LEFT  -> xOffset--
			GLFW.GLFW_KEY_UP    -> yOffset++
			GLFW.GLFW_KEY_DOWN  -> yOffset--
		}
		return super.keyReleased(keyCode, scanCode, modifiers)
	}

	override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
		currentMouseX -= deltaX
		currentMouseY -= deltaY
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
	}

	override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
		val guiScale = client!!.window.scaleFactor
		xOffset += (currentMouseX / scale * guiScale).toFloat()
		yOffset += (currentMouseY / scale * guiScale).toFloat()
		currentMouseX = 0.0
		currentMouseY = 0.0
		xOffset = xOffset
		yOffset = yOffset
		return super.mouseReleased(mouseX, mouseY, button)
	}

	override fun resize(client: MinecraftClient, width: Int, height: Int) {
		super.resize(client, width, height)
	}

	override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
//		ChiliMap.LOGGER.info("scrolled by {}", amount);
		val sqr2 = 1.4142135623730951
		var scalingFactor = when (amount) {
			-1.0 -> 1 / sqr2
			else -> sqr2
		}
		val pScale = scale
		scale *= scalingFactor
		scale = if (scale > 128.0001) scale / sqr2 else if (scale <= 1.0 / (1 shl 11) * sqr2) scale * sqr2 else scale
		scalingFactor = if (pScale == scale) 1.0 else scalingFactor
		val guiScale = client!!.window.scaleFactor

		xOffset -= ((width / 2 - mouseX) * guiScale * (scalingFactor - 1) / scale).toFloat()
		yOffset -= ((height / 2 - mouseY) * guiScale * (scalingFactor - 1) / scale).toFloat()
		currentMouseX *= scalingFactor
		currentMouseY *= scalingFactor
		return super.mouseScrolled(mouseX, mouseY, amount)
	}

	override fun isPauseScreen() = false

	companion object {
		private var _lastPos: Vec3d? = null
		var lastPos: Vec3d
			get() : Vec3d {
				return _lastPos
					?: client_.player!!.pos.also {
						lastPos = it
					}// if(_lastPos == null) client_.player!!.pos.also { _lastPos = it } else _lastPos!!
			}
			set(value) {
				_lastPos = value
			}
		val hasPos get() = _lastPos != null
	}
}

private infix fun Double.f(d: Double): String = String.format("%${d + if (d == .0) .1 else sign(d)}f", this)

//fun Boolean.toInt() = compareTo(false)

infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
	require(start.isFinite())
	require(endInclusive.isFinite())
	require(step > 0.0) { "Step must be positive, was: $step." }
	require(start + step != start) { "Step must be over Double precision for start value" }
	val sequence = generateSequence(start) { previous ->
		if (previous == Double.POSITIVE_INFINITY) null else {
			val next = previous + step
			if (next > endInclusive) null else next
		}
	}
	return sequence.asIterable()
}

infix fun <T: ClassLoader>T.nyaa(owo: T) = null

infix fun Any?.pad(count: Int) = this pad (" " to count)

infix fun Any?.pad(pair: Pair<String, Int>): String {
	val string = this.toString()
	return string + pair.first.repeat(max(0, pair.second - string.length))
}
