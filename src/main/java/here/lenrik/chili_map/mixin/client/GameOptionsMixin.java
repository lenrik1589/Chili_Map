package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.ChilliMapClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

import static java.util.List.of;

@Mixin(GameOptions.class)
@Environment(EnvType.CLIENT)
public class GameOptionsMixin {

	@Mutable @Shadow @Final public KeyBinding[] allKeys;

	@Inject(
			method = "<init>",
			at = @At(
					"TAIL"
			)
	) private void addKeyBinds (CallbackInfo info) {
		var list = new ArrayList<>(of(allKeys));
		list.addAll(of(
				ChilliMapClient.Companion.getOpenWorldMapKey(),
				ChilliMapClient.Companion.getZoomInKey(),
				ChilliMapClient.Companion.getZoomOutKey()
		));
		allKeys = list.toArray(new KeyBinding[0]);
	}
}
