package org.balduvian.db

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class DB(val connection: Connection) {
  companion object {
    fun connect(): DB {
      val connection = DriverManager.getConnection("jdbc:sqlite:serena.db")
      connection.autoCommit = true
      return DB(connection)
    }
  }

  fun execDiscard(query: String) = connection.createStatement().use { it.execute(query) }

  inline fun <T> execResultSet(query: String, callback: (ResultSet) -> T) =
      connection.createStatement().use { it.executeQuery(query).use(callback) }

  fun setup() {
    execDiscard(
        """
                CREATE TABLE IF NOT EXISTS [connectionsSubmission] (
                    [date]          TEXT,
                    [id]            INT,
                    [gameString]    TEXT
                );                
            """
            .trimIndent())
    execDiscard(
        """
                CREATE TABLE IF NOT EXISTS [token] (
                    [value]         TEXT
                );
            """
            .trimIndent())
  }

  fun getToken(): String =
      execResultSet("SELECT [value] FROM [token]") {
        if (!it.next()) throw Exception("empty results")
        val token = it.getString(1)
        token
      }
}

fun main() {
  val db = DB.connect()
  // db.setup()

  db.execDiscard("INSERT INTO [token] VALUES ('...token...')")

  val token = db.getToken()

  val g = 0

  db.connection.commit()
}
