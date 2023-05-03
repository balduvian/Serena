package org.balduvian

object Color {
	private fun parseColorPart(colorPart: String): Int? {
		return when (colorPart.length) {
			6 -> colorPart.toIntOrNull(16)
			3 -> {
				val baseColor = colorPart.toIntOrNull(16) ?: return null

				val red = baseColor.ushr(8) * 17
				val gre = baseColor.ushr(4).and(7) * 17
				val blu = baseColor.and(7) * 17

				red.shl(16).or(gre.shl(8)).or(blu)
			}
			else -> null
		}
	}

	fun parseColor(input: String): Int? {
		val trimmed = input.trim()

		return if (trimmed.startsWith("#")) {
			parseColorPart(trimmed.substring(1))
		} else if (trimmed.startsWith("0x")) {
			parseColorPart(trimmed.substring(2))
		} else {
			parseColorPart(trimmed)
		}
	}

	fun name(color: Int): String {
		return "#${String.format("%06X", color)}"
	}
}

fun main() {
	println(Color.name(0x20c))
}
