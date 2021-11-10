package here.lenrik.chili_map.mixin;

import here.lenrik.chili_map.ChilliMap;
import here.lenrik.chili_map.client.ChilliMapClient;
import here.lenrik.chili_map.client.WorldMapScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin{
	@Inject(
			method = "handleInputEvents",
			at = @At("RETURN")
	) private void openWorldMap(CallbackInfo info){
		if(ChilliMapClient.keyOpenWorldMap.isPressed()){
			MinecraftClient.getInstance().setScreen(new WorldMapScreen(ChilliMapClient.mapContainer.getName()));
		}
	}

	@Inject(
			method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",
			at = @At(
					"RETURN"
			)
	) private void saveInjector(Screen screen, CallbackInfo info){
	}
}
