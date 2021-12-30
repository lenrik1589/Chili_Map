package here.lenrik.chili_map.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.ClientModInitializer
import here.lenrik.chili_map.map.AreaMap
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.TextureManager
import net.minecraft.resource.ResourceManager

@Environment(EnvType.CLIENT)
class ChilliMapClient : ClientModInitializer {
	override fun onInitializeClient() {}

	companion object {
		var map: AreaMap? = null
		var renderer = MapRenderer(TextureManager(ResourceManager.Empty.INSTANCE))
	}
}