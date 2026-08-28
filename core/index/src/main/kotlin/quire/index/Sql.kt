package quire.index

/**
 * The narrow slice of SQL this module needs, so `core:index` stays pure Kotlin.
 *
 * Android ships its own SQLite and the desktop JVM needs a JDBC driver; neither belongs in
 * a module that has to run in both. The schema and every statement live here, on this
 * side of the port, so the two platforms share the SQL rather than reimplementing it.
 */
interface Sql {
    fun execute(statement: String)
    fun update(statement: String, args: List<Any?> = emptyList())
    fun <T> query(statement: String, args: List<Any?> = emptyList(), map: (Row) -> T): List<T>
    /** Runs [body] in a transaction, rolling back if it throws. */
    fun <T> transaction(body: () -> T): T
}

/** One row of a result set, read positionally. Columns are 0-based. */
interface Row {
    fun string(column: Int): String
    fun int(column: Int): Int
    fun long(column: Int): Long
    fun double(column: Int): Double
    fun stringOrNull(column: Int): String?
}
