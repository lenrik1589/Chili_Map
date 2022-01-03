package here.lenrik.chili_map.client

import here.lenrik.chili_map.Vec2i
import here.lenrik.chili_map.map.MapMarker
import here.lenrik.chili_map.map.MapMarker.Type.FRAME
import here.lenrik.chili_map.map.MapMarker.Type.PLAYER
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3f
import net.minecraft.util.math.Vec3i
import kotlin.math.floor

class RenderHelper {
	companion object {
		val MAP_ICONS_RENDER_LAYER: RenderLayer

		init {
			val MAP_ICONS_TEXTURE = Identifier("textures/map/map_icons.png")
			MAP_ICONS_RENDER_LAYER = RenderLayer.getText(MAP_ICONS_TEXTURE)
		}

	}
}