package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.ChilliMapClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HudMixin {
	@Inject(
			method = "render",
			at = @At(
					value = "RETURN"
			)
	)
	private void addMap (MatrixStack matrices, float tickDelta, CallbackInfo info) {
		ChilliMapClient.Companion.getRenderer().drawMinimap(matrices);
	}

}
