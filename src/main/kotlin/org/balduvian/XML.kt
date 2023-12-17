package org.balduvian

import java.io.File
import java.io.InputStream
import java.lang.Error

object XML {
	fun parse(stream: InputStream): Node {
		val bytes = stream.readAllBytes()

		val tokens = tokenize(bytes)

		return lex(bytes, tokens)
	}

	fun parse(string: String): Node {
		val bytes = string.toByteArray()

		val tokens = tokenize(bytes)

		return lex(bytes, tokens)
	}

	private enum class TokenType {
		OPEN,
		CLOSE,
		IDENTIFIER,
		EQUALS,
		SINGLE_QUOTE,
		FULL_QUOTE,
		FULL_CLOSE,
		FULL_OPEN,
		OTHER,
		ESCAPE,
		WHITESPACE,
		QUEST_OPEN,
		QUEST_CLOSE,
		CDATA_BEGIN,
		CDATA_END,
	}

	sealed class Node {
		abstract fun firstChild(): Node?
		abstract fun firstChildOfTag(tagName: String): Node?
		abstract fun childrenOfTag(tagName: String): ArrayList<Node>
		abstract fun contents(): String
	}
	class TagNode (val tag: String, val attributes: HashMap<String, String>, val children: ArrayList<Node>) : Node() {
		override fun firstChild() = children.firstOrNull()
		override fun firstChildOfTag(tagName: String) = children.firstOrNull { node -> node is TagNode && node.tag == tagName }
		override fun childrenOfTag(tagName: String) = children.filter { node -> node is TagNode && node.tag == tagName } as ArrayList<Node>
		override fun contents(): String = firstChild()?.contents() ?: ""
	}
	class StringNode(val string: String): Node() {
		override fun firstChild() = null
		override fun firstChildOfTag(tagName: String) = null
		override fun childrenOfTag(tagName: String) = ArrayList<Node>()
		override fun contents() = string
	}

	private class TokenStream(val tokens: ArrayList<Token>) {
		var index = 0

		fun next(type: TokenType? = null, skipWhitespace: Boolean =  true): Token {
			if (index >= tokens.size) throw Error("unexpected end of tokens")
			var token = tokens[index++]
			if (skipWhitespace && token.type == TokenType.WHITESPACE) {
				if (index >= tokens.size) throw Error("unexpected end of tokens")
				token = tokens[index++]
			}
			if (type != null && token.type !== type) throw Error("expected token of type \"$type\", got \"${token.type}\" instead")
			return token
		}

		fun hasNext(): Boolean {
			return index < tokens.size
		}

		fun rewind() {
			--index
		}
	}

	private fun lexAttribute(bytes: ByteArray, tokenStream: TokenStream): String {
		tokenStream.next(type=TokenType.EQUALS)
		val quoteType = tokenStream.next().type
		if (quoteType !== TokenType.SINGLE_QUOTE && quoteType !== TokenType.FULL_QUOTE) throw Error("expected quote")

		val builder = StringBuilder()

		while (true) {
			val token = tokenStream.next()

			if (token.type == quoteType) {
				return builder.toString()
			} else {
				builder.append(token.extractString(bytes))
			}
		}
	}

	private fun lexTag(bytes: ByteArray, tokenStream: TokenStream, isQuestionMark: Boolean): Pair<TagNode, Boolean> {
		val name = tokenStream.next(type=TokenType.IDENTIFIER).extractString(bytes)
		val attributes = HashMap<String, String>()

		while (true) {
			val token = tokenStream.next()

			when (token.type) {
				TokenType.CLOSE -> {
					if (isQuestionMark) throw Error("unexpected closing tag")
					return Pair(TagNode(name, attributes, ArrayList()), true)
				}
				TokenType.FULL_CLOSE -> {
					if (isQuestionMark) throw Error("unexpected closing tag")
					return Pair(TagNode(name, attributes, ArrayList()), false)
				}
				TokenType.QUEST_CLOSE -> {
					if (!isQuestionMark) throw Error("unexpected question closing tag")
					return Pair(TagNode(name, attributes, ArrayList()), false)
				}
				TokenType.IDENTIFIER -> {
					val attributeName = token.extractString(bytes)
					val attributeValue = lexAttribute(bytes, tokenStream)

					attributes[attributeName] = attributeValue
				}
				else -> {
					throw Error("unexpected token")
				}
			}
		}
	}

