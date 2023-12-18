package org.balduvian

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimingManager(val dailyEvents: Array<DailyEvent<*>>) {
	private fun nextTime(days: Long, hour: Int): LocalDateTime {
		return LocalDateTime.now().withHour(hour).withMinute(0).withSecond(0).plusDays(days)
	}

	data class ScheduledEvent(val time: LocalDateTime, val doesRepeat: Boolean, val dailyEvent: DailyEvent<*>) {
		companion object {
			fun dailyEvent(time: LocalDateTime, dailyEvent: DailyEvent<*>) = ScheduledEvent(time, true, dailyEvent)

			fun interruptEvent(dailyEvent: DailyEvent<*>) = ScheduledEvent(LocalDateTime.now(), false, dailyEvent)
		}

		override fun toString(): String {
			return "Event (${if (doesRepeat) "daily" else "interrupt" })"
		}

		fun withTime(time: LocalDateTime) = ScheduledEvent(time, doesRepeat, dailyEvent)
	}

	private val eventQueue = ArrayList<ScheduledEvent>()

	private fun scheduleEvent(event: ScheduledEvent) {
		println("Adding $event for ${event.time}")
		eventQueue.add(event)
	}

	fun start() {
		dailyEvents.forEach { dailyEvent ->
			scheduleEvent(ScheduledEvent(nextTime(0, dailyEvent.hour), true, dailyEvent))
		}

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

					val result = try {

					} catch (ex: Throwable) {
						println("event failed")
						println()
					}

					if (event.doesRepeat) {
						if (BotMain.isDoneToday()) {
							println("The message has already been sent today, setting next message time 1 day forward")
							scheduleEvent(ScheduledEvent.dailyEvent(nextTime(1)))

						} else {
							val collection = BotMain.bot.grabRandomMessage(DAYS_BACK, grabChannel, false)

							if (collection.isEmpty()) {
								println("Daily message could not be retrieved, trying again in 1 minute")
								scheduleEvent(ScheduledEvent.dailyEvent(LocalDateTime.now().plusMinutes(1)))

							} else {
								println("Staging daily message")
								BotMain.bot.sendStageMessage(collection, stageChannel)

								/* schedule next daily event */
								scheduleEvent(ScheduledEvent.dailyEvent(nextTime(1)))

								println("Setting sent for today")
								BotMain.lastDay = LastDay.create()
								BotMain.lastDay?.save()
							}
						}
					} else {
						val collection = BotMain.bot.grabRandomMessage(DAYS_BACK, grabChannel, event.cached)

						if (collection.isEmpty()) {
							println("Message could not be retrieved, trying again in 1 minute")
							scheduleEvent(event.withTime(LocalDateTime.now().plusMinutes(1)))

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
