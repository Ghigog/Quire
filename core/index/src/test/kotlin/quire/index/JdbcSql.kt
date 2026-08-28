package quire.index

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/**
 * [Sql] over JDBC, for exercising the real schema on a desktop JVM.
 *
 * Test-only. Android supplies its own implementation over the platform's SQLite; the point
 * of the port is that both run the same statements from [Schema] and [IndexWriter].
 */
class JdbcSql(path: String, readOnly: Boolean = false) : Sql, AutoCloseable {

    private val connection: Connection =
        DriverManager.getConnection("jdbc:sqlite:$path").apply {
            if (readOnly) createStatement().use { it.execute("PRAGMA query_only = ON") }
        }

    override fun execute(statement: String) {
        connection.createStatement().use { it.execute(statement) }
    }

    override fun update(statement: String, args: List<Any?>) {
        connection.prepareStatement(statement).use { ps ->
            bind(ps, args)
            ps.executeUpdate()
        }
    }

    override fun <T> query(statement: String, args: List<Any?>, map: (Row) -> T): List<T> =
        connection.prepareStatement(statement).use { ps ->
            bind(ps, args)
            ps.executeQuery().use { rs ->
                buildList { while (rs.next()) add(map(JdbcRow(rs))) }
            }
        }

    override fun <T> transaction(body: () -> T): T {
        connection.autoCommit = false
        return try {
            body().also { connection.commit() }
        } catch (t: Throwable) {
            connection.rollback()
            throw t
        } finally {
            connection.autoCommit = true
        }
    }

    override fun close() = connection.close()

    private fun bind(ps: java.sql.PreparedStatement, args: List<Any?>) {
        args.forEachIndexed { i, arg ->
            when (arg) {
                null -> ps.setNull(i + 1, java.sql.Types.VARCHAR)
                is Int -> ps.setInt(i + 1, arg)
                is Long -> ps.setLong(i + 1, arg)
                is Double -> ps.setDouble(i + 1, arg)
                else -> ps.setString(i + 1, arg.toString())
            }
        }
    }

    private class JdbcRow(private val rs: ResultSet) : Row {
        override fun string(column: Int): String = rs.getString(column + 1)
        override fun int(column: Int): Int = rs.getInt(column + 1)
        override fun long(column: Int): Long = rs.getLong(column + 1)
        override fun double(column: Int): Double = rs.getDouble(column + 1)
        override fun stringOrNull(column: Int): String? = rs.getString(column + 1)
    }
}
