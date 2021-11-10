package here.lenrik.chili_map.client;

import here.lenrik.chili_map.map.MapTexture;
import here.lenrik.chili_map.misc.Misc;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

import static net.minecraft.util.Formatting.GREEN;
import static net.minecraft.util.Formatting.WHITE;

public class WorldMapScreen extends Screen{
	private float xOffset;
	private float zOffset;
	private double scale = 1;
	private int scaleWorkaround;

	public WorldMapScreen(Text text){
		super(text);
		assert MinecraftClient.getInstance().player != null : "Player must be present";
		scaleWorkaround = (/*0.f + */MinecraftClient.getInstance().getWindow().getWidth()) / MinecraftClient.getInstance().getWindow().getScaledWidth();
		xOffset = -MinecraftClient.getInstance().player.getBlockX();
		zOffset = -MinecraftClient.getInstance().player.getBlockZ();
	}

	@Override public void render(MatrixStack matrices, int mouseX, int mouseY, float delta){
		MinecraftClient client = MinecraftClient.getInstance();
		client.getProfiler().push("chili_map_worldmap_render");
		renderBackground(matrices);
		assert client.player != null && client.world != null : "call this method in the actual world with a player";
		int guiScale = client.options.guiScale;
		guiScale = guiScale == 0? scaleWorkaround : guiScale;
		super.render(matrices, mouseX, mouseY, delta);
		var vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
		if(client.options.debugEnabled){
			client.textRenderer.draw(matrices, Text.of("" + (int) (xOffset + currentMouseX / scale * guiScale) + ":" + (int) (zOffset + currentMouseY / scale * guiScale) + "/" + (float) scale), 0, 0, 0xffffff);
//			client.textRenderer.draw(matrices, Text.of("" + (int) ((width / 2 - mouseX) * guiScale / scale) + ":" + (int) ((height / 2 - mouseY) * guiScale / scale) + "/" + (float) scale + ":" + guiScale), 0, client.textRenderer.fontHeight, 0xffffff);
		}
		matrices.push();
		matrices.translate(currentMouseX + width / 2, currentMouseY + height / 2, 0);
		matrices.scale((float) scale, (float) scale, 1);
		matrices.scale(1.f / guiScale, 1.f / guiScale, 1);
		matrices.translate(xOffset, zOffset, 0);
		int shownMaps = 0;
		for(
				Misc.Vec2i mapPos :
				ChilliMapClient.mapContainer.getMapsPos(
						client.world.getRegistryKey().getValue()
				).stream().sorted(
						Comparator.comparingInt(vec -> vec.y() + vec.y())
				).toList()
		){
			int l = (int) (mapPos.x() * 128 - 64 + xOffset + currentMouseX / scale * guiScale),
					r = (int) (mapPos.x() * 128 + 64 + xOffset + currentMouseX / scale * guiScale),
					t = (int) (mapPos.y() * 128 - 64 + zOffset + currentMouseY / scale * guiScale),
					b = (int) (mapPos.y() * 128 + 64 + zOffset + currentMouseY / scale * guiScale);
			if(
					r < -width / scale / 2 * guiScale ||
					b < -height / scale / 2 * guiScale ||
					l >= width / scale / 2 * guiScale ||
					t >= height / scale / 2 * guiScale
			)
				continue;
			++shownMaps;
			matrices.push();
			MapTexture tex = ChilliMapClient.mapContainer.getMapTexture(mapPos);
			matrices.translate(
					mapPos.x() * 128 - 64,
					mapPos.y() * 128 - 64,
					0
			);
			tex.draw(matrices, vertexConsumers, false);
			if(client.options.debugEnabled){
				matrices.push();
				matrices.translate(0, 0, -1000);
//				for(int i = 0; i < 256; i++){ fill(matrices, i < 128? i : 127, i < 128? 0 : i & 127, i < 128? i + 1 : 128, i < 128? 1 : (i & 127) + 1, 0xd0000000 | i | (255 - i) << 16); fill(matrices, i < 128? 0 : i & 127, i < 128? i : 127, i < 128? 1 : (i & 127) + 1, i < 128? i + 1 : 128, 0xd0000000 | i | (255 - i) << 16);}
				int i = (int) (128 * scale / guiScale + .1);
				matrices.scale(guiScale / (float) scale, guiScale / (float) scale, 1);
				fill(matrices, 0, 0, i - 1, 1, 0x50FF0000);
				fill(matrices, 0, 1, 1, i, 0x50FF0000);
				fill(matrices, i - 1, 0, i, i - 1, 0x500000FF);
				fill(matrices, 1, i - 1, i, i, 0x500000FF);
				if(scale > .25){
					matrices.scale(.5F, .5F, 1);
					matrices.translate(0, 0, 1000);
					client.textRenderer.draw(matrices, Text.of(mapPos.toString()).copy().formatted(mapPos.equals(Misc.mapPos(client.player.getPos()))? GREEN : WHITE), 0, 0, 0xffffff);
				}
				matrices.pop();
			}
			matrices.pop();
		}

		var iconIterator = MinecraftClient.getInstance().world.getPlayers().stream().map(
				player -> {
					var pos = Misc.mapPos(player.getPos().add(64, 64, 0));
					return new MapIcon(
							MapIcon.Type.PLAYER,
							(byte) ((player.getX() - pos.x() * 128) * 2 + 2),
							(byte) ((player.getZ() - pos.y() * 128) * 2),
							(byte) Math.round((player.getHeadYaw() / 22.5) % 16),
							player.getDisplayName()
					);
				}
		).iterator();
		Misc.renderMapIcons(matrices, iconIterator, vertexConsumers, false, MapTexture.MAP_ICONS_RENDER_LAYER);
		matrices.pop();
		if(client.options.debugEnabled){
			client.textRenderer.draw(matrices, Text.of(String.valueOf(shownMaps)), 0, textRenderer.fontHeight, 0xffffff);
		}
		client.getProfiler().pop();
	}

