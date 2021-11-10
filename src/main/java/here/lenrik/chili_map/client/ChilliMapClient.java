package here.lenrik.chili_map.client;

import here.lenrik.chili_map.map.MapContainer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ChilliMapClient implements ClientModInitializer{
	public static MinimapRenderer minimapRenderer;
	public static MapContainer mapContainer;
	public static int updateCounter = -1;
	public static int autosaveCounter = 0;
	public static KeyBinding keyOpenWorldMap = new KeyBinding("option.chili_map.key.worldmap.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.gameplay");

	@Override
	public void onInitializeClient( ){
	}
}
