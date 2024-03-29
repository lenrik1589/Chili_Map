package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.ChiliMapClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
		ChiliMapClient.Companion.getRenderer().drawMinimap(matrices, tickDelta);
	}

}
