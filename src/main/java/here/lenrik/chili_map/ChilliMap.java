package here.lenrik.chili_map;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

import java.nio.file.Path;

public class ChilliMap implements ModInitializer {

	public static final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chili_map");

	public static Logger LOGGER = LogManager.getLogger("Chill Map");

	public void onInitialize (ModContainer mod) {
	}
}
