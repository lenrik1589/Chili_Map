package here.lenrik.chili_map.client

import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class RenderHelper {
	companion object {
		val MAP_ICONS_RENDER_LAYER: RenderLayer

		init {
			val MAP_ICONS_TEXTURE = Identifier("textures/map/map_icons.png")
			MAP_ICONS_RENDER_LAYER = RenderLayer.getText(MAP_ICONS_TEXTURE)
		}

	}
}