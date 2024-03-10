package org.balduvian.connections

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import org.balduvian.Util.unit
import org.balduvian.createStorage
import java.time.LocalDate
import kotlin.io.path.Path

object Connections {
	private val REFERENCE_NUMBER = 271
	private val REFERENCE_DATE =  LocalDate.of(2024, 3, 8)

	private fun dateFromNumber(number: Int) = REFERENCE_DATE.plusDays((number - REFERENCE_NUMBER).toLong())

	private val configStorage = createStorage<Config>(
		Path("./connections/config.txt"),
		readData = { file -> Config(file.readLines().first().toLong()) },
		writeData = { file, data -> file.writeText(data.roleId.toString()) },
		writePlaceholder = { file -> file.writeText("CONNECTIONS ROLE ID ON THIS LINE") }
	)

	private data class Config(val roleId: Long)

	private var config = configStorage.load()

	fun handleMessage(message: Message) {
		val member = message.member ?: return
		val game = Game.parse(message) ?: return
		val gameDay = dateFromNumber(game.puzzleNumber)
		val today = LocalDate.now()

		if (!isConnectionsUser(member)) return

		val data = ConnectionsData.get()

		val submitDay = data.getSubmitDay(gameDay) ?: return message.reply("yeah not so sure about that puzzle number").submit().unit()
		val submitId = member.idLong

		if (submitDay.complete || submitDay.submittedIds.contains(submitId)) return

		data.addSubmittedId(submitDay, submitId)

		if (today != gameDay) return

		val (roleMembers, submittedMembers) = getSubmittedSet(message.guild, submitDay.submittedIds)

		if (submittedMembers.size == roleMembers.size) {
			data.completeDay(submitDay)
			message.channel.sendMessage("all connections submitted today\nyou may now send messages without spoilers").submit()
		} else {
			message.channel.sendMessage("${submittedMembers.size} out of ${roleMembers.size} connections submitted")
				.submit()
		}
	}

	private fun getRoleMembers(guild: Guild): List<Member> {
		return guild.members.filter { member -> member.roles.any { role -> role.idLong == config.roleId } }
	}

	private fun isConnectionsUser(member: Member): Boolean {
		return member.roles.any { it.idLong == config.roleId }
	}

	data class SubmittedSet(val roleMembers: List<Member>, val submittedMembers: List<Member>)

	private fun getSubmittedSet(guild: Guild, submittedIds: HashSet<Long>): SubmittedSet {
		val roleMembers = getRoleMembers(guild)
		return SubmittedSet(roleMembers, roleMembers.filter { submittedIds.contains(it.idLong) })
	}
}