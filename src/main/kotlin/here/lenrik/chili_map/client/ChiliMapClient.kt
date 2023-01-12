package here.lenrik.chili_map.client

import fi.dy.masa.malilib.config.ConfigManager
import fi.dy.masa.malilib.config.options.ConfigHotkey
import fi.dy.masa.malilib.event.InitializationHandler
import fi.dy.masa.malilib.event.InputEventHandler
import fi.dy.masa.malilib.hotkeys.IKeybindManager
import fi.dy.masa.malilib.hotkeys.IKeybindProvider
import here.lenrik.chili_map.ChiliMap
import here.lenrik.chili_map.ChiliMap.Companion.LOGGER
import here.lenrik.chili_map.client.compat.journeymap.ServerPlayerLocationPacket
import here.lenrik.chili_map.client.config.ChiliMapClientConfig
import here.lenrik.chili_map.client.config.ChiliMapConfigScreen
import here.lenrik.chili_map.client.gui.MinimapRenderer
import here.lenrik.chili_map.client.gui.screens.CreateWaypointScreen
import here.lenrik.chili_map.client.gui.screens.WorldMapScreen
import here.lenrik.chili_map.map.MapContainer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.TextureManager
import net.minecraft.network.ClientConnection
import net.minecraft.resource.ResourceManager
import net.minecraft.util.math.Vec3d
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.full.memberProperties

@Environment(EnvType.CLIENT)
class ChiliMapClient : ClientModInitializer {
	override fun onInitializeClient(mod: ModContainer) {
		InitializationHandler.getInstance().registerInitializationHandler {
			ConfigManager.getInstance().registerConfigHandler(ChiliMap.ModId, config)
			InputEventHandler.getKeybindManager().registerKeybindProvider(object : IKeybindProvider {
				override fun addHotkeys(manager: IKeybindManager) {
					manager.addHotkeysForCategory(
						"ChiliMap",
						"adsads",
						ChiliMapClientConfig::class.memberProperties.filter { it.isFinal }
							.mapNotNull { it.get(config) as? ConfigHotkey })
				}

				override fun addKeysToMap(manager: IKeybindManager) =
					ChiliMapClientConfig::class.memberProperties.filter { it.isFinal }
						.mapNotNull { it.get(config) as? ConfigHotkey }.forEach {
							manager.addKeybindToMap(it.keybind)
						}
			})
			config.openWorldMapKey.keybind.setCallback { _, _ ->
				client.setScreen(WorldMapScreen(container!!.name))
				true
			}
			config.openSettingsKey.keybind.setCallback { _, _ ->
				MinecraftClient.getInstance().setScreen(ChiliMapConfigScreen(MinecraftClient.getInstance().currentScreen))
				true
			}
			config.zoomInKey.keybind.setCallback { _, _ ->
				config.minimapZoom = max(config.minimapZoom - 1, 0)
				true
			}
			config.zoomOutKey.keybind.setCallback { _, _ ->
				config.minimapZoom = min(config.minimapZoom + 1, 4)
				true
			}
			config.createWaypointKey.keybind.setCallback { _, _ ->
				client.setScreen(
					CreateWaypointScreen(
						container!!.getLevel(
							client.world!!.registryKey.value
						),
						client.cameraEntity?.pos?: client.player?.pos?: Vec3d.ZERO
					)
				)
				true
			}
		}
		var counter = 0
		ClientPlayNetworking.registerGlobalReceiver(ServerPlayerLocationPacket.CHANNEL) { _, _, buf, _ ->
			if ((counter) == 0) {
				val bytes = buf.writtenBytes
				LOGGER.info("received packet from journeymap:player_loc, {}", bytes)
				buf.readerIndex(bytes.size - 1)
			}
			counter = (counter + 1) % 100
		}
	}

	companion object {
		fun loadSaved(mapsStoragePath: Path, name: String) {
			container = MapContainer(name, mapsStoragePath.toAbsolutePath())
		}

		fun onJoined(connection: ClientConnection) {
			autoSaveCounter = 0
			val name =
				if (connection.isLocal) (MinecraftClient.getInstance().server as SessionAccessor?)!!.getSession().directoryName else connection.address.toString()
					.replace("/", "_")
			val mapsStoragePath = ChiliMap.configDir.resolve(name)
			if (connection.isLocal) {
				LOGGER.info("Opened world \"$name\"")
			} else {
				LOGGER.info("Logged onto ${connection.address}, store directory: $mapsStoragePath")
			}
			loadSaved(mapsStoragePath, name)
			LOGGER.info("loaded map data successfully exiting.")
		}

		@JvmStatic
		fun saveLoaded() {
			renderer.clear()
			container?.save()
		}

		var updateCounter: Int = 0

		@JvmStatic
		var container: MapContainer? = null
		var autoSaveCounter: Int = 0

		@JvmStatic
		var renderer = MinimapRenderer(TextureManager(ResourceManager.Empty.INSTANCE))

		@JvmStatic
		val config = ChiliMapClientConfig()

		@JvmStatic
		val client: MinecraftClient get() = MinecraftClient.getInstance()

		@JvmStatic
		var renderedPlayerList: Boolean = false
	}

}

//private fun PacketByteBuf.toJson(): String {
//	when(this.readBlockPos())
//}
