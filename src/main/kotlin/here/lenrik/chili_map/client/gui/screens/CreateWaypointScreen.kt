package here.lenrik.chili_map.client.gui.screens

import com.google.gson.JsonParseException
import here.lenrik.chili_map.client.gui.TextFieldWidget
import here.lenrik.chili_map.map.LevelMap
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.Vec3d

@Suppress("UNUSED_PARAMETER")
class CreateWaypointScreen(level: LevelMap, pos: Vec3d) : Screen(TranslatableText("Nyah")) {
	val nameField = TextInputField(0, 0, 0, 0, Validators.STF)
	val posX = TextInputField(0, 0, 0, 0, Validators.DOUBLE)
	val posY = TextInputField(0, 0, 0, 0, Validators.DOUBLE)
	val posZ = TextInputField(0, 0, 0, 0, Validators.DOUBLE)
	val type = TextInputField(0, 0, 0, 0, Validators.DOUBLE)

	override fun init() {
		addSelectableChild(TextFieldWidget(client?.textRenderer, 0, 0, 10, 10, Text.of("a")))
	}

	override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
		renderBackground(matrices)
		super.render(matrices, mouseX, mouseY, delta)
	}

	enum class Validators {
		DOUBLE {
			override fun <T> validate(input: String): T = try {
				input.toDouble() as T
			} catch (nfe: NumberFormatException) {
				var lastValid = input.length
				while (lastValid > 0) {
					try {
						input.substring(lastValid).toDouble()
						break
					} catch (nfe: NumberFormatException) {
						--lastValid
					}
				}
				throw InvalidInputError(lastValid, type = "Double")
			}
		},
		Literal {
			override fun <T> validate(input: String): T = try {
				Text.Serializer.fromJson(input) as T	
			} catch (e: JsonParseException) {
				throw InvalidInputError(0, type = "Text")
			}
		},
		STF {
			override fun <T> validate(input: String): T {
				TODO("Not yet implemented")
			}
		}
		;

		@Throws(InvalidInputError::class)
		abstract fun <T> validate(input: String): T

		inner class InvalidInputError(
			@Suppress("CanBeParameter") val pos: Int,
			message: String? = null,
			type: String? = null
		) : RuntimeException(message ?: "could not parse ${type ?: "input"} starting $pos")
	}

	inner class TextInputField(
		x: Int,
		y: Int,
		width: Int,
		height: Int,
		val validator: Validators,
		startingMessage: Text = Text.of("")
	) : ClickableWidget(x, y, width, height, startingMessage) {
		override fun appendNarrations(builder: NarrationMessageBuilder?) = Unit

	}
}