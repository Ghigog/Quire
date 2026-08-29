package quire.spike.tts

import android.database.sqlite.SQLiteDatabase
import quire.index.Row
import quire.index.Sql

/**
 * [Sql] over the platform's SQLite — the implementation `core:index` was designed around.
 *
 * The port exists so `core:index` stays pure Kotlin and can be tested on a desktop in
 * seconds. The schema and every statement live on that side; this supplies the driver and
 * nothing else, which is why it has no knowledge of tables or columns.
 *
 * Opened read-only: the index belongs to the companion app, and the TTS service having no
 * way to write it is the reason there is no locking problem between the two processes.
 */
class AndroidSql(path: String) : Sql, AutoCloseable {

    private val db: SQLiteDatabase =
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)

    override fun execute(statement: String) = db.execSQL(statement)

    override fun update(statement: String, args: List<Any?>) =
        db.execSQL(statement, args.toTypedArray())

    override fun <T> query(statement: String, args: List<Any?>, map: (Row) -> T): List<T> {
        // rawQuery binds everything as a string; the schema's integer columns are read
        // back through Cursor.getInt/getLong, which converts, so this is lossless for the
        // values core:index actually binds.
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
