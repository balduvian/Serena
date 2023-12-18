package org.balduvian

import BotMain
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import java.time.Duration
import kotlin.system.exitProcess

data class WindowEventProps(val useCache: Boolean)

class WindowEvent : DailyEvent<WindowEventProps>(12) {
	val MAX_TRIES: Int = 4
	val DAYS_BACK = 30

	private fun getChannel(id: Long): TextChannel {
		val channel = BotMain.bot.getChannel(id)

		if (channel == null) {
			println("Channel of ID $id does not exist")
			exitProcess(-1)
		}

		return channel
	}

	override fun createDefaultProps(): WindowEventProps {
		return WindowEventProps(false)
	}

	override fun onEvent(currentTry: Int, props: WindowEventProps): EventResult {
		val grabChannel = getChannel(BotMain.data.grabChannelID)
		val stageChannel = getChannel(BotMain.data.stageChannelID)

		val collection = BotMain.bot.grabRandomMessage(DAYS_BACK, grabChannel, props.useCache)

		return if (collection.isEmpty()) {
			println("daily message could not be retrieved")

			if (currentTry < MAX_TRIES) {
				println("trying again in 1 minute (attempt ${currentTry + 1} of $MAX_TRIES)")
				Reschedule(Duration.ofMinutes(1))

			} else {
				println("maximum daily tries ($MAX_TRIES) exceeded, stopping")
				Done
			}
		} else {
			println("staging daily message")
			BotMain.bot.sendStageMessage(collection, stageChannel)

			Done
		}
	}
}
