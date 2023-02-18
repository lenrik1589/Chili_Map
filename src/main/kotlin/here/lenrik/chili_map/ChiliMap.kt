package here.lenrik.chili_map

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.QuiltLoader
import java.nio.file.Path

class ChiliMap : org.quiltmc.qsl.base.api.entrypoint.ModInitializer, net.fabricmc.api.ModInitializer {
	override fun onInitialize(mod: ModContainer) {
		ModId = mod.metadata().id()
	}

	override fun onInitialize() {
		ModId = "chili_map"
	}

	companion object {
		@JvmStatic
		val configDir: Path = QuiltLoader.getConfigDir().resolve("chili_map")
		@JvmStatic
		var LOGGER: Logger = LogManager.getLogger("Chill Map")
		@JvmStatic
		lateinit var ModId: String
	}
}