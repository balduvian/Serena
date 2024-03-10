package org.balduvian.connections

import net.dv8tion.jda.api.entities.Message

data class Game(val puzzleNumber: Int) {
	companion object {
		private const val YELLOW = "🟨"
		private const val GREEN = "🟩"
		private const val BLUE = "🟦"
		private const val PURPLE = "🟪"

		private fun findConnectionsLines(lines: List<String>, startIndex: Int): Int? {
			for (i in startIndex until lines.size) {
				val line = lines[i].trimEnd()
				if (line == "Connections") return i
			}

			return null
		}

		private fun getPuzzleNumber(line: String): Int? {
			val parts = line.trimEnd().split(' ')
			if (parts.size != 2) return null
			if (parts[0] != "Puzzle") return null
			if (!parts[1].startsWith('#')) return null
			return parts[1].substring(1).toIntOrNull()
		}

		private fun isSquareLine(line: String): Boolean {
			val trimmed = line.trimEnd()
			if (trimmed.length != 8) return false

			for (i in 0 until 4) {
				val sub = trimmed.substring(i * 2, i * 2 + 2)
				if (sub != YELLOW && sub != GREEN && sub != BLUE && sub != PURPLE) return false
			}

			return true
		}

		private data class ParseResult(val index: Int, val puzzleNumber: Int?)

		private fun parseAtIndex(lines: List<String>, startIndex: Int): ParseResult {
			var index = findConnectionsLines(lines, startIndex) ?: return ParseResult(lines.size, null)

			if (++index >= lines.size) return ParseResult(lines.size, null)
			val puzzleNumber = getPuzzleNumber(lines[index]) ?: return ParseResult(index, null)

			var numSquareLines = 0

			while (++index < lines.size) {
				if (isSquareLine(lines[index])) ++numSquareLines else break
			}

			return ParseResult(index, if (numSquareLines >= 4) puzzleNumber else null)
		}

		fun parse(message: Message): Game? {
			val text = message.contentRaw

			val lines = text.lines()

			var index = 0

			while (index < lines.size) {
				val (newIndex, puzzleNumber) = parseAtIndex(lines, index)

				if (puzzleNumber != null) return Game(puzzleNumber)
				else index = newIndex
			}

			return null
		}
	}
}
