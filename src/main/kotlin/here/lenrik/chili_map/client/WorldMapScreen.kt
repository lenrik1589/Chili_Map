package here.lenrik.chili_map.client

import here.lenrik.chili_map.Vec2i
import here.lenrik.chili_map.client.RenderHelper.Companion.MAP_ICONS_RENDER_LAYER
import here.lenrik.chili_map.map.AreaMap
import here.lenrik.chili_map.map.LevelMap
import here.lenrik.chili_map.map.MapMarker
import here.lenrik.chili_map.map.MapMarker.Type.FRAME
import here.lenrik.chili_map.map.MapMarker.Type.PLAYER
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3f
import net.minecraft.util.math.Vec3i
import org.lwjgl.glfw.GLFW
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.floor
import kotlin.math.log2

class WorldMapScreen(text: Text?) : Screen(text) {
	private var xOffset: Float
	private var yOffset: Float
	private var initialScale = 2
	private var scale = initialScale + .0
	private var scaleWorkaround: Int

	init {
		assert(MinecraftClient.getInstance().player != null) { "Player must be present" }
		scaleWorkaround = /*0.f + */
			MinecraftClient.getInstance().window.width / MinecraftClient.getInstance().window.scaledWidth
		xOffset = -MinecraftClient.getInstance().player!!.blockX.toFloat()
		yOffset = -MinecraftClient.getInstance().player!!.blockZ.toFloat()
	}

