package here.lenrik.chili_map.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBind;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import static java.util.List.of;

@Mixin(GameOptions.class)
@Environment(EnvType.CLIENT)
public class GameOptionsMixin {

	@Mutable @Shadow @Final public KeyBind[] allKeys;

//	@Inject(
//			method = "<init>",
//			at = @At(
//					"TAIL"
//			)
//	) private void addKeyBinds (CallbackInfo info) {
//		var list = new ArrayList<>(of(allKeys));
//		list.addAll(of(
//				ChilliMapClient.Companion.getConfig().getOpenWorldMapKey(),
//				ChilliMapClient.Companion.getConfig().getZoomInKey(),
//				ChilliMapClient.Companion.getConfig().getZoomOutKey()
//		));
//		allKeys = list.toArray(new KeyBind[0]);
//	}
}
