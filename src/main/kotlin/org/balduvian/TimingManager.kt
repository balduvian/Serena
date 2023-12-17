package org.balduvian

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.system.exitProcess

object TimingManager {
	const val SEND_HOUR = 12
	const val DAYS_BACK = 30

	fun nextTime(days: Long): LocalDateTime {
		return LocalDateTime.now().withHour(SEND_HOUR).withMinute(0).withSecond(0).plusDays(days)
	}

	private fun getChannel(id: Long): TextChannel {
		val channel = BotMain.bot.getChannel(id)

		if (channel == null) {
			println("Channel of ID $id does not exist")
			exitProcess(-1)
		}

		return channel
	}

	data class Event(val time: LocalDateTime, val daily: Boolean, val cached: Boolean) {
		companion object {
			fun dailyEvent(time: LocalDateTime) = Event(time, true, false)

			fun interruptEvent(cached: Boolean) = Event(LocalDateTime.now(), false, cached)
		}

		override fun toString(): String {
			return "Event (${if (daily) "daily" else "interrupt, ${if (cached) "cached" else "refresh"}"})"
		}

		fun withTime(time: LocalDateTime) = Event(time, daily, cached)
	}

	private val eventQueue = ArrayList<Event>()

	fun addEvent(event: Event) {
		println("Adding $event for ${event.time}")
		eventQueue.add(event)
	}

	fun start() {
		addEvent(Event.dailyEvent(nextTime(0)))

		Thread {
			while (true) {
				val now = LocalDateTime.now()

				val eventIndex = eventQueue.indexOfFirst { event ->
					ChronoUnit.SECONDS.between(now, event.time) <= 0
				}

				/* it's time for one of the events in the queue */
				if (eventIndex != -1) {
					val event = eventQueue.removeAt(eventIndex)

					println("Triggered $event")

					val grabChannel = getChannel(BotMain.data.grabChannelID)
					val stageChannel = getChannel(BotMain.data.stageChannelID)

					if (event.daily) {
						if (BotMain.isDoneToday()) {
							println("The message has already been sent today, setting next message time 1 day forward")
							addEvent(Event.dailyEvent(nextTime(1)))

						} else {
							val collection = BotMain.bot.grabRandomMessage(DAYS_BACK, grabChannel, false)

							if (collection.isEmpty()) {
								println("Daily message could not be retrieved, trying again in 1 minute")
								addEvent(Event.dailyEvent(LocalDateTime.now().plusMinutes(1)))

							} else {
								println("Staging daily message")
								BotMain.bot.sendStageMessage(collection, stageChannel)

								/* schedule next daily event */
								addEvent(Event.dailyEvent(nextTime(1)))

								println("Setting sent for today")
								BotMain.lastDay = LastDay.create()
								BotMain.lastDay?.save()
							}
						}
					} else {
						val collection = BotMain.bot.grabRandomMessage(DAYS_BACK, grabChannel, event.cached)

						if (collection.isEmpty()) {
							println("Message could not be retrieved, trying again in 1 minute")
							addEvent(event.withTime(LocalDateTime.now().plusMinutes(1)))

						} else {
							println("Staging message")
							BotMain.bot.addCommandReactions(BotMain.bot.sendStageMessage(collection, stageChannel))
						}
					}
				}

				Thread.sleep(ChronoUnit.SECONDS.duration.toMillis())
			}
		}.start()
	}
}
