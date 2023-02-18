package here.lenrik.chili_map.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import here.lenrik.chili_map.Vec2i
import here.lenrik.chili_map.client.ChiliMapClient
import here.lenrik.chili_map.client.config.ChiliMapClientConfig
import here.lenrik.chili_map.client.gui.screens.WorldMapScreen
import here.lenrik.chili_map.map.AreaMap
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.MapColor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.render.*
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.texture.TextureManager
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3f
import net.minecraft.util.math.Vec3i
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@Environment(EnvType.CLIENT)
class MinimapRenderer(val textureManager: TextureManager) : DrawableHelper() {
	private val MAP_BACKGROUND = RenderLayer.getText(Identifier("textures/map/map_background.png"))
	private val MAP_BACKGROUND_CHECKERBOARD =
		RenderLayer.getText(Identifier("textures/map/map_background_checkerboard.png"))
	private val DEFAULT_IMAGE_WIDTH = 128
	private val DEFAULT_IMAGE_HEIGHT = 128
	private val mapTextures = HashMap<AreaMap, MapTexture>()
	var image = BufferedImage(128, 128, BufferedImage.TYPE_4BYTE_ABGR)

	fun getMapTexture(areaMap: AreaMap): MapTexture {
		return mapTextures.compute(
			areaMap
		) { map, texture -> (texture ?: MapTexture(map)) }!!
	}

	fun clear() {
		val manager = MinecraftClient.getInstance().textureManager
		for((_, texture) in mapTextures) {
			texture.map.hasUpdated = true
			manager.destroyTexture(texture.id)
		}
		mapTextures.clear()
	}

