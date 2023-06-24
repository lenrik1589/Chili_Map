package here.lenrik.chili_map.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.joml.Matrix4f

@Suppress("NAME_SHADOWING")
fun fill(matrices: MatrixStack, x1: Number, y1: Number, x2: Number, y2: Number, color: Int) {
	val (x1, x2) = with(if(x1.toDouble() < x2.toDouble()) x2 to x1 else x1 to x2){ first.toFloat() to second.toFloat() }
	val (y1, y2) = with(if(y1.toDouble() < y2.toDouble()) y2 to y1 else y1 to y2){ first.toFloat() to second.toFloat() }

	val f = (color shr 24 and 0xFF) / 255.0F
	val g = (color shr 16 and 0xFF) / 255.0F
	val h = (color shr 8 and 0xFF) / 255.0F
	val j = (color and 0xFF) / 255.0F
	val bufferBuilder = Tessellator.getInstance().bufferBuilder
	RenderSystem.enableBlend()
//	RenderSystem.disableTexture()
	RenderSystem.defaultBlendFunc()
	RenderSystem.setShader(GameRenderer::getPositionColorShader)
	bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
	val matrix = matrices.peek().model
	bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(g, h, j, f).next()
	bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(g, h, j, f).next()
	bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(g, h, j, f).next()
	bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(g, h, j, f).next()
	bufferBuilder.end()
	BufferRenderer.draw(bufferBuilder.end())
//	RenderSystem.enableTexture()
	RenderSystem.disableBlend()
}

fun BufferBuilder.vertex(matrix: Matrix4f, x: Double, y: Double, fl: Float): VertexConsumer = vertex(matrix, x.toFloat(), y.toFloat(), fl)

val MAP_ICONS_RENDER_LAYER: RenderLayer by lazy {
	val MAP_ICONS_TEXTURE = Identifier("textures/map/map_icons.png")
	RenderLayer.getText(MAP_ICONS_TEXTURE)

}