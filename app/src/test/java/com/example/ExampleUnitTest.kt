package com.example

import com.example.model.LrcParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun lrcParser_parsesTimestampsCorrectly() {
    val lrc = """
      [00:12.50]First line
      [00:25.00]Second line
      [01:05.20]Third line
    """.trimIndent()

    val parsed = LrcParser.parse(lrc)
    assertTrue(parsed.isSynced)
    assertEquals(3, parsed.lines.size)
    assertEquals(12500L, parsed.lines[0].timestampMs)
    assertEquals("First line", parsed.lines[0].text)
    assertEquals(25000L, parsed.lines[1].timestampMs)
    assertEquals("Second line", parsed.lines[1].text)
    assertEquals(65200L, parsed.lines[2].timestampMs)
    assertEquals("Third line", parsed.lines[2].text)
  }

  @Test
  fun lyricsData_findsActiveLineCorrectly() {
    val lrc = """
      [00:10.00]Line 1
      [00:20.00]Line 2
      [00:30.00]Line 3
    """.trimIndent()

    val parsed = LrcParser.parse(lrc)
    assertEquals(-1, parsed.getActiveLineIndex(5000L)) // before line 1
    assertEquals(0, parsed.getActiveLineIndex(12000L)) // inside line 1
    assertEquals(1, parsed.getActiveLineIndex(25000L)) // inside line 2
    assertEquals(2, parsed.getActiveLineIndex(35000L)) // after line 3
  }
}

