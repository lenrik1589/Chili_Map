package here.lenrik.chili_map.client;

import here.lenrik.chili_map.map.MapRegion;
import here.lenrik.chili_map.map.MapTexture;
import here.lenrik.chili_map.misc.Misc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class MinimapRenderer extends DrawableHelper{

	public MinimapRenderer( ){
	}

	public void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Misc.Vec2i pos, MapRegion state, boolean hidePlayerIcons){
		MinecraftClient.getInstance().textRenderer.draw(
				Text.of("" + state.centerX + "×" + state.centerZ + " " + MinecraftClient.getInstance().player.getBlockX() + "×" + MinecraftClient.getInstance().player.getBlockZ()),
				0, 128, -1, true, matrices.peek().getModel(), vertexConsumers, false, 0x00000000, 255);
		ChilliMapClient.mapContainer.getMapTexture(pos).draw(matrices, vertexConsumers, hidePlayerIcons);
		try{
			if((ChilliMapClient.updateCounter & 31) == 0)
				ImageIO.write(ChilliMapClient.mapContainer.getMapTexture(pos).toBufferedImage(), "PNG", new File("image.png"));
		} catch(IOException e) {
			e.printStackTrace();
		}
		var iconIterator = MinecraftClient.getInstance().world.getPlayers().stream().map(
				player -> state.isPlayerInbound(player)? new MapIcon(
						MapIcon.Type.PLAYER,
						(byte) ((player.getX() - state.centerX) * 2),
						(byte) ((player.getZ() - state.centerZ) * 2),
						(byte) Math.round((player.getHeadYaw() / 22.5) % 16),
						player.getDisplayName()
				) : null
		).filter(Objects::nonNull).iterator();
		Misc.renderMapIcons(matrices, iconIterator, vertexConsumers, hidePlayerIcons, MapTexture.MAP_ICONS_RENDER_LAYER);
	}

}
