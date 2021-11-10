package here.lenrik.chili_map.mixin;

import here.lenrik.chili_map.client.ChilliMapClient;
import here.lenrik.chili_map.misc.Misc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World{

	protected ClientWorldMixin(MutableWorldProperties worldProperties, RegistryKey<World> key, DimensionType type, Supplier<Profiler> profilerSupplier, boolean client, boolean debug, long seed){
		super(worldProperties, key, type, profilerSupplier, client, debug, seed);
		throw new IllegalStateException("Mixin class cannot be instantiated");
	}

	@Inject(
			method = "tick",
			at = @At(
					"RETURN"
			)
	) private void updateClientSideMap(BooleanSupplier shouldKeepTicking, CallbackInfo info){
		var pos = Misc.mapPos(MinecraftClient.getInstance().player.getPos());
		if(((ChilliMapClient.updateCounter = (++ChilliMapClient.updateCounter) % (1 << 4)) & 0) == 0){
			for(int i = -1; i <= 1; ++i){
				for(int j = -1; j <= 1; ++j){
					Misc.Vec2i other = new Misc.Vec2i(pos.x() + i, pos.y() + j);
					ChilliMapClient.mapContainer.updateRegion(this, other);
					ChilliMapClient.mapContainer.updateTexture(other, i == 0 && j == 0);
				}
			}
		}
		if((ChilliMapClient.autosaveCounter = (++ChilliMapClient.autosaveCounter) % (20 * 50)) == 0){
			ChilliMapClient.mapContainer.autoSave();
		}
	}

	@Inject(
			method = "disconnect",
			at = @At(
					"RETURN"
			)
	) private void saveOnDisconnect(CallbackInfo info){
		ChilliMapClient.mapContainer.save();
		ChilliMapClient.mapContainer.reset();
	}
}
