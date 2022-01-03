package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.ChilliMapClient;
import here.lenrik.chili_map.client.MinimapRenderer;
import here.lenrik.chili_map.client.WorldMapScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
	@Inject(
			method = "<init>",
			at = @At(
					"RETURN"
			)
	) private void getRealTextureManager (RunArgs runArgs, CallbackInfo ci) {
		ChilliMapClient.Companion.setRenderer(new MinimapRenderer(MinecraftClient.getInstance().getTextureManager()));
	}

	@Inject(
			method = "handleInputEvents",
			at = @At("RETURN")
	) private void openWorldMap (CallbackInfo ci) {
		if(ChilliMapClient.Companion.getOpenWorldMapKey().isPressed()) {
			MinecraftClient.getInstance().setScreen(new WorldMapScreen(Objects.requireNonNull(ChilliMapClient.Companion.getContainer()).getName()));
		}
		if(ChilliMapClient.Companion.getZoomInKey().wasPressed()) {
			ChilliMapClient.Companion.setZoom(max(ChilliMapClient.Companion.getZoom() - 1, 0));
		}
		if(ChilliMapClient.Companion.getZoomOutKey().wasPressed()) {
			ChilliMapClient.Companion.setZoom(min(ChilliMapClient.Companion.getZoom() + 1, 4));
		}
	}
}