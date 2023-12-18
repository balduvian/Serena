package org.balduvian

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.imageio.ImageIO
import javax.security.auth.login.LoginException
import kotlin.random.Random

class Serena(val jda: JDA) : ListenerAdapter() {
	val ACCEPT = "✅"
	val REJECT = "❌"
	val STOP = "\uD83D\uDED1"

	var messageCache = ArrayList<Message>()

    override fun onReady(event: ReadyEvent) {
    	println("Serena up and running")

	   //TimingManager.start()

	    jda.updateCommands().addCommands(
			Commands.slash("rolecolor", "Changes your role color")
				.addOption(OptionType.STRING, "color", "New Role Color", true)
		).queue()
    }

	val colorHelp = """
		Malformed color string
		
		Try using the form `#000000` where each digit can be one of:
		> **0 1 2 3 4 5 6 7 8 9 A B C D E F**
		
		Additionally you can just use 6 hex digits, or use `0x` as the prefix
	""".trimIndent()

	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		fun errorMessage(message: String) {
			event.reply(message).setEphemeral(true).queue()
		}

		if (event.name == "rolecolor") {
			val colorString = event.getOption("color")?.asString ?: return errorMessage("Please include color")
			val colorInt = Color.parseColor(colorString) ?: return errorMessage(colorHelp)
			val member = event.member ?: return errorMessage("Who are you?")

			val colorRole = member.roles.firstOrNull() ?: return errorMessage("You have no roles")

			event.deferReply().queue { hook ->
				colorRole.manager.setColor(colorInt).queue({
					val image = ImageIO.read(URL(member.effectiveAvatarUrl))
					val byteArrayOutputStream = ByteArrayOutputStream()
					ImageIO.write(image, "PNG", byteArrayOutputStream)

					val filename = "pfp.png"

					hook.editOriginalEmbeds(
						EmbedBuilder().setThumbnail("attachment://${filename}").setDescription(
							"Set ${Util.mention(colorRole)}'s color to **${Color.name(colorInt)}**"
						)
							.setColor(colorInt)
							.build()
					)
						.setFiles(FileUpload.fromData(byteArrayOutputStream.toByteArray(), filename))
						.queue()
				}, { throwable ->
					hook.editOriginal(throwable.localizedMessage).queue()
				})
			}
		}
	}

	override fun onMessageReceived(event: MessageReceivedEvent) {
		val member = event.member ?: return

		if (!member.hasPermission(Permission.ADMINISTRATOR)) return

		val message = event.message.contentRaw

		if (message == "&&cached") {
			println("Received cached command from ${member.effectiveName}")
			TimingManager.addEvent(TimingManager.ScheduledEvent.interruptEvent(true))

		} else if (message == "&&force") {
			println("Received force command from ${member.effectiveName}")
			TimingManager.addEvent(TimingManager.ScheduledEvent.interruptEvent(false))
		}
	}

	override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
		val member = event.member ?: return
		if (member.user.isBot) return

		if (!member.hasPermission(Permission.ADMINISTRATOR)) return

		val reactMessage = event.channel.retrieveMessageById(event.messageId).complete()

		/* has to be sent by the bot */
		if (reactMessage.author.idLong != jda.selfUser.idLong) return

		/* must have an embed */
		if (reactMessage.embeds.isEmpty()) return

		fun removeReactions() = reactMessage.clearReactions().queue()

		when (event.emoji.name) {
			ACCEPT -> {
				println("Received accept reaction command from ${member.effectiveName}")

				val sendChannel = getChannel(BotMain.data.sendChannelID) ?: return println("Could not find send channel")

				removeReactions()

				sendChannel.sendMessageEmbeds(reactMessage.embeds[0]).queue()

				println("Sent final message")
			}
			REJECT -> {
				println("Received reject reaction command from ${member.effectiveName}")

				/* find another message */
				TimingManager.addEvent(TimingManager.ScheduledEvent.interruptEvent(true))

				removeReactions()
			}
			STOP -> {
				println("Received stop reaction command from ${member.effectiveName}")

				removeReactions()
			}
		}
	}

	fun getChannel(id: Long): TextChannel? {
		return jda.getTextChannelById(id)
	}

    fun grabRandomMessage(days: Int, channel: TextChannel, useCache: Boolean): ArrayList<Message> {
	    /* collect messages again */
	    if (!useCache || messageCache.isEmpty()) {
		    val now = LocalDateTime.now()

		    try {
			    val history = channel.history
			    var endIndex: Int

			    while (true) {
				    val startIndex = history.retrievedHistory.size
				    val retrieved = history.retrievePast(100).complete()
				    endIndex = history.retrievedHistory.size

				    println("Retrieved ${retrieved.size} messages")

				    /* crop the list to only include messages within the last 30 days */
				    for (i in startIndex until endIndex) {
					    val message = history.retrievedHistory[i]

					    val daysOld = ChronoUnit.DAYS.between(message.timeCreated.toLocalDateTime(), now)

					    if (daysOld > days) {
						    endIndex = i
						    break
					    }
				    }

				    /* if there are no more messages to get */
				    if (endIndex < history.retrievedHistory.size || retrieved.isEmpty()) break
			    }

			    println("last message time: ${history.retrievedHistory.lastOrNull()?.timeCreated} | end index: $endIndex")

			    /* cache history */
			    messageCache = ArrayList(history.retrievedHistory.take(endIndex))

		    } catch (ex: Exception) {
			    ex.printStackTrace()
			    return ArrayList()
		    }
	    }

	    println("cache size: ${messageCache.size}")

	    return findMessages(messageCache, 9)
    }

	/**
	 * @return a list of contiguous messages to combine and send
	 * or an empty list if no suitable chain of messages could be found
	 */
	fun findMessages(messages: ArrayList<Message>, minWordCount: Int): ArrayList<Message> {
		if (messages.isEmpty()) return ArrayList()

		val random = Random(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))

		fun hasImage(message: Message) = message.attachments.isNotEmpty() && message.attachments[0].isImage
		fun isBy(message: Message, id: Long) = message.author.idLong == id
		fun wordCount(message: Message) = message.contentRaw.split(' ', '\n', '-').size

		fun find(collection: ArrayList<Message>, direction: Boolean, index: Int, onMessage: (Message) -> Boolean): Int {
			if (direction) { /* earlier */
				if (index < messages.lastIndex) {
					val message = messages[index + 1]

					if (onMessage(message)) {
						collection.add(0, message)

						return index + 1
					}
				}
			} else { /* later */
				if (index > 0) {
					val message = messages[index - 1]

					if (onMessage(message)) {
						collection.add(message)

						return index - 1
					}
				}
			}

			return -1
		}

		fun keepFinding(
			collection: ArrayList<Message>,
		    direction: Boolean,
			startIndex: Int,
			onMessage: (Message) -> Boolean,
			satisfy: (ArrayList<Message>) -> Boolean
		): ArrayList<Message> {
			var index = startIndex

			while (true) {
				index = find(collection, direction, index, onMessage)

				if (index == -1) {
					return ArrayList()

				} else if (satisfy(collection)) {
					return collection
				}
			}
		}

		fun findEither(startMessage: Message, index: Int, id: Long, hasImage: Boolean): ArrayList<Message> {
			val direction = random.nextBoolean()

			fun accept(message: Message): Boolean = hasImage == hasImage(message) && isBy(message, id)

			val collection = arrayListOf(startMessage)

			find(collection, direction, index, ::accept) != -1 || find(collection, !direction, index, ::accept) != -1

			return collection
		}

		fun findExtend(startMessage: Message, index: Int, id: Long): ArrayList<Message> {
			val direction = random.nextBoolean()

			fun accept(message: Message) = isBy(message, id)

			fun satisfy(list: ArrayList<Message>) = list.fold(0) { acc, message ->
				if (hasImage(message)) return true
				acc + wordCount(message)
			} >= minWordCount

			val collection0 = keepFinding(arrayListOf(startMessage), direction, index, ::accept, ::satisfy)
			if (collection0.isNotEmpty()) return collection0

			return keepFinding(arrayListOf(startMessage), !direction, index, ::accept, ::satisfy)
		}

		for (attempt in 0 until 32) {
			val startIndex = random.nextInt(0, messages.size)

			val startMessage = messages[startIndex]
			val userID = startMessage.author.idLong

			if (hasImage(startMessage)) {
				/* images always succeed */
				return findEither(startMessage, startIndex, userID, false)

			} else if (wordCount(startMessage) >= minWordCount) {
				/* lengthy messages always succeed */
				return findEither(startMessage, startIndex, userID, true)

			} else {
				/* non-images, non-length requirements must extend */
				val collection = findExtend(startMessage, startIndex, userID)
				if (collection.isNotEmpty()) return collection
			}
		}

		return ArrayList()
	}

	fun addCommandReactions(message: Message) {
		message.addReaction(Emoji.fromUnicode(ACCEPT)).queue()
		message.addReaction(Emoji.fromUnicode(REJECT)).queue()
		message.addReaction(Emoji.fromUnicode(STOP)).queue()
	}

	fun createEmbed(collection: ArrayList<Message>): MessageEmbed {
		val DEFAULT_IMAGE = "https://cdn.discordapp.com/embed/avatars/0.png"

		val oldestMessage = collection.first()

		val builder = EmbedBuilder()

		/* who sent the message */
		val author = try {
			oldestMessage.guild.retrieveMemberById(oldestMessage.author.id).complete()
		} catch (ex: Exception) {
			null
		}

		if (author != null) {
			builder.setAuthor(author.effectiveName, null, author.user.effectiveAvatarUrl)
			builder.setThumbnail(author.user.effectiveAvatarUrl)
			builder.setColor(author.colorRaw)

		} else {
			builder.setAuthor("UNKNOWN", null, DEFAULT_IMAGE)
			builder.setThumbnail(DEFAULT_IMAGE)
			builder.setColor(0x6064f4)
		}

		/* combine all messages text into one block, separated by newlines */
		builder.setDescription(collection.joinToString("\n", "", "", -1, "") { it.contentRaw })

		/* send the one image among the collection */
		val image = collection.find { message ->
			message.attachments.isNotEmpty() && message.attachments[0].isImage
		}?.attachments?.get(0)

		if (image != null) builder.setImage(image.url)

		/* guild information in the footer */
		if (oldestMessage.isFromGuild) {
			val guild = collection.first().guild
			builder.setFooter(guild.name, guild.iconUrl ?: DEFAULT_IMAGE)

		} else {
			builder.setFooter("UNKNOWN", DEFAULT_IMAGE)
		}

		/* sent time of the first message as timestamp */
		builder.setTimestamp(oldestMessage.timeCreated)

		return builder.build()
	}

	fun sendStageMessage(collection: ArrayList<Message>, channel: TextChannel): Message {
		val embed = createEmbed(collection)

		val message = channel.sendMessageEmbeds(embed).complete()

		addCommandReactions(message)

		return message
	}

    companion object {
        fun createBot(token: String): Serena? {
            val builder = JDABuilder.create(token,
	            GatewayIntent.GUILD_MEMBERS,
	            GatewayIntent.GUILD_MESSAGES,
	            GatewayIntent.GUILD_PRESENCES,
	            GatewayIntent.GUILD_MESSAGE_REACTIONS,
	            GatewayIntent.GUILD_MESSAGE_TYPING,
	            GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
	            GatewayIntent.GUILD_VOICE_STATES,
			).setMemberCachePolicy(MemberCachePolicy.ALL)
	            .enableCache(CacheFlag.values().asList())

            return try {
                val jda = builder.build()

                val bot = Serena(jda)
                jda.addEventListener(bot)

                bot

            } catch (ex: LoginException) {
                ex.printStackTrace()

                null
            }
        }
    }
}