	private fun lexClosingTag(bytes: ByteArray, tokenStream: TokenStream): String {
		val name = tokenStream.next(type=TokenType.IDENTIFIER).extractString(bytes)
		tokenStream.next(TokenType.CLOSE)
		return name
	}

	private fun lexCdata(bytes: ByteArray, tokenStream: TokenStream): StringNode {
		val builder = StringBuilder()

		while (true) {
			val token = tokenStream.next(skipWhitespace = false)

			if (token.type === TokenType.CDATA_END) {
				return StringNode(builder.toString())
			} else {
				builder.append(token.extractLiteralString(bytes))
			}
		}
	}

	private fun lexContents(bytes: ByteArray, tokenStream: TokenStream): StringNode? {
		val builder = StringBuilder()
		var index = 0

		while (true) {
			val token = tokenStream.next(skipWhitespace = false)

			if (token.type === TokenType.OPEN || token.type === TokenType.FULL_OPEN) {
				tokenStream.rewind()

				if (builder.isEmpty()) return null
				return StringNode(builder.toString())
			} else {
				if (index > 0 || token.type !== TokenType.WHITESPACE) {
					builder.append(token.extractString(bytes))
				}
			}

			++index
		}
	}


	private fun lex(bytes: ByteArray, tokens: ArrayList<Token>): Node {
		val tokenStream = TokenStream(tokens)

		val root = TagNode("root", HashMap(), ArrayList())
		val treeStack = arrayListOf(root)

		while (tokenStream.hasNext()) {
			val token = tokenStream.next()

			if (token.type === TokenType.OPEN || token.type === TokenType.QUEST_OPEN) {
				val (tag, hasContents) = lexTag(bytes, tokenStream, token.type == TokenType.QUEST_OPEN)

				val parent = treeStack.last()
				/* no nested trs */
				if (parent.tag == "tr" && tag.tag == "tr") continue
				parent.children.add(tag)

				if (hasContents) treeStack.add(tag)

			} else if (token.type === TokenType.FULL_OPEN) {
				val tagName = lexClosingTag(bytes, tokenStream)

				val topTag = treeStack.last().tag
				/* ignore erroneous closing tags */
				if (tagName != topTag) continue // throw Error("closing tag \"$tagName\", expected \"$topTag\"")

				treeStack.removeLast()
				if (treeStack.isEmpty()) throw Error("trying to close beyond root")

			} else if (token.type === TokenType.CDATA_BEGIN){
				val contents = lexCdata(bytes, tokenStream)

				val parent = treeStack.last()
				parent.children.add(contents)

			} else {
				tokenStream.rewind()
				val contents = lexContents(bytes, tokenStream) ?: continue

				val parent = treeStack.last()
				parent.children.add(contents)
			}
		}

		return root
	}

	private fun unescape(escapeSequence: String): Char {
		val midSection = escapeSequence.subSequence(1, escapeSequence.length - 1)

		return when {
			midSection == "quot" -> '"'
			midSection == "apos" -> '\''
			midSection == "lt" -> '<'
			midSection == "gt" -> '>'
			midSection == "amp" -> '&'
			midSection.startsWith('#') -> Char(midSection.substring(1).toInt())
			else -> throw Error("invalid escape seqence $midSection")
		}
	}

	private data class Token(val type: TokenType, val start: Int, val end: Int) {
		fun extractString(bytes: ByteArray): String {
			val str = extractLiteralString(bytes)
			if (type == TokenType.ESCAPE) return "${unescape(str)}"
			return str
		}

		fun extractLiteralString(bytes: ByteArray): String {
			return bytes.sliceArray(start until end).decodeToString()
		}
	}

	fun unescapeString(input: String): String {
		return input.replace(Regex("&(.+?);")) { matchResult ->
			"${unescape(matchResult.value)}"
		}
	}

	private fun isInitialIdentifierChar(current: Int): Boolean {
		return (current >= 'A'.code && current <= 'Z'.code) || (current >= 'a'.code && current <= 'z'.code)
	}

	private fun isIdentifierChar(current: Int): Boolean {
		return isInitialIdentifierChar(current) || (current >= '0'.code && current <= '9'.code) || current == ':'.code
	}

