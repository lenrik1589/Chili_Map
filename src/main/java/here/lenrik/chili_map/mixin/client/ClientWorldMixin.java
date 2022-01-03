package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.ChilliMapClient;
import here.lenrik.chili_map.map.MapUpdater;
import kotlin.NotImplementedError;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.function.Supplier;

import static org.spongepowered.asm.mixin.injection.At.Shift.AFTER;

@Mixin(ClientWorld.class)
abstract class ClientWorldMixin extends World {
	protected ClientWorldMixin (MutableWorldProperties mutableWorldProperties, RegistryKey<World> registryKey, DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l) {
		super(mutableWorldProperties, registryKey, dimensionType, supplier, bl, bl2, l);
		throw new NotImplementedError();
	}

	@Inject(
			method = "tickEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/profiler/Profiler;pop()V",
					shift = AFTER
			)
	) private void updateMap (CallbackInfo info) {
		MapUpdater.Companion.updateColors(this, Objects.requireNonNull(MinecraftClient.getInstance().player));
	}

	@Inject(
			method = "disconnect",
			at = @At(
					"RETURN"
			)
	) private void onDisconnect (CallbackInfo info) {
		ChilliMapClient.Companion.saveLoaded();
	}
}