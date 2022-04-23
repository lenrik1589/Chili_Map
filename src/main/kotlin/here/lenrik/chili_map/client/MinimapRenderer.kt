package here.lenrik.chili_map.client

import here.lenrik.chili_map.Vec2i
import here.lenrik.chili_map.client.ChilliMapClient.Companion.minimapMode
import here.lenrik.chili_map.client.ChilliMapClient.Companion.zoom
import here.lenrik.chili_map.client.RenderHelper.Companion.MAP_ICONS_RENDER_LAYER
import here.lenrik.chili_map.map.AreaMap
import here.lenrik.chili_map.map.MapMarker
import here.lenrik.chili_map.map.MapMarker.Type.OTHER
import here.lenrik.chili_map.map.MapMarker.Type.SELF
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.MapColor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.texture.TextureManager
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3f
import net.minecraft.util.math.Vec3i
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

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

	fun drawMinimap(matrices: MatrixStack) {
		val client = MinecraftClient.getInstance()
		val level = ChilliMapClient.container!!.getLevel(MinecraftClient.getInstance().world!!.registryKey.value)

		if(!client.options.debugEnabled) {
			client.gameRenderer.lightmapTextureManager.disable()
			matrices.push()
			matrices.translate(10.0, 10.0, .0)
			val consumerProviders = client.bufferBuilders.entityVertexConsumers as VertexConsumerProvider.Immediate
			val backgroundConsumer: VertexConsumer = consumerProviders.getBuffer(MAP_BACKGROUND_CHECKERBOARD)
			val matrix4f = matrices.peek().model
			val tl = -6.0f
			val br = 134.0f
			backgroundConsumer.vertex(matrix4f, tl, br, -1.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f).light(255).next()
			backgroundConsumer.vertex(matrix4f, br, br, -1.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f).light(255).next()
			backgroundConsumer.vertex(matrix4f, br, tl, -1.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f).light(255).next()
			backgroundConsumer.vertex(matrix4f, tl, tl, -1.0f).color(255, 255, 255, 255).texture(0.0f, 0.0f).light(255).next()
			when (minimapMode) {
				ChilliMapClient.MinimapMode.SingleMap -> {
					val map = level.getMap(client.player!!.pos, zoom)
					val texture = ChilliMapClient.renderer.getMapTexture(map)
					texture.updateIfNeeded()
					texture.draw(matrices, consumerProviders)
					consumerProviders.drawCurrentLayer()
					with(AreaMap.toTopLeftCorner(map.pos)) {
						with(Vec2i(this.x.toInt(), this.z.toInt())) {
							val to = this + Vec2i(128 shl zoom, 128 shl zoom)
							matrices.push()
							matrices.translate(-this.x.toDouble(), -this.y.toDouble(), .0)
							val vertexConsumer: VertexConsumer = consumerProviders.getBuffer(MAP_ICONS_RENDER_LAYER)
							ChilliMapClient.container!!.getMarkers(this, to).also {
								it.addAll(MinecraftClient.getInstance().world!!.players.filter { player ->
									player.x >= this.x && player.x < to.x && player.z >= this.y && player.z < to.y
								}.map<PlayerEntity, MapMarker> { player ->
									val rotation = ((player.yaw + (if(player.yaw < 0) -8 else +8)) / 22.5).toInt()
									MapMarker(
										if(player == MinecraftClient.getInstance().player!!) SELF else OTHER,
										with(
											(Vec2i(floor(player.x).toInt(), floor(player.z).toInt()) - this) * Vec2i(128, 128) / (to - this) + this
										) { Vec3i(this.x.toDouble(), player.y, this.y.toDouble()) },
//								Vec3i(player.x, player.y, player.z),
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
								matrices.translate(it.pos.x.toDouble(), it.pos.z.toDouble(), .0)
								matrices.scale(4.0f, 4.0f, 3.0f)
								matrices.translate(0.125, 0.125, 0.0)
								matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(it.rotation * 22.5f))
								matrices.translate(-0.125, 0.125, 0.0)
								val model = matrices.peek().model
								vertexConsumer.vertex(model, -1f, +1f, 0f).color(255, 255, 255, 255).texture(g, h).light(255).next()
								vertexConsumer.vertex(model, +1f, +1f, 0f).color(255, 255, 255, 255).texture(l, h).light(255).next()
								vertexConsumer.vertex(model, +1f, -1f, 0f).color(255, 255, 255, 255).texture(l, m).light(255).next()
								vertexConsumer.vertex(model, -1f, -1f, 0f).color(255, 255, 255, 255).texture(g, m).light(255).next()
								matrices.pop()
							}
							consumerProviders.drawCurrentLayer()
							matrices.pop()
						}
					}
				}
				ChilliMapClient.MinimapMode.Centered -> {
					val maps = mutableListOf<AreaMap>()
					for(i in 0..8) {
						val map = level.getMap(client.player!!.pos.add(Vec3d((i % 3 - 1) * 64.0001/* - 64*/, .0, (i / 3 - 1) * 64.0001/* - 64*/)), zoom)
						if(map !in maps) maps.add(map)
					}
					for(map in maps) {
						val topLeftAreaCorner = Vec2i(
							map.pos.x * 128,
							map.pos.y * 128
						)
						val bottomRightAreaCorner = Vec2i(
							(map.pos.x + 1) * 128,
							(map.pos.y + 1) * 128
						)
						val texture = ChilliMapClient.renderer.getMapTexture(map)
						texture.updateIfNeeded()
						texture.draw(
							matrices,
							consumerProviders,
							max(0.0, topLeftAreaCorner.x - client.player!!.pos.x).toFloat(),
							max(0.0, topLeftAreaCorner.y - client.player!!.pos.z).toFloat(),
							min(128.0, bottomRightAreaCorner.x - client.player!!.pos.x).toFloat(),
							min(128.0, bottomRightAreaCorner.y - client.player!!.pos.z).toFloat(),
							max(0.05, client.player!!.pos.x - topLeftAreaCorner.x).toFloat() / 128,
							max(0.05, client.player!!.pos.z - topLeftAreaCorner.y).toFloat() / 128,
							min(127.95, client.player!!.pos.x - topLeftAreaCorner.x + 128).toFloat() / 128,
							min(127.95, client.player!!.pos.z - topLeftAreaCorner.y + 128).toFloat() / 128
						)
//						break
					}
					consumerProviders.drawCurrentLayer()
				}
			}
			matrices.pop()
			client.gameRenderer.lightmapTextureManager.enable()
		}
		if(ChilliMapClient.autoSaveCounter >= 3000) {
			ChilliMapClient.autoSaveCounter -= 1000
			ChilliMapClient.container!!.save(autoSave = true)
		}
	}

	@Environment(EnvType.CLIENT)
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
