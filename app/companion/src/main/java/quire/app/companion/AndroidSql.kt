package quire.app.companion

import android.database.sqlite.SQLiteDatabase
import quire.index.Row
import quire.index.Sql

/**
 * [Sql] over the platform's SQLite, opened writable.
 *
 * A companion-owned copy of `spike/ttsbinding`'s `AndroidSql` rather than a shared one:
 * that copy is a throwaway harness and never a dependency (CLAUDE.md §3), and `core:index`
 * stays pure Kotlin so this port has to live beside whoever uses it. The importer is the
 * only thing that ever writes `dialogue_index.db` — the TTS service only ever reads — so
 * there is no locking problem between the two processes.
 */
class AndroidSql(path: String) : Sql, AutoCloseable {

    private val db: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(path, null)

    override fun execute(statement: String) = db.execSQL(statement)

    override fun update(statement: String, args: List<Any?>) =
        db.execSQL(statement, args.toTypedArray())

    override fun <T> query(statement: String, args: List<Any?>, map: (Row) -> T): List<T> {
        val bind = args.map { it?.toString() }.toTypedArray()
        db.rawQuery(statement, bind).use { cursor ->
            val out = mutableListOf<T>()
            val row = object : Row {
                override fun string(column: Int): String = cursor.getString(column)
                override fun int(column: Int): Int = cursor.getInt(column)
                override fun long(column: Int): Long = cursor.getLong(column)
                override fun double(column: Int): Double = cursor.getDouble(column)
                override fun stringOrNull(column: Int): String? =
                    if (cursor.isNull(column)) null else cursor.getString(column)
            }
            while (cursor.moveToNext()) out += map(row)
            return out
        }
    }

    override fun <T> transaction(body: () -> T): T {
        db.beginTransaction()
        try {
            val result = body()
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }

    override fun close() = db.close()
}
