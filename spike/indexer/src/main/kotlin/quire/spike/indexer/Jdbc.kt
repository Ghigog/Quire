package quire.spike.indexer

import java.sql.Connection
import java.sql.DriverManager
import quire.index.Row
import quire.index.Sql

/**
 * [Sql] over JDBC, so this tool can write the real schema on a desktop.
 *
 * `core:index` has an equivalent in its test source set. Not shared, because promoting it
 * to that module's main source set would put a JDBC driver on a shipped module's compile
 * path to serve a spike. Duplicated here deliberately, and it dies with this module.
 */
class Jdbc(path: String) : Sql, AutoCloseable {

    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$path")

    override fun execute(statement: String) {
        connection.createStatement().use { it.execute(statement) }
    }

    override fun update(statement: String, args: List<Any?>) {
        connection.prepareStatement(statement).use { ps ->
            args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
            ps.executeUpdate()
        }
    }

    override fun <T> query(statement: String, args: List<Any?>, map: (Row) -> T): List<T> {
        connection.prepareStatement(statement).use { ps ->
            args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
            ps.executeQuery().use { rs ->
                val out = mutableListOf<T>()
                while (rs.next()) {
                    out += map(object : Row {
                        override fun string(column: Int) = rs.getString(column + 1)
                        override fun int(column: Int) = rs.getInt(column + 1)
                        override fun long(column: Int) = rs.getLong(column + 1)
                        override fun double(column: Int) = rs.getDouble(column + 1)
                        override fun stringOrNull(column: Int): String? = rs.getString(column + 1)
                    })
                }
                return out
            }
        }
    }

    override fun <T> transaction(body: () -> T): T {
        connection.autoCommit = false
        try {
            val result = body()
            connection.commit()
            return result
        } catch (t: Throwable) {
            connection.rollback()
            throw t
        } finally {
            connection.autoCommit = true
        }
    }

    override fun close() = connection.close()
}