	@Override public boolean keyReleased(int keyCode, int scanCode, int modifiers){
		switch(keyCode){
			case GLFW.GLFW_KEY_RIGHT -> xOffset = (int) xOffset + 1;
			case GLFW.GLFW_KEY_LEFT -> xOffset = (int) xOffset - 1;
			case GLFW.GLFW_KEY_UP -> zOffset = (int) zOffset + 1;
			case GLFW.GLFW_KEY_DOWN -> zOffset = (int) zOffset - 1;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	double currentMouseX = 0, currentMouseY = 0;

	@Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY){
		currentMouseX += deltaX;
		currentMouseY += deltaY;
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override public boolean mouseReleased(double mouseX, double mouseY, int button){
		int guiScale = client.options.guiScale;
		guiScale = guiScale == 0? scaleWorkaround : guiScale;
		xOffset += currentMouseX / scale * guiScale;
		zOffset += currentMouseY / scale * guiScale;
		currentMouseX = 0;
		currentMouseY = 0;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override public void resize(MinecraftClient client, int width, int height){
		scaleWorkaround = (MinecraftClient.getInstance().getWindow().getWidth()) / MinecraftClient.getInstance().getWindow().getScaledWidth();
		super.resize(client, width, height);
	}

	@Override public boolean mouseScrolled(double mouseX, double mouseY, double amount){
//		ChilliMap.LOGGER.info("scrolled by {}", amount);
		double sqr2 = 1.4142135623730951;
		double scalingFactor = ((1 + amount) * sqr2 + (1 - amount) / sqr2) / 2;
		double pScale = scale;
		scale *= scalingFactor;
		scale = scale > 128.0001? scale / sqr2 : scale < 1. / (1 << 8)? scale * sqr2 : scale;
		scalingFactor = pScale == scale? 1 : scalingFactor;
		int guiScale = client.options.guiScale;
		guiScale = guiScale == 0? scaleWorkaround : guiScale;
		xOffset += (float) ((width / 2 - mouseX) * guiScale * (scalingFactor - 1) / scale);
		zOffset += (float) ((height / 2 - mouseY) * guiScale * (scalingFactor - 1) / scale);
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override public boolean isPauseScreen( ){
		return false;
	}
}
