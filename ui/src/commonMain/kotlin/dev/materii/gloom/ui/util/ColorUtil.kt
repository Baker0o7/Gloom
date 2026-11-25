package dev.materii.gloom.ui.util

import androidx.compose.ui.graphics.Color

private val hexCodeRegex = "^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$".toRegex()
private val parsedColors = mutableMapOf<String, Color>()

/** Parses `#rgb`, `#rrggbb` (with or without the `#`); anything else falls back to black. */
val String.parsedColor: Color
    get() {
        parsedColors[this]?.let { return it }
        val hex = hexCodeRegex.matchEntire(this.trim())?.groupValues?.get(1) ?: return Color.Black
        val full = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        val color = runCatching {
            Color(
                full.substring(0, 2).toInt(16),
                full.substring(2, 4).toInt(16),
                full.substring(4, 6).toInt(16)
            )
        }.getOrDefault(Color.Black)
        parsedColors[this] = color
        return color
    }

val Color.hexCode: String
    inline get() {
        val a: Int = (alpha * 255).toInt()
        val r: Int = (red * 255).toInt()
        val g: Int = (green * 255).toInt()
        val b: Int = (blue * 255).toInt()
        return "%02X%02X%02X%02X".format(r, g, b, a)
    }