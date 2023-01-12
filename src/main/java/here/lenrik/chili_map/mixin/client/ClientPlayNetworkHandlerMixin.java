package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.ChiliMapClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
@Environment(EnvType.CLIENT)
public class ClientPlayNetworkHandlerMixin {

	@Shadow @Final private ClientConnection connection;

	@Inject(
			method = "onGameJoin",
			at = @At("TAIL")
	)
	private void joinInjector(GameJoinS2CPacket packet, CallbackInfo info){
		ChiliMapClient.Companion.onJoined(connection);
	} // /kill @e[type=item]

//	@Inject(
//			method = "onDisconnected",
//			at = @At("RETURN")
//	) private void disconnectInjector(Text reason, CallbackInfo info){
//		ChiliMap.LOGGER.info("disconnected!");
//		ChilliMapClient.mapContainer.save();
//		ChilliMapClient.minimapRenderer.clearStateTextures();
//	}
}
