package here.lenrik.chili_map.mixin.client

import here.lenrik.chili_map.client.ChilliMapClient
import here.lenrik.chili_map.client.MapRenderer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.RunArgs
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MinecraftClient::class)
abstract class MinecraftClientMixin {
//	@Shadow
//	var textureManager: TextureManager = TextureManager(ResourceManager.Empty.INSTANCE)
	@Inject(
		method = ["<init>"],
		at = [At(
			"RETURN"
		)]
	) fun getRealTextureManager(rudArgs: RunArgs, info: CallbackInfo){
		ChilliMapClient.renderer = MapRenderer(MinecraftClient.getInstance().textureManager)
	}
}