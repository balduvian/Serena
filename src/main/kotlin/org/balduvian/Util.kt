package org.balduvian

import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User

object Util {
	fun mention(user: User): String {
		return "<@${user.id}>"
	}

	fun mention(role: Role): String {
		return "<@&${role.id}>"
	}

	fun Any?.unit() {}
	fun Any?.void(): Void? = null
}
