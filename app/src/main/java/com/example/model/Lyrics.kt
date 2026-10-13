package com.example.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsData(
    val lines: List<LyricLine>,
    val plainText: String,
    val isSynced: Boolean
) {
    fun getActiveLineIndex(positionMs: Long): Int {
        if (!isSynced || lines.isEmpty()) return -1
        return lines.indexOfLast { it.timestampMs <= positionMs }
    }
}

object LrcParser {
    private val lrcRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")

    fun parse(lrcContent: String): LyricsData {
        if (lrcContent.isBlank()) {
            return LyricsData(emptyList(), "Lyrics not available.", false)
        }

        val lines = mutableListOf<LyricLine>()
        val plainBuilder = StringBuilder()

        lrcContent.lineSequence().forEach { line ->
            val match = lrcRegex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msGroup = match.groupValues[3]
                val ms = when (msGroup.length) {
                    1 -> (msGroup.toLongOrNull() ?: 0L) * 100
                    2 -> (msGroup.toLongOrNull() ?: 0L) * 10
                    3 -> msGroup.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val totalMs = min * 60 * 1000 + sec * 1000 + ms
                val text = match.groupValues[4].trim()

                if (text.isNotEmpty()) {
                    lines.add(LyricLine(totalMs, text))
                    plainBuilder.appendLine(text)
                }
            } else if (!line.startsWith("[") && line.isNotBlank()) {
                plainBuilder.appendLine(line.trim())
            }
        }

        lines.sortBy { it.timestampMs }

        return if (lines.isNotEmpty()) {
            LyricsData(lines, plainBuilder.toString().trim(), true)
        } else {
            LyricsData(emptyList(), lrcContent.trim(), false)
        }
    }
}