	private fun tokenize(bytes: ByteArray): ArrayList<Token> {
		val tokens = ArrayList<Token>()

		var withinTokenType: TokenType? = null
		var startIndex = 0
		var index = 0

		fun createToken(type: TokenType, includesCurrent: Boolean) {
			if (index > startIndex && withinTokenType == null) {
				tokens.add(Token(TokenType.OTHER, startIndex, index))
				startIndex = index
			}
			tokens.add(Token(type, startIndex, if (includesCurrent) index + 1 else index))
			withinTokenType = null
			startIndex = if (includesCurrent) index + 1 else index
			if (!includesCurrent) --index
		}

		fun enterToken(type: TokenType) {
			if (index > startIndex && withinTokenType == null) {
				tokens.add(Token(TokenType.OTHER, startIndex, index))
			}
			withinTokenType = type
			startIndex = index
		}

		fun reinterpret() {
			withinTokenType = null
			--index
		}

		while (index < bytes.size) {
			val current = bytes[index].toInt()
			val subIndex = index - startIndex

			if (withinTokenType == TokenType.OPEN) {
				if (subIndex == 1) {
					if (current == '!'.code) {
						/* continue */
					} else if (current == '/'.code) {
						createToken(TokenType.FULL_OPEN, true)
					} else if (current == '?'.code) {
						createToken(TokenType.QUEST_OPEN, true)
					} else {
						createToken(TokenType.OPEN, false)
					}
				} else {
					if (current == "<![CDATA["[subIndex].code) {
						if (subIndex == "<![CDATA[".lastIndex) {
							createToken(TokenType.CDATA_BEGIN, true)
						}
					} else {
						reinterpret()
					}
				}
			} else if (withinTokenType == TokenType.CLOSE) {
				if (current == '>'.code) {
					createToken(TokenType.CLOSE, true)
				} else {
					reinterpret()
				}
			} else if (withinTokenType == TokenType.FULL_CLOSE) {
				if (current == '>'.code) {
					createToken(TokenType.FULL_CLOSE, true)
				} else {
					reinterpret()
				}
			} else if (withinTokenType == TokenType.QUEST_CLOSE) {
				if (current == '>'.code) {
					createToken(TokenType.QUEST_CLOSE, true)
				} else {
					reinterpret()
				}
			} else if (withinTokenType == TokenType.ESCAPE) {
				if (current == ';'.code) {
					createToken(TokenType.ESCAPE, true)
				}
			} else if (withinTokenType == TokenType.CDATA_END) {
				if (current == "]]>"[subIndex].code) {
					if (subIndex == "]]>".lastIndex) {
						createToken(TokenType.CDATA_END, true)
					}
				} else {
					reinterpret()
				}
			} else if (withinTokenType == TokenType.IDENTIFIER) {
				if (!isIdentifierChar(current)) {
					createToken(TokenType.IDENTIFIER, false)
				}
			} else if (withinTokenType == TokenType.WHITESPACE) {
				if (!current.toChar().isWhitespace()) {
					createToken(TokenType.WHITESPACE, false)
				}
			} else {
				if (current == '<'.code) {
					enterToken(TokenType.OPEN)
				} else if (current == '>'.code) {
					createToken(TokenType.CLOSE, true)
				} else if (current == '?'.code) {
					enterToken(TokenType.QUEST_CLOSE)
				} else if (current == '/'.code) {
					enterToken(TokenType.FULL_CLOSE)
				} else if (current == '='.code) {
					createToken(TokenType.EQUALS, true)
				} else if (current == '"'.code) {
					createToken(TokenType.FULL_QUOTE, true)
				} else if (current == '\''.code) {
					createToken(TokenType.SINGLE_QUOTE, true)
				} else if (current == '&'.code) {
					enterToken(TokenType.ESCAPE)
				} else if (current == ']'.code) {
					enterToken(TokenType.CDATA_END)
				} else if (isInitialIdentifierChar(current)) {
					enterToken(TokenType.IDENTIFIER)
				} else if (current.toChar().isWhitespace()) {
					enterToken(TokenType.WHITESPACE)
				}
			}

			++index
		}

		return tokens
	}
}

fun main() {
	val root = XML.parse(File("development/test.xml").inputStream())

	val items = root.firstChildOfTag("rss")?.firstChildOfTag("channel")?.childrenOfTag("item") ?: return

	val oahuHtml = items.find { item ->
		item.firstChildOfTag("title")?.contents() == "Forecast for Oahu"
	}?.firstChildOfTag("description")?.contents()

	val oahuDocument = oahuHtml?.let(XML::parse)

	oahuDocument?.firstChildOfTag("table")
}