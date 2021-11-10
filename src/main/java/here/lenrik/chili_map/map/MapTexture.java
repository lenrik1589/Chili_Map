package here.lenrik.chili_map.map;

import here.lenrik.chili_map.misc.Misc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

import java.awt.image.BufferedImage;

@Environment(EnvType.CLIENT) public
class MapTexture implements AutoCloseable{
	private static final int DEFAULT_IMAGE_HEIGHT = 128;
	private static final int DEFAULT_IMAGE_WIDTH = 128;
	private static final Identifier MAP_ICONS_TEXTURE = new Identifier("textures/map/map_icons.png");
	public static final RenderLayer MAP_ICONS_RENDER_LAYER = RenderLayer.getText(MAP_ICONS_TEXTURE);
	private MapRegion state;
	private final NativeImageBackedTexture texture;
	private final RenderLayer renderLayer;
	private boolean needsUpdate = true;

	MapTexture(MapContainer mapContainer, Misc.Vec2i pos, MapRegion mapRegion){
		state = mapRegion;
		texture = new NativeImageBackedTexture(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, true);
		Identifier identifier = mapContainer.textureManager.registerDynamicTexture("chili_map/" + mapRegion.getDimension().getNamespace() +"_" + mapRegion.getDimension().getPath() + "/" + pos.x() + "_" + pos.y(), texture);
		renderLayer = RenderLayer.getText(identifier);
	}

	void setState(MapRegion state){
		this.needsUpdate |= this.state != state;
		this.state = state;
	}


	public void setNeedsUpdate( ){
		needsUpdate = true;
	}

	void updateTexture( ){
		for(int y = 0; y < 128; ++y){
			for(int x = 0; x < 128; ++x){
				int index = x + y * 128;
				int color = this.state.colors[index] & 255;
				if(color / 4 == 0){
					texture.getImage().setColor(x, y, 0);
				} else {
					texture.getImage().setColor(x, y, MapColor.COLORS[color / 4].getRenderColor(color & 3));
				}
			}
		}

		texture.upload();
	}

	public void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, boolean hidePlayerIcons){
		if(needsUpdate){
			updateTexture();
			needsUpdate = false;
		}
		Matrix4f matrix4f = matrices.peek().getModel();

		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.renderLayer);
		float z = -0.01F;
		vertexConsumer.vertex(matrix4f, 0, 128, z).color(255, 255, 255, 255).texture(0.0F, 1.0F).light(255).next();
		vertexConsumer.vertex(matrix4f, 128, 128, z).color(255, 255, 255, 255).texture(1.0F, 1.0F).light(255).next();
		vertexConsumer.vertex(matrix4f, 128, 0, z).color(255, 255, 255, 255).texture(1.0F, 0.0F).light(255).next();
		vertexConsumer.vertex(matrix4f, 0, 0, z).color(255, 255, 255, 255).texture(0.0F, 0.0F).light(255).next();

		matrices.translate(0, 0, -z);

		Misc.renderMapIcons(matrices, state.getIcons().iterator(), vertexConsumers, hidePlayerIcons, MAP_ICONS_RENDER_LAYER);
	}

	public BufferedImage toBufferedImage( ){
		var img = new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		NativeImage image = texture.getImage();
		for(int i = 0; i < DEFAULT_IMAGE_WIDTH; i++){
			for(int j = 0; j < DEFAULT_IMAGE_HEIGHT; j++){
				int color = image.getColor(i, j);
				img.setRGB(i, j, (color & 0xff00ff00) | ((color & 0xff) << 16) | ((color & 0xff0000) >> 16) /*(image.getRed(i, j) << 16) | (image.getGreen(i, j) << 8) | image.getBlue(i, j)*/);
			}
		}
		return img;
	}

	public void close( ){
		texture.close();
	}
}
