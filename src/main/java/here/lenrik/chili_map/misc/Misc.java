package here.lenrik.chili_map.misc;

import here.lenrik.chili_map.Vec2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

import java.util.Iterator;

public class Misc{

	public static void renderMapIcons(MatrixStack matrices, Iterator<MapIcon> iconIterator, VertexConsumerProvider vertexConsumers, boolean hidePlayerIcons, RenderLayer MAP_ICONS_RENDER_LAYER){
		var k = 0;
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		while(true){
			MapIcon mapIcon;
			do {
				if(!iconIterator.hasNext()){
					vertexConsumers.getBuffer(MAP_ICONS_RENDER_LAYER);
//					textRenderer.draw(Text.of(" "), 0.0F, 0.0F, -1, false, matrices.peek().getModel(), vertexConsumers, false, 0x00000000, light);
					return;
				}

				mapIcon = iconIterator.next();
			} while(hidePlayerIcons && !mapIcon.isAlwaysRendered());

			matrices.push();
			matrices.translate(0.0F + mapIcon.getX() / 2.0F + 64.0F, 0.0F + mapIcon.getZ() / 2.0F + 64.0F, -0.019999999552965164D);
			matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion((mapIcon.getRotation() * 360) / 16.0F));
			matrices.scale(4.0F, 4.0F, 3.0F);
			matrices.translate(-0.125D, 0.125D, 0.0D);
			byte iconIndex = mapIcon.getTypeId();
			float left = (float) (iconIndex % 16) / 16.0F;
			float top = (float) (iconIndex / 16) / 16.0F;
			float right = (float) (iconIndex % 16 + 1) / 16.0F;
			float bottom = (float) (iconIndex / 16 + 1) / 16.0F;
			Matrix4f matrix4f2 = matrices.peek().getModel();
			float n = -0.001F;

			VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(MAP_ICONS_RENDER_LAYER);

			vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, (float) k * n).color(255, 255, 255, 255).texture(left, top).light(255).next();
			vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, (float) k * n).color(255, 255, 255, 255).texture(right, top).light(255).next();
			vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, (float) k * n).color(255, 255, 255, 255).texture(right, bottom).light(255).next();
			vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, (float) k * n).color(255, 255, 255, 255).texture(left, bottom).light(255).next();
			matrices.pop();
			if(mapIcon.getText() != null){
				Text text = mapIcon.getText();
				float width = textRenderer.getWidth(text);
				float widthNormalized = 25.0F / width;
				float p = MathHelper.clamp(widthNormalized, 0.0F, 2.0F / 3.0F);
				matrices.push();
				matrices.translate(0.0F + mapIcon.getX() / 2.0F + 64.0F - width * p / 2.0F, 0.0F + mapIcon.getZ() / 2.0F + 64.0F + 4.0F, -0.02500000037252903D);
				matrices.scale(p, p, 1.0F);
				matrices.translate(0.0D, 0.0D, 0.10000000149011612D);

//				DrawableHelper.fill(matrices, -1, -1, textRenderer.getWidth(text), textRenderer.fontHeight, 0x80000000);
//				matrices.translate(0.0D, 0.0D, 0.10000000149011612D);

				textRenderer.draw(text, 0.0F, 0.0F, 0x00ffffff, false, matrices.peek().getModel(), vertexConsumers, false, 0x08000000, 255);
				matrices.pop();
			}

			++k;
		}

	}
}
