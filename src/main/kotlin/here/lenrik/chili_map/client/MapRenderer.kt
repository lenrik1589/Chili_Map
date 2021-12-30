package here.lenrik.chili_map.client

import here.lenrik.chili_map.map.AreaMap
import net.minecraft.block.MapColor
import net.minecraft.client.render.*
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.texture.TextureManager
import net.minecraft.util.Identifier
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class MapRenderer(val textureManager: TextureManager) {
	private val MAP_ICONS_TEXTURE = Identifier("textures/map/map_icons.png")
	val MAP_ICONS_RENDER_LAYER = RenderLayer.getText(MAP_ICONS_TEXTURE)
	private val DEFAULT_IMAGE_WIDTH = 128
	private val DEFAULT_IMAGE_HEIGHT = 128
	private val mapTextures = HashMap<AreaMap, MapTexture>()
	var image = BufferedImage(128, 128, BufferedImage.TYPE_4BYTE_ABGR)

	fun getMapTexture(areaMap: AreaMap): MapTexture {
		return mapTextures.compute(
			areaMap
		) { map, texture -> (texture ?: MapTexture(map)) }!!
	}

	inner class MapTexture(val map: AreaMap) {
		private var renderLayer: RenderLayer
		private var texture: NativeImageBackedTexture = NativeImageBackedTexture(128, 128, true)

		init {
			val identifier: Identifier = this@MapRenderer.textureManager.registerDynamicTexture(
				"chili_map/",
				texture
			)
			this.renderLayer = RenderLayer.getText(identifier)
		}

		private fun updateTexture() {
			for (i in 0..127) {
				for (j in 0..127) {
					val k = j + i * 128
					val color = MapColor.getRenderColor(this.map.colors[k].toInt())
					texture.image!!.setPixelColor(j, i, color)
					image.raster.setPixel(
						j,
						i,
						listOf(color and 0xff, color shr 8 and 0xff, color shr 16 and 0xff, color shr 24 and 0xff).toIntArray()
					)
					//
					// #g##
				}
			}
			texture.upload()
			ImageIO.write(image, "PNG", File("file.png"))
		}

		fun draw() {
			updateTexture()
			val buffer = Tessellator.getInstance().buffer
			buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
			texture.bindTexture()
			buffer.vertex(0.0,0.0,0.0).texture(0f,0f).next()
			buffer.vertex(1.0,0.0,0.0).texture(1f,0f).next()
			buffer.vertex(1.0,1.0,0.0).texture(1f,1f).next()
			buffer.vertex(0.0,1.0,0.0).texture(0f,1f).next()
			Tessellator.getInstance().draw()
//			val i = 0
//			val j = 0
//			val f = 0.0f
//			val matrix4f = matrices.peek().model
//			val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
//			vertexConsumer.vertex(0.0f, 1.0f, -0.01f, 1.0f, 1f, 1f, 1f, 0.0f, 1.0f, 0, light, 0f,1f,0f)
//			vertexConsumer.vertex(1.0f, 1.0f, -0.01f, 1.0f, 1f, 1f, 1f, 1.0f, 1.0f, 0, light, 0f,1f,0f)
//			vertexConsumer.vertex(1.0f, 0.0f, -0.01f, 1.0f, 1f, 1f, 1f, 1.0f, 0.0f, 0, light, 0f,1f,0f)
//			vertexConsumer.vertex(0.0f, 0.0f, -0.01f, 1.0f, 1f, 1f, 1f, 0.0f, 0.0f, 0, light, 0f,1f,0f)
//			var k = 0
/*			for (mapIcon in this.map.getIcons()) {
				if (!hidePlayerIcons || mapIcon.isAlwaysRendered) {
					matrices.push()
					matrices.translate(
						(0.0f + mapIcon.x / 2.0f + 64.0f).toDouble(),
						(0.0f + mapIcon.z / 2.0f + 64.0f).toDouble(),
						-0.02
					)
					matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(mapIcon.rotation * 360 / 16.0f))
					matrices.scale(4.0f, 4.0f, 3.0f)
					matrices.translate(-0.125, 0.125, 0.0)
					val b = mapIcon.typeId
					val g = (b % 16 + 0).toFloat() / 16.0f
					val h = (b / 16 + 0).toFloat() / 16.0f
					val l = (b % 16 + 1).toFloat() / 16.0f
					val m = (b / 16 + 1).toFloat() / 16.0f
					val matrix4f2 = matrices.peek().model
					val n = -0.001f
					val vertexConsumer2 = vertexConsumers.getBuffer(MapRenderer.MAP_ICONS_RENDER_LAYER)
					vertexConsumer2.vertex(matrix4f2, -1.0f, 1.0f, k.toFloat() * -0.001f).color(255, 255, 255, 255).texture(g, h).light(light).next()
					vertexConsumer2.vertex(matrix4f2, 1.0f, 1.0f, k.toFloat() * -0.001f).color(255, 255, 255, 255).texture(l, h).light(light).next()
					vertexConsumer2.vertex(matrix4f2, 1.0f, -1.0f, k.toFloat() * -0.001f).color(255, 255, 255, 255).texture(l, m).light(light).next()
					vertexConsumer2.vertex(matrix4f2, -1.0f, -1.0f, k.toFloat() * -0.001f).color(255, 255, 255, 255).texture(g, m).light(light).next()
					matrices.pop()
					if (mapIcon.text != null) {
						val textRenderer = MinecraftClient.getInstance().textRenderer
						val text = mapIcon.text
						val o = textRenderer.getWidth(text).toFloat()
						val p = MathHelper.clamp(25.0f / o, 0.0f, 6.0f / 9.0f)
						matrices.push()
						matrices.translate(
							(0.0f + mapIcon.x.toFloat() / 2.0f + 64.0f - o * p / 2.0f).toDouble(),
							(0.0f + mapIcon.z.toFloat() / 2.0f + 64.0f + 4.0f).toDouble(),
							-0.025
						)
						matrices.scale(p, p, 1.0f)
						matrices.translate(0.0, 0.0, -0.1)
						textRenderer.draw(
							text,
							0.0f,
							0.0f,
							-1,
							false,
							matrices.peek().model,
							vertexConsumers,
							false,
							Int.MIN_VALUE,
							light
						)
						matrices.pop()
					}
					++k
				}
			}*/
		}

		fun close() {
			texture.close()
		}

	}
}
