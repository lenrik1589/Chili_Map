package here.lenrik.chili_map.mixin;

import here.lenrik.chili_map.ChilliMap;
import here.lenrik.chili_map.client.ChilliMapClient;
import here.lenrik.chili_map.map.MapContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
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
public class ConnectMixin{

	@Shadow @Final private ClientConnection connection;

	@Shadow private ClientWorld world;

	@Inject(
			method = "onGameJoin",
			at = @At("TAIL")
	)
	private void joinInjector(GameJoinS2CPacket packet, CallbackInfo info){
		ChilliMapClient.updateCounter = -1;
		ChilliMapClient.autosaveCounter = 0;
		String name = this.connection.isLocal()? MinecraftClient.getInstance().getServer().getSaveProperties().getLevelName() : connection.getAddress().toString().replace("/", " ");
		var mapsStoragePath = ChilliMap.configDir.resolve(name + ".dat");
		if (this.connection.isLocal()){
			ChilliMap.LOGGER.info("Opened world \"{}\"", name);
		}else {
			ChilliMap.LOGGER.info("Logged onto {}, store directory: {}", connection.getAddress(), mapsStoragePath);
		}
		ChilliMapClient.mapContainer = MapContainer.loadRegions(mapsStoragePath, name);
//		try{			ItemStack item = null;			PlayerInventory inventory = player.getInventory();			if(inventory.getStack(inventory.selectedSlot).getItem() instanceof FilledMapItem){			item = inventory.getStack(inventory.selectedSlot);		} else if(inventory.getStack(inventory.selectedSlot).getItem() instanceof AirBlockItem){			item = FilledMapItem.createMap(player.getServerWorld(), player.getBlockX(), player.getBlockZ(), (byte) 0, true, true);			inventory.setStack(inventory.selectedSlot, item);		} else {			return;		}			var newState = FilledMapItem.getOrCreateMapState(item, player.getServerWorld());			var data = IntBuffer.allocate(newState.colors.length * 4);			for(int i = 0, l = newState.colors.length; i < l; i++){			int b = newState.colors[i];			b = b < 0? b + 256 : b;			MapColor mapColor = MapColor.COLORS[b / 4];			int color = mapColor == MapColor.CLEAR? 0xffffff : mapColor.getRenderColor(b % 4);			data.put(color % 256);			data.put((color >> 8) % 256);			data.put((color >> 16) % 256);			var a = (color >> 24) % 256;			data.put(a);		}			try{			var image = new BufferedImage(128, 128, BufferedImage.TYPE_4BYTE_ABGR);			var raster = image.getWritableTile(0, 0);			int[] pixels = data.array();			raster.setPixels(0, 0, 128, 128, pixels);			image.setData(raster);			ImageIO.write(image, "PNG", new File("/projects/minecraft/chili_map/run/image.png"));		} catch(IOException e) {			e.printStackTrace();		}		} catch(Throwable t) {			t.printStackTrace();		}
	} // /kill @e[type=item]

//	@Inject(
//			method = "onDisconnected",
//			at = @At("RETURN")
//	) private void disconnectInjector(Text reason, CallbackInfo info){
//		ChilliMap.LOGGER.info("disconnected!");
//		ChilliMapClient.mapContainer.save();
//		ChilliMapClient.minimapRenderer.clearStateTextures();
//	}
}
