package here.lenrik.chili_map.client.gui

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.Screen.hasShiftDown
import net.minecraft.client.gui.screen.Screen.isSelectAll
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.*
import net.minecraft.util.Util
import net.minecraft.util.math.MathHelper
import org.quiltmc.loader.api.minecraft.ClientOnly
import java.util.*
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Suppress("SameParameterValue", "NAME_SHADOWING")
@ClientOnly
class TextFieldWidget(
	private val textRenderer: TextRenderer?,
	x: Int,
	y: Int,
	width: Int,
	height: Int,
	startingMessage: Text?,
	copyFrom: TextFieldWidget? = null
) : ClickableWidget(x, y, width, height, startingMessage), Drawable, Element {
	var text = ""
		set(text) {
			if (textPredicate.test(text)) {
				field = if (text.length > maxLength) {
					text.substring(0, maxLength)
				} else {
					text
				}
				setCursorToEnd()
				selectionEnd = selectionStart
				onChanged(text)
			}
		}
	var maxLength = 32
		set(maxLength) {
		field = maxLength
		if (text.length > maxLength) {
			text = text.substring(0, maxLength)
			onChanged(text)
		}
	}
	var focusedTicks = 0
	var drawsBackground = true
	var focusUnlocked = true
	var isEditable = true

	var selecting = false
	var firstCharacterIndex = 0
	var selectionStart = 0
		set(cursor) {
			field = MathHelper.clamp(cursor, 0, text.length)
		}
	var selectionEnd = 0
		set(index) {
			val textLength = text.length
			field = MathHelper.clamp(index, 0, textLength)
			if (textRenderer != null) {
				if (firstCharacterIndex > textLength) {
					firstCharacterIndex = textLength
				}
				val string = textRenderer.trimToWidth(text.substring(firstCharacterIndex), innerWidth)
				val k = string.length + firstCharacterIndex
				if (selectionEnd == firstCharacterIndex) {
					firstCharacterIndex -= textRenderer.trimToWidth(text, innerWidth, true).length
				}
				if (selectionEnd > k) {
					firstCharacterIndex += selectionEnd - k
				} else if (selectionEnd <= firstCharacterIndex) {
					firstCharacterIndex -= firstCharacterIndex - selectionEnd
				}
				firstCharacterIndex = MathHelper.clamp(firstCharacterIndex, 0, textLength)
			}
		}
	var editableColor = 14737632
	var uneditableColor = 7368816
	var suggestion: String? = null
	var changedListener: Consumer<String>? = null

	var textPredicate = Predicate(Objects::nonNull)

	var renderTextProvider = BiFunction { string: String, _: Int ->
		OrderedText.forward(string, Style.EMPTY)
	}

	init {
		if (copyFrom != null) {
			text = copyFrom.text
		}
	}


	fun tick() {
		++focusedTicks
	}

	override fun getNarrationMessage() = TranslatableText("gui.narrate.editBox", message, this.text)

	val selectedText: String
		get() {
			val i = min(selectionStart, selectionEnd)
			val j = max(selectionStart, selectionEnd)
			return text.substring(i, j)
		}


	fun write(text: String) {
		val i = min(selectionStart, selectionEnd)
		val j = max(selectionStart, selectionEnd)
		val k = maxLength - this.text.length - (i - j)
		var string = buildString {
			text.filter(::isValid).forEach(this::append)
		}
		var l = string.length
		if (k < l) {
			string = string.substring(0, k)
			l = k
		}
		val string2 = StringBuilder(this.text).replace(i, j, string).toString()
		if (textPredicate.test(string2)) {
			this.text = string2
			selectionStart = i + l
			selectionEnd = selectionStart
			onChanged(this.text)
		}
	}

	private fun isValid(crah: Char) = crah > ' ' && crah != 127.toChar()

	private fun onChanged(newText: String) {
		changedListener?.accept(newText)
	}

	private fun erase(offset: Int) {
		if (Screen.hasControlDown()) {
			eraseWords(offset)
		} else {
			eraseCharacters(offset)
		}
	}

	fun eraseWords(wordOffset: Int) {
		if (text.isNotEmpty()) {
			if (selectionEnd != selectionStart) {
				write("")
			} else {
				eraseCharacters(getWordSkipPosition(wordOffset) - selectionStart)
			}
		}
	}

	fun eraseCharacters(characterOffset: Int) {
		if (text.isNotEmpty()) {
			if (selectionEnd != selectionStart) {
				write("")
			} else {
				val i = getCursorPosWithOffset(characterOffset)
				val j = min(i, selectionStart)
				val k = max(i, selectionStart)
				if (j != k) {
					val string = StringBuilder(text).delete(j, k).toString()
					if (textPredicate.test(string)) {
						text = string
						cursor = j
					}
				}
			}
		}
	}

	fun getWordSkipPosition(wordOffset: Int): Int {
		return this.getWordSkipPosition(wordOffset, cursor)
	}

	private fun getWordSkipPosition(wordOffset: Int, cursorPosition: Int): Int {
		return this.getWordSkipPosition(wordOffset, cursorPosition, true)
	}

	private fun getWordSkipPosition(wordOffset: Int, cursorPosition: Int, skipOverSpaces: Boolean): Int {
		var i = cursorPosition
		val bl = wordOffset < 0
		val j = abs(wordOffset)
		for (k in 0 until j) {
			if (!bl) {
				val l = text.length
				i = text.indexOf(32.toChar(), i)
				if (i == -1) {
					i = l
				} else {
					while (skipOverSpaces && i < l && text[i] == ' ') {
						++i
					}
				}
			} else {
				while (skipOverSpaces && i > 0 && text[i - 1] == ' ') {
					--i
				}
				while (i > 0 && text[i - 1] != ' ') {
					--i
				}
			}
		}
		return i
	}

	fun moveCursor(offset: Int) {
		cursor = getCursorPosWithOffset(offset)
	}

	private fun getCursorPosWithOffset(offset: Int): Int {
		return Util.moveCursor(text, selectionStart, offset)
	}


	fun setCursorToStart() {
		cursor = 0
	}

	fun setCursorToEnd() {
		cursor = text.length
	}

	override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
		return if (!isActive) {
			false
		} else {
			selecting = hasShiftDown()
			if (isSelectAll(keyCode)) {
				setCursorToEnd()
				selectionEnd = 0
				true
			} else if (Screen.isCopy(keyCode)) {
				MinecraftClient.getInstance().keyboard.clipboard = selectedText
				true
			} else if (Screen.isPaste(keyCode)) {
				if (isEditable) {
					write(MinecraftClient.getInstance().keyboard.clipboard)
				}
				true
			} else if (Screen.isCut(keyCode)) {
				MinecraftClient.getInstance().keyboard.clipboard = selectedText
				if (isEditable) {
					write("")
				}
				true
			} else {
				when (keyCode) {
					259  -> if (isEditable) {
						selecting = false
						erase(-1)
						selecting = hasShiftDown()
					}

					261  -> if (isEditable) {
						selecting = false
						erase(1)
						selecting = hasShiftDown()
					}

					262  -> if (Screen.hasControlDown()) {
						cursor = this.getWordSkipPosition(1)
					} else {
						moveCursor(1)
					}

					263  -> if (Screen.hasControlDown()) {
						cursor = this.getWordSkipPosition(-1)
					} else {
						moveCursor(-1)
					}

					268  -> setCursorToStart()

					269  -> setCursorToEnd()

					else -> return false
				}
				return true
			}
		}
	}

	val isActive: Boolean
		get() = isVisible && this.isFocused && isEditable

	override fun charTyped(chr: Char, modifiers: Int): Boolean {
		return if (!isActive) {
			false
		} else if (isValid(chr)) {
			if (isEditable) {
				write(chr.toString())
			}
			true
		} else {
			false
		}
	}

	override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
		return if (!isVisible) {
			false
		} else {
			val bl =
				mouseX >= this.x.toDouble() && mouseX < (this.x + width).toDouble() && mouseY >= y.toDouble() && mouseY < (y + height).toDouble()
			if (focusUnlocked) {
				isFocused = bl
			}
			if (this.isFocused && bl && button == 0) {
				var i = MathHelper.floor(mouseX) - this.x
				if (drawsBackground) {
					i -= 4
				}
				val string = textRenderer!!.trimToWidth(text.substring(firstCharacterIndex), innerWidth)
				cursor = textRenderer.trimToWidth(string, i).length + firstCharacterIndex
				true
			} else {
				false
			}
		}
	}

	override fun renderButton(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
		if (isVisible) {
			if (drawsBackground()) {
				val borderColor = if (this.isFocused) BORDER_COLOR_FOCUSED else BORDER_COLOR
				fill(matrices, this.x - 1, y - 1, this.x + width + 1, y + height + 1, borderColor)
				fill(matrices, this.x, y, this.x + width, y + height, BACKGROUND_COLOR)
			}
			val textColor = if (isEditable) editableColor else uneditableColor
			val j = selectionStart - firstCharacterIndex
			var k = selectionEnd - firstCharacterIndex
			val string = textRenderer!!.trimToWidth(text.substring(firstCharacterIndex), innerWidth)
			val bl = j >= 0 && j <= string.length
			val bl2 = this.isFocused && focusedTicks / 6 % 2 == 0 && bl
			val xWithOffset = if (drawsBackground) this.x + 4 else this.x
			val yWithOffset = if (drawsBackground) y + (height - 8) / 2 else y
			var n = xWithOffset
			if (k > string.length) {
				k = string.length
			}
			if (string.isNotEmpty()) {
				val string2 = if (bl) string.substring(0, j) else string
				n = textRenderer.drawWithShadow(
					matrices,
					renderTextProvider.apply(string2, firstCharacterIndex),
					xWithOffset.toFloat(),
					yWithOffset.toFloat(),
					textColor
				)
			}
			val bl3 = selectionStart < text.length || text.length >= maxLength
			var o = n
			if (!bl) {
				o = if (j > 0) xWithOffset + width else xWithOffset
			} else if (bl3) {
				o = n - 1
				--n
			}
			if (string.isNotEmpty() && bl && j < string.length) {
				textRenderer.drawWithShadow(
					matrices,
					renderTextProvider.apply(string.substring(j), selectionStart),
					n.toFloat(),
					yWithOffset.toFloat(),
					textColor
				)
			}
			if (!bl3 && suggestion != null) {
				textRenderer.drawWithShadow(matrices, suggestion, (o - 1).toFloat(), yWithOffset.toFloat(), -8355712)
			}
			if (bl2) {
				if (bl3) {
					fill(matrices, o, yWithOffset - 1, o + 1, yWithOffset + 1 + 9, -3092272)
				} else {
					textRenderer.drawWithShadow(matrices, "_", o.toFloat(), yWithOffset.toFloat(), textColor)
				}
			}
			if (k != j) {
				val p = xWithOffset + textRenderer.getWidth(string.substring(0, k))
				drawSelectionHighlight(o, yWithOffset - 1, p - 1, yWithOffset + 1 + 9)
			}
		}
	}

	private fun drawSelectionHighlight(x1: Int, y1: Int, x2: Int, y2: Int) {
		var (x1, x2) = if (x1 > x2) x2 to x1 else x1 to x2
		val (y1, y2) = if (y1 > y2) y2 to y1 else y1 to y2
		if (x2 > this.x + width) {
			x2 = this.x + width
		}
		if (x1 > this.x + width) {
			x1 = this.x + width
		}
		val tessellator = Tessellator.getInstance()
		val bufferBuilder = tessellator.buffer
		RenderSystem.setShader { GameRenderer.getPositionShader() }
		RenderSystem.setShaderColor(0.0f, 0.0f, 1.0f, 1.0f)
		RenderSystem.disableTexture()
		RenderSystem.enableColorLogicOp()
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE)
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
		bufferBuilder.vertex(x1.toDouble(), y2.toDouble(), 0.0).next()
		bufferBuilder.vertex(x2.toDouble(), y2.toDouble(), 0.0).next()
		bufferBuilder.vertex(x2.toDouble(), y1.toDouble(), 0.0).next()
		bufferBuilder.vertex(x1.toDouble(), y1.toDouble(), 0.0).next()
		tessellator.draw()
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
		RenderSystem.disableColorLogicOp()
		RenderSystem.enableTexture()
	}



	var cursor: Int
		get() = selectionStart
		set(cursor) {
			selectionStart = cursor
			if (!selecting) {
				selectionEnd = selectionStart
			}
			onChanged(text)
		}

	private fun drawsBackground(): Boolean {
		return drawsBackground
	}

	override fun changeFocus(lookForwards: Boolean): Boolean {
		return visible && isEditable && super<ClickableWidget>.changeFocus(lookForwards)
	}

	override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
		return visible && mouseX >= this.x.toDouble() && mouseX < (this.x + width).toDouble() && mouseY >= y.toDouble() && mouseY < (y + height).toDouble()
	}

	override fun onFocusedChanged(newFocused: Boolean) {
		if (newFocused) {
			focusedTicks = 0
		}
	}

	val innerWidth: Int
		get() = if (drawsBackground()) width - 8 else width

	var isVisible: Boolean
		get() = visible
		set(visible) {
			this.visible = visible
		}


	fun getCharacterX(index: Int): Int {
		return if (index > text.length) this.x else this.x + textRenderer!!.getWidth(text.substring(0, index))
	}

	var x: Int
		get() = super.x
		set(x) {
			this.x = x
		}

	override fun appendNarrations(builder: NarrationMessageBuilder) {
		builder.put(NarrationPart.TITLE, TranslatableText("narration.edit_box", text))
	}

	companion object {
		const val BACKWARDS = -1
		const val FORWARDS = 1
		private const val INSERT_CURSOR_WIDTH = 1
		private const val INSERT_CURSOR_COLOR = -3092272
		private const val UNDERSCORE = "_"
		const val DEFAULT_EDITABLE_COLOR = 14737632
		private const val BORDER_COLOR_FOCUSED = -1
		private const val BORDER_COLOR = -6250336
		private const val BACKGROUND_COLOR = -16777216
	}
}