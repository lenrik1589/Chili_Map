package here.lenrik.chili_map.mixin.client

import here.lenrik.chili_map.ChilliMap
import here.lenrik.chili_map.client.ChilliMapClient
import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.profiler.Profiler
import net.minecraft.util.registry.RegistryKey
import net.minecraft.world.MutableWorldProperties
import net.minecraft.world.World
import net.minecraft.world.dimension.DimensionType
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.At.Shift.BEFORE
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.function.Supplier

@Mixin(ClientWorld::class)
abstract class ClientWorldMixin(p1: MutableWorldProperties?, p2: RegistryKey<World>?, p3: DimensionType?, p4: Supplier<Profiler>?, bl: Boolean, bl2: Boolean, l: Long) : World(p1, p2, p3, p4, bl, bl2, l) {
	@Inject(
		method = ["tickEntities"],
		at = [At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/profiler/Profiler;pop()V",
			shift = BEFORE
		)]
	) fun updateMap(info: CallbackInfo){
		ChilliMapClient.map?.updateColors(this, MinecraftClient.getInstance().player!!)
	}
}