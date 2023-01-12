package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.ChiliMapClient;
import here.lenrik.chili_map.client.gui.MinimapRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
	@Shadow
	private static MinecraftClient instance;

	@Inject(
			method = "<init>",
			at = @At(
					"RETURN"
			)
	)
	private void getRealTextureManager(RunArgs runArgs, CallbackInfo ci) {
		ChiliMapClient.setRenderer(new MinimapRenderer(MinecraftClient.getInstance().getTextureManager()));
	}
}