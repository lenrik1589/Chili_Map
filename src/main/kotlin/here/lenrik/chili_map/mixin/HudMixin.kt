package here.lenrik.chili_map.mixin

import here.lenrik.chili_map.client.ChilliMapClient
import here.lenrik.chili_map.map.AreaMap
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.render.Camera
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Matrix4f
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(WorldRenderer::class)
class HudMixin : DrawableHelper() {
	@Inject(
		method = ["render"],
		at = [At(
			value = "RETURN"
		)]
	)
	fun addMap(
		matrices: MatrixStack,
		tickDelta: Float,
		limitTime: Long,
		renderBlockOutline: Boolean,
		camera: Camera,
		gameRenderer: GameRenderer,
		lightmapTextureManager: LightmapTextureManager,
		stack: Matrix4f, info: CallbackInfo
	) {
		if (ChilliMapClient.map == null || !ChilliMapClient.map!!.isInsideMap(MinecraftClient.getInstance().player!!.pos)) {
			ChilliMapClient.map = MinecraftClient.getInstance().player?.let { AreaMap(it.pos) }!!
		}
		val map = ChilliMapClient.map!!
		MinecraftClient.getInstance().gameRenderer.lightmapTextureManager.disable()
		ChilliMapClient.renderer.getMapTexture(map).draw()
		MinecraftClient.getInstance().gameRenderer.lightmapTextureManager.enable()
	}
}