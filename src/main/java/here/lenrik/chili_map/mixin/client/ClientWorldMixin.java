package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.ChiliMapClient;
import here.lenrik.chili_map.map.MapUpdater;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static org.spongepowered.asm.mixin.injection.At.Shift.AFTER;

@Mixin(ClientWorld.class)
abstract class ClientWorldMixin {
	@Inject(
			method = "tickEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/profiler/Profiler;pop()V",
					shift = AFTER
			)
	) private void updateMap (CallbackInfo info) {

		MapUpdater.updateContainer((World)(Object) this, Objects.requireNonNull(MinecraftClient.getInstance().player));
	}

	@Inject(
			method = "disconnect",
			at = @At(
					"RETURN"
			)
	) private void onDisconnect (CallbackInfo info) {
		ChiliMapClient.saveLoaded();
	}
}