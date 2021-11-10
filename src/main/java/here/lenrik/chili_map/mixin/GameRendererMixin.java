package here.lenrik.chili_map.mixin;

import here.lenrik.chili_map.client.ChilliMapClient;
import here.lenrik.chili_map.client.MinimapRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(
			method = "<init>",
			at = @At(
					"RETURN"
			)
	) private void createMapOverlayRenderer(MinecraftClient minecraftClient, ResourceManager resourceManager, BufferBuilderStorage bufferBuilderStorage, CallbackInfo info){
		ChilliMapClient.minimapRenderer = new MinimapRenderer();
	}

}
