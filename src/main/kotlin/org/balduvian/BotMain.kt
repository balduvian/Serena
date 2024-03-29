import org.balduvian.Data
import org.balduvian.LastDay
import org.balduvian.Serena
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.system.exitProcess

object BotMain {
	var data = Data.get() ?: run {
		println("Could not load data file")
		exitProcess(-1)
	}

	val token = readToken("./token.txt") ?: run {
		println("Token file not found")
		exitProcess(-1)
	}

	val bot: Serena = Serena.createBot(token) ?: run {
		println("Bot could not be created")
		exitProcess(-1)
	}

	var lastDay = LastDay.get()

	private fun readToken(filename: String): String? {
		val file = File(filename)

		return if (file.exists() && file.isFile) {
			val reader = BufferedReader(FileReader(file))

			reader.readLine()

		} else {
			null
		}
	}

	fun isDoneToday(): Boolean {
		return lastDay?.today() ?: false
	}
}

fun main() {
	BotMain

	while (true) {
		val input = try {
			readln()
		} catch (ex: Throwable) {
			println("Not in a suitable console environment")
			break
		}

		if ("quit".startsWith(input)) {
			println("Bot shutting down...")

			BotMain.bot.jda.shutdown()
			exitProcess(0)
		}

		if ("restart".startsWith(input)) {
			println("Restarting...")

			val runningDir = System.getProperty("user.dir")
			Runtime.getRuntime().exec("cmd.exe /c start cmd.exe /k \"cd \"$runningDir\" & run.bat\"")
			exitProcess(67)
		}
	}
}
