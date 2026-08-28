package quire.spike

/**
 * Minimal RFC 4180 reader.
 *
 * PDNC's quotation files carry quoted fields containing commas *and* newlines — a quote
 * spanning a paragraph break is one field — so a line-splitting reader silently mangles
 * them. No dependency for this: the format is small enough to read correctly in 40 lines.
 */
object Csv {

    fun parse(text: String): List<Map<String, String>> {
        val rows = rows(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first()
        return rows.drop(1)
            .filter { it.size == header.size }
            .map { row -> header.indices.associate { header[it] to row[it] } }
    }

    private fun rows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> { row += field.toString(); field.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row += field.toString(); field.clear()
                    rows += row; row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); rows += row }
        return rows.filter { it.size > 1 }
    }
}