	override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
		val client = MinecraftClient.getInstance()
		client.profiler.push("chili_map_world_map_render")
		renderBackground(matrices)
		assert(client.player != null && client.world != null) { "call this method in the actual world with a player" }
		var guiScale = client.options.guiScale
		guiScale = if (guiScale == 0) scaleWorkaround else guiScale
		super.render(matrices, mouseX, mouseY, delta)
		val vertexConsumers: VertexConsumerProvider.Immediate = client.bufferBuilders.entityVertexConsumers
		val zoom = min(max(log2(2 / scale).toInt(), 0), 4)
		val scaleZoom = 1 shl zoom
		if (client.options.debugEnabled) {
			client.textRenderer.draw(
				matrices,
				Text.of("" + (xOffset + currentMouseX / scale * guiScale).toInt() + ":" + (yOffset + currentMouseY / scale * guiScale).toInt() + "/" + scale.toFloat()),
				0f,
				0f,
				-1
			)
			//			client.textRenderer.draw(matrices, Text.of("" + (int) ((width / 2 - mouseX) * guiScale / scale) + ":" + (int) ((height / 2 - mouseY) * guiScale / scale) + "/" + (float) scale + ":" + guiScale), 0, client.textRenderer.fontHeight, 0xffffff);
		}
		matrices.push()
		matrices.translate(currentMouseX + width / 2, currentMouseY + height / 2, 0.0)
		matrices.scale((scale / guiScale).toFloat(), (scale / guiScale).toFloat(), 1f)
		matrices.translate(xOffset.toDouble(), yOffset.toDouble(), 0.0)
		matrices.push()
		var shownMaps = 0
		var shownRegions = 0
		val topLeftScreenCorner = Vec2i(
			(-(width / 2 + currentMouseX) / scale * guiScale - xOffset).toInt(),
			(-(height / 2 + currentMouseY) / scale * guiScale - yOffset).toInt()
		)
		val bottomRightScreenCorner = Vec2i(
			((width / 2 - currentMouseX) / scale * guiScale - xOffset).toInt(),
			((height / 2 - currentMouseY) / scale * guiScale - yOffset).toInt()
		)
		val lines = mutableListOf<Text>()
		for (region in ChilliMapClient.container!!.getLevel(
			client.world!!.registryKey.value
		).regions) {
			val topLeftRegionCorner = region.key * LevelMap.boundary - Vec2i(64, 64)
			val bottomRightRegionCorner = (region.key + Vec2i(1, 1)) * LevelMap.boundary - Vec2i(64, 64)
//			lines += Text.of(
//				buildString { append(region.value.pos.toShortString()); append(": "); append((topLeftRegionCorner.x > bottomRightScreenCorner.x).toInt()); append(" "); append((topLeftRegionCorner.y > bottomRightScreenCorner.y).toInt()); append(" "); append((topLeftScreenCorner.x > bottomRightRegionCorner.x).toInt()); append(" "); append((topLeftScreenCorner.y > bottomRightRegionCorner.y).toInt()); append(" "); append(topLeftRegionCorner.toShortString()); append(" > "); append(bottomRightScreenCorner.toShortString()); append(" | "); append(topLeftScreenCorner.toShortString()); append(" > "); append(bottomRightRegionCorner.toShortString()) }
//			)
			if (
				topLeftRegionCorner.x > bottomRightScreenCorner.x ||
				topLeftRegionCorner.y > bottomRightScreenCorner.y ||
				topLeftScreenCorner.x > bottomRightRegionCorner.x ||
				topLeftScreenCorner.y > bottomRightRegionCorner.y
			) continue
			++shownRegions
			for (mapZoomPos in region.value.areas.keys.stream().sorted(
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
				if (
					topLeftAreaCorner.x > bottomRightScreenCorner.x ||
					topLeftAreaCorner.y > bottomRightScreenCorner.y ||
					bottomRightAreaCorner.x < topLeftScreenCorner.x ||
					bottomRightAreaCorner.y < topLeftScreenCorner.y ||
					mapZoomPos.z != zoom
				) continue
				++shownMaps
				matrices.push()
				val texture = ChilliMapClient.renderer.getMapTexture(region.value.getMap(mapZoomPos))
				matrices.translate(
					((mapZoomPos.x * 128) * scaleZoom).toDouble() - 64,
					((mapZoomPos.y * 128) * scaleZoom).toDouble() - 64,
					.0
				)
				matrices.scale(scaleZoom.toFloat(), scaleZoom.toFloat(), 1f)
				texture.updateIfNeeded()
				texture.draw(matrices, vertexConsumers)
				client.debugRenderer.toggleShowChunkBorder()
				if (client.debugRenderer.toggleShowChunkBorder()) {
					vertexConsumers.drawCurrentLayer()
					matrices.push()
					val i = (128 * scale * scaleZoom / guiScale + .1).toInt()
					matrices.scale(
						(guiScale.toDouble() / scaleZoom / scale).toFloat(),
						(guiScale.toDouble() / scaleZoom / scale).toFloat(),
						1f
					)
					matrices.translate(.0, .0, 600.0)
					for (j in 0 until 2 * i) {
						DrawableHelper.fill(
							matrices,
							if (j < i) j else i - 1,
							if (j < i) 0 else j - i,
							if (j < i) j + 1 else i,
							if (j < i) 1 else (j - i) + 1,
							-0x2fffffff or (j * 256 / (i * 2)) or ((256 - j * 256 / (i * 2)) shl 16)
						)
						fill(
							matrices,
							if (j < i) 0 else j - i,
							if (j < i) j else i - 1,
							if (j < i) 1 else (j - i) + 1,
							if (j < i) j + 1 else i,
							-0x2fffffff or (j * 256 / (i * 2)) or ((256 - j * 256 / (i * 2)) shl 16)
						)
					}
//					DrawableHelper.fill(matrices, 0 + 0, 0 + 0, i - 1, 1 + 0, 0x7fFF0000)
//					DrawableHelper.fill(matrices, 0 + 0, 1 + 0, 1 + 0, i + 0, 0x7fFF0000)
//					DrawableHelper.fill(matrices, i - 1, 0 + 0, i + 0, i - 1, 0x7f0000FF)
//					DrawableHelper.fill(matrices, 1 + 0, i - 1, i + 0, i + 0, 0x7f0000FF)
					if (scale * scaleZoom > .25) {
						matrices.scale(.5f, .5f, 1f)
//						matrices.translate(0.0, 0.0, -1000.0)
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
		matrices.pop()
		matrices.translate(topLeftScreenCorner.x.toDouble(), topLeftScreenCorner.y.toDouble(), .0)
//		matrices.scale(-(scale / guiScale).toFloat(), -(scale / guiScale).toFloat(), 1f)
//		matrices.translate(-topLeftScreenCorner.x.toDouble(), -topLeftScreenCorner.y.toDouble(), .0)
		val vertexConsumer: VertexConsumer = vertexConsumers.getBuffer(MAP_ICONS_RENDER_LAYER)
		ChilliMapClient.container!!.getMarkers(topLeftScreenCorner, bottomRightScreenCorner).also {
			it.addAll(MinecraftClient.getInstance().world!!.players.filter { player ->
				player.x >= topLeftScreenCorner.x && player.x < bottomRightScreenCorner.x && player.z >= topLeftScreenCorner.y && player.z < bottomRightScreenCorner.y/* || true*/
			}.map<PlayerEntity, MapMarker> { player ->
				val rotation = ((player.yaw + (if (player.yaw < 0) -8 else +8)) / 22.5).toInt()
				MapMarker(
					if (player == MinecraftClient.getInstance().player!!) PLAYER else FRAME,
					with(
						(Vec2i(
							floor(player.x).toInt(),
							floor(player.z).toInt()
						) - topLeftScreenCorner) * (topLeftScreenCorner - bottomRightScreenCorner) / (bottomRightScreenCorner - topLeftScreenCorner) + topLeftScreenCorner
					) { Vec3i(this.x.toDouble(), player.y, this.y.toDouble()) },
					rotation.toFloat()
				)
			})
		}.forEach {
			val b: Int = it.type.id
			val g = (b % 16 + 0).toFloat() / 16.0f
			val h = (b / 16 + 0).toFloat() / 16.0f
			val l = (b % 16 + 1).toFloat() / 16.0f
			val m = (b / 16 + 1).toFloat() / 16.0f
			matrices.translate(.0, .0, -0.001)
			matrices.push()
			matrices.translate(-it.pos.x.toDouble() + topLeftScreenCorner.x, -it.pos.z.toDouble() + topLeftScreenCorner.y, .0)
			matrices.scale(4.0f, 4.0f, 3.0f)
			matrices.translate(0.125, 0.125, 0.0)
			matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(it.rotation * 22.5f))
			matrices.translate(-0.125, 0.125, 0.0)
//			matrices.translate(-0.125, 0.125, 0.0)
			val model = matrices.peek().model
			vertexConsumer.vertex(model, -1f, +1f, 0f).color(255, 255, 255, 255).texture(g, h).light(255).next()
			vertexConsumer.vertex(model, +1f, +1f, 0f).color(255, 255, 255, 255).texture(l, h).light(255).next()
			vertexConsumer.vertex(model, +1f, -1f, 0f).color(255, 255, 255, 255).texture(l, m).light(255).next()
			vertexConsumer.vertex(model, -1f, -1f, 0f).color(255, 255, 255, 255).texture(g, m).light(255).next()
			matrices.pop()
		}
		vertexConsumers.drawCurrentLayer()
		matrices.pop()
		vertexConsumers.drawCurrentLayer()
		if (client.options.debugEnabled) {
			matrices.push()
			matrices.translate(.0, textRenderer.fontHeight.toDouble(), .0)
			client.textRenderer.draw(matrices, Text.of(shownMaps.toString()), 0f, 0f, 0xffffff)
			matrices.translate(.0, textRenderer.fontHeight.toDouble(), .0)
			client.textRenderer.draw(matrices, Text.of(shownRegions.toString()), 0f, 0f, 0xffffff)
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
			GLFW.GLFW_KEY_LEFT -> xOffset--
			GLFW.GLFW_KEY_UP -> yOffset++
			GLFW.GLFW_KEY_DOWN -> yOffset--
		}
		return super.keyReleased(keyCode, scanCode, modifiers)
	}

	private var currentMouseX = 0.0

	private var currentMouseY = 0.0

	override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
		currentMouseX += deltaX
		currentMouseY += deltaY
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
	}

	override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
		var guiScale = client!!.options.guiScale
		guiScale = if (guiScale == 0) scaleWorkaround else guiScale
		xOffset += (currentMouseX / scale * guiScale).toFloat()
		yOffset += (currentMouseY / scale * guiScale).toFloat()
		currentMouseX = 0.0
		currentMouseY = 0.0
		return super.mouseReleased(mouseX, mouseY, button)
	}

	override fun resize(client: MinecraftClient, width: Int, height: Int) {
		scaleWorkaround = MinecraftClient.getInstance().window.width / MinecraftClient.getInstance().window.scaledWidth
		super.resize(client, width, height)
	}

	override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
//		ChilliMap.LOGGER.info("scrolled by {}", amount);
		val sqr2 = 1.4142135623730951
		var scalingFactor = ((1 + amount) * sqr2 + (1 - amount) / sqr2) / 2
		val pScale = scale
		scale *= scalingFactor
		scale = if (scale > 128.0001) scale / sqr2 else if (scale < 1.0 / (1 shl 8)) scale * sqr2 else scale
		scalingFactor = if (pScale == scale) 1.0 else scalingFactor
		var guiScale = client!!.options.guiScale
		guiScale = if (guiScale == 0) scaleWorkaround else guiScale
		xOffset += ((width / 2 - mouseX) * guiScale * (scalingFactor - 1) / scale).toFloat()
		yOffset += ((height / 2 - mouseY) * guiScale * (scalingFactor - 1) / scale).toFloat()
		return super.mouseScrolled(mouseX, mouseY, amount)
	}

	override fun isPauseScreen(): Boolean = false
}

fun Boolean.toInt() = compareTo(false)