	fun drawMinimap(matrices: MatrixStack, tickDelta: Float) {
		val client = MinecraftClient.getInstance()
		val world = MinecraftClient.getInstance().world!!
		val level = ChiliMapClient.container!!.getLevel(world.registryKey.value)

		if(!client.options.debugEnabled) {
			client.gameRenderer.lightmapTextureManager.disable()
			matrices.push()
			matrices.translate(10.0, 10.0, .0)
			matrices.scale((1f / client.window.scaleFactor).toFloat(), (1f / client.window.scaleFactor).toFloat(), 1F)
			matrices.scale(ChiliMapClient.config.minimapScale.toFloat(), ChiliMapClient.config.minimapScale.toFloat(), 0F)

			val consumerProviders = client.bufferBuilders.entityVertexConsumers as VertexConsumerProvider.Immediate
			val backgroundConsumer: VertexConsumer = consumerProviders.getBuffer(MAP_BACKGROUND_CHECKERBOARD)
			val matrix4f = matrices.peek().model
			val tl = -6.0f
			val br = 134.0f
			backgroundConsumer.vertex(matrix4f, tl, br, -1.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f).light(255).next()
			backgroundConsumer.vertex(matrix4f, br, br, -1.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f).light(255).next()
			backgroundConsumer.vertex(matrix4f, br, tl, -1.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f).light(255).next()
			backgroundConsumer.vertex(matrix4f, tl, tl, -1.0f).color(255, 255, 255, 255).texture(0.0f, 0.0f).light(255).next()
			val minimapZoom = 1 shl ChiliMapClient.config.minimapZoom
			val side = 128 * minimapZoom
			val mapFocusPos = when(ChiliMapClient.config.minimapMode) {
				ChiliMapClientConfig.MinimapMode.SingleMap, ChiliMapClientConfig.MinimapMode.Centered -> {
					client.player?.pos?.add(client.player?.velocity?.multiply(tickDelta + .0))!!
				}
				ChiliMapClientConfig.MinimapMode.Static                                               -> {
					if(WorldMapScreen.hasPos) WorldMapScreen.lastPos else client.player!!.pos.also { WorldMapScreen.lastPos = client.player!!.pos }
				}
			}
			when (ChiliMapClient.config.minimapMode) {
				ChiliMapClientConfig.MinimapMode.SingleMap -> {
					val map = level.getMap(mapFocusPos, ChiliMapClient.config.minimapZoom)
					val texture = ChiliMapClient.renderer.getMapTexture(map)
					texture.updateIfNeeded()
					texture.draw(matrices, consumerProviders)
					consumerProviders.drawCurrentLayer()
					with(AreaMap.toTopLeftCorner(map.pos)) {
						with(Vec2i(this.x.toInt(), this.z.toInt())) {
							val to = this + Vec2i(side, side)
							matrices.push()
							matrices.translate(-this.x.toDouble(), -this.y.toDouble(), .0)
							val vertexConsumer: VertexConsumer = consumerProviders.getBuffer(MAP_ICONS_RENDER_LAYER)
							level.getMarkers(this, to, world.entities).forEach {
								val b: Int = it.type.id
								val g = (b % 16 + 0).toFloat() / 16.0f
								val h = (b / 16 + 0).toFloat() / 16.0f
								val l = (b % 16 + 1).toFloat() / 16.0f
								val m = (b / 16 + 1).toFloat() / 16.0f
								matrices.translate(.0, .0, -0.001)
								matrices.push()
								matrices.translate(it.pos.x, it.pos.z, .0)
								matrices.scale(4.0f, 4.0f, 3.0f)
								matrices.translate(0.125, 0.125, 0.0)
								matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(it.rotation * 22.5f))
								matrices.translate(-0.125, 0.125, 0.0)
								val model = matrices.peek().model
								vertexConsumer.vertex(model, -1f, +1f, 0f).fullbrightTexture(g, h).next()
								vertexConsumer.vertex(model, +1f, +1f, 0f).fullbrightTexture(l, h).next()
								vertexConsumer.vertex(model, +1f, -1f, 0f).fullbrightTexture(l, m).next()
								vertexConsumer.vertex(model, -1f, -1f, 0f).fullbrightTexture(g, m).next()
								matrices.pop()
							}
							consumerProviders.drawCurrentLayer()
							matrices.pop()
						}
					}

				}

				ChiliMapClientConfig.MinimapMode.Centered, ChiliMapClientConfig.MinimapMode.Static -> {
					val maps = mutableSetOf<AreaMap>()
					for(i in 0..8) {
						maps.add(level.getMap(mapFocusPos.add(
							Vec3d(
								(i % 3 - 1) * (side / 2 + .0001),
								.0,
								(i / 3 - 1) * (side / 2 + .0001)
							)
						), ChiliMapClient.config.minimapZoom))
					}
					consumerProviders.drawCurrentLayer()
					RenderSystem.enableScissor(
						(10 * client.window.scaleFactor).toInt(),
						(client.window.height - 10 * client.window.scaleFactor - 128 * ChiliMapClient.config.minimapScale).toInt(),
						(128 * ChiliMapClient.config.minimapScale).toInt(),
						(128 * ChiliMapClient.config.minimapScale).toInt()
					)
					for(map in maps) {
						val texture = ChiliMapClient.renderer.getMapTexture(map)
						texture.updateIfNeeded()
						val x = 64 + map.pos.x * 128 - (mapFocusPos.x + 64).div(minimapZoom).toFloat()
						val y = 64 + map.pos.y * 128 - (mapFocusPos.z + 64).div(minimapZoom).toFloat()
						texture.draw(matrices, consumerProviders, x, y, 128f + x, 128f + y)
						consumerProviders.drawCurrentLayer()
//						break
					}
					val from = mapFocusPos.add((-side / 2).toDouble(), .0, (-side / 2).toDouble())
					with(Vec2i(from.x.toInt(), from.z.toInt())) {
						val to = this + Vec2i(side, side)
						matrices.push()
						matrices.translate(-from.x, -from.z, .0)
						val vertexConsumer: VertexConsumer = consumerProviders.getBuffer(MAP_ICONS_RENDER_LAYER)
						level.getMarkers(this, to, world.entities).forEach {
							val b: Int = it.type.id
							val g = (b % 16 + 0).toFloat() / 16.0f
							val h = (b / 16 + 0).toFloat() / 16.0f
							val l = (b % 16 + 1).toFloat() / 16.0f
							val m = (b / 16 + 1).toFloat() / 16.0f
							matrices.translate(.0, .0, -0.001)
							matrices.push()
							matrices.translate(it.pos.x, it.pos.z, .0)
							matrices.push()
							matrices.scale(4.0f, 4.0f, 3.0f)
							matrices.translate(0.125, 0.125, 0.0)
							matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(it.rotation * 22.5f))
							matrices.translate(-0.125, 0.125, 0.0)
							val model = matrices.peek().model
							vertexConsumer.vertex(model, -1f, +1f, 0f).fullbrightTexture(g, h).next()
							vertexConsumer.vertex(model, +1f, +1f, 0f).fullbrightTexture(l, h).next()
							vertexConsumer.vertex(model, +1f, -1f, 0f).fullbrightTexture(l, m).next()
							vertexConsumer.vertex(model, -1f, -1f, 0f).fullbrightTexture(g, m).next()
							matrices.pop()
							if(ChiliMapClient.renderedPlayerList) {
								matrices.translate(.0, 4.0, .0)
								drawCenteredText(matrices, client.textRenderer, it.name, 0, 0, 0xffffff)
							}
							matrices.pop()
						}
						consumerProviders.drawCurrentLayer()
						matrices.pop()
					}
					RenderSystem.disableScissor()
				}
			}
			matrices.pop()
			client.gameRenderer.lightmapTextureManager.enable()
		}
		if(ChiliMapClient.autoSaveCounter >= (2 * 60 + 30) * 20) {
			ChiliMapClient.autoSaveCounter -= 50 * 20
			ChiliMapClient.container!!.save(autoSave = true)
		}
		ChiliMapClient.renderedPlayerList = false
	}

	//	@Environment(EnvType.CLIENT)
	inner class MapTexture(val map: AreaMap) : AutoCloseable {
		private var texture: NativeImageBackedTexture =
			NativeImageBackedTexture(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, true)
		val id: Identifier = MinecraftClient.getInstance().textureManager.registerDynamicTexture(
			"chili_map/minimap_${map.pos.toShortString().replace(" ", "_").replace(",", "")}",
			texture
		)

		private var renderLayer: RenderLayer = RenderLayer.getText(id)

		private fun updateTexture() {
			for(y in 0..127) {
				for(x in 0..127) {
					val index = x + y * 128
					val color = MapColor.getRenderColor(this.map.colors[index].toInt())
					texture.image!!.setPixelColor(x, y, color)
					image.setRGB(x, y, color and 0xff00ff00.toInt() or (color and 0xff shl 16) or (color and 0xff0000 shr 16))
				}
			}
			texture.upload()
			if(map.pos == Vec3i(24, -11, 0)) ImageIO.write(image, "PNG", File("file.png"))
			map.hasUpdated = false
		}

		fun draw(
			matrices: MatrixStack,
			vertexConsumers: VertexConsumerProvider,
			fromXVec: Float = 0f,
			fromYVec: Float = 0f,
			toXVec: Float = 128f,
			toYVec: Float = 128f,
			fromXTex: Float = 0.000390625f,
			fromYTex: Float = 0.000390625f,
			toXTex: Float = 0.99960935f,
			toYTex: Float = 0.99960935f,
		) {
			val matrix4f = matrices.peek().model

			val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
			val z = -0.01f
			vertexConsumer.vertex(matrix4f, fromXVec, toYVec, z).color(255, 255, 255, 255).texture(fromXTex, toYTex).light(255).next()
			vertexConsumer.vertex(matrix4f, toXVec, toYVec, z).color(255, 255, 255, 255).texture(toXTex, toYTex).light(255).next()
			vertexConsumer.vertex(matrix4f, toXVec, fromYVec, z).color(255, 255, 255, 255).texture(toXTex, fromYTex).light(255).next()
			vertexConsumer.vertex(matrix4f, fromXVec, fromYVec, z).color(255, 255, 255, 255).texture(fromXTex, fromYTex).light(255).next()
			matrices.translate(0.0, 0.0, (-z).toDouble())
		}

		fun updateIfNeeded() {
			if(map.hasUpdated && !map.isEmpty()) {
				updateTexture()
			}
		}

		override fun close() {
			texture.close()
		}
	}

}

fun VertexConsumer.fullbrightTexture(u: Float, v: Float): VertexConsumer = this.color(255, 255, 255, 255).texture(u, v).light(255)

