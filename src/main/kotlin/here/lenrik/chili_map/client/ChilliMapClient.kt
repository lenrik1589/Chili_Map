package here.lenrik.chili_map.client

import here.lenrik.chili_map.ChilliMap
import here.lenrik.chili_map.ChilliMap.LOGGER
import here.lenrik.chili_map.map.MapContainer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.texture.TextureManager
import net.minecraft.network.ClientConnection
import net.minecraft.resource.ResourceManager
import org.lwjgl.glfw.GLFW
import java.nio.file.Path

@Environment(EnvType.CLIENT)
class ChilliMapClient : ClientModInitializer {
	override fun onInitializeClient() {

	}

	companion object {
		fun loadSaved(mapsStoragePath: Path, name: String) {
			container = MapContainer(name, mapsStoragePath.toAbsolutePath())
		}

		fun onJoined(connection: ClientConnection) {
			autoSaveCounter = 0
			val name =
				if (connection.isLocal)
					MinecraftClient.getInstance().server!!.saveProperties.levelName
				else
					connection.address.toString().replace("/", "_")
			val mapsStoragePath = ChilliMap.configDir.resolve(name)
			if (connection.isLocal) {
				LOGGER.info("Opened world \"$name\"")
			} else {
				LOGGER.info("Logged onto ${connection.address}, store directory: $mapsStoragePath")
			}
			loadSaved(mapsStoragePath, name)
			LOGGER.info("loaded map data successfully exiting.")
//			MinecraftClient.getInstance().stop()
		}

		fun saveLoaded() {
			renderer.clear()
			container?.save()
		}

		val openWorldMapKey = KeyBinding("option.chili_map.key.world_map.open", GLFW.GLFW_KEY_N, "key.categories.gameplay")
		val zoomInKey = KeyBinding("option.chili_map.mini_map.zoom_in", GLFW.GLFW_KEY_MINUS, "key.categories.gameplay")
		val zoomOutKey = KeyBinding("option.chili_map.mini_map.zoom_out", GLFW.GLFW_KEY_EQUAL, "key.categories.gameplay")
		var updateCounter: Int = 0
		var container: MapContainer? = null
		var autoSaveCounter: Int = 0
		var zoom = 0
		var renderer = MinimapRenderer(TextureManager(ResourceManager.Empty.INSTANCE))
	}
}