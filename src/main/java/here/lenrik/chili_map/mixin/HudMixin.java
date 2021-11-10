package here.lenrik.chili_map.mixin;

import here.lenrik.chili_map.client.ChilliMapClient;
import here.lenrik.chili_map.misc.Misc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class HudMixin{
	@Shadow @Final private MinecraftClient client;

	@Inject(
			method = "render",
			at = @At("RETURN")
	)
	private void injectMapOverlay(MatrixStack matrices, float tickDelta, CallbackInfo info){
		assert client.world != null && client.player != null: "not in-game context";
		Misc.Vec2i pos = Misc.mapPos(client.player.getPos());
		ChilliMapClient.minimapRenderer.draw(
				matrices,
				client.getBufferBuilders().getEntityVertexConsumers(),
				pos,
				ChilliMapClient.mapContainer.getRegion(pos),
				false
		);
	}
}
