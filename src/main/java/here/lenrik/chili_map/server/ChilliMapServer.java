package here.lenrik.chili_map.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.server.DedicatedServerModInitializer;

@Environment(EnvType.SERVER)
public class ChilliMapServer implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer (ModContainer mod) {
	}
}
