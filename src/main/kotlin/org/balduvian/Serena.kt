package org.balduvian

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.balduvian.connections.Connections
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO
import javax.security.auth.login.LoginException

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
		Connections.handleMessage(event.message)
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
				GatewayIntent.MESSAGE_CONTENT,
			).setMemberCachePolicy(MemberCachePolicy.ALL)
	            .enableCache(
		            CacheFlag.ACTIVITY,
		            CacheFlag.EMOJI,
		            CacheFlag.ONLINE_STATUS,
		            CacheFlag.ROLE_TAGS,
					CacheFlag.CLIENT_STATUS,
					CacheFlag.MEMBER_OVERRIDES,
					CacheFlag.STICKER,
				).disableCache(CacheFlag.VOICE_STATE, CacheFlag.SCHEDULED_EVENTS)

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
