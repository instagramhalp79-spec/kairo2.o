package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KAIRO Music", appName)
  }

  @Test
  fun `youtube search returns real results with videoId`() {
    kotlinx.coroutines.runBlocking {
      val results = com.example.core.youtube.YouTubeMediaResolver.search("Arijit Singh Tum Hi Ho")
      org.junit.Assert.assertTrue("Expected search results", results.isNotEmpty())
      val first = results.first()
      org.junit.Assert.assertNotNull("videoId must not be null", first.videoId)
      org.junit.Assert.assertTrue("videoId must not be empty", first.videoId!!.isNotBlank())
      org.junit.Assert.assertTrue("title must not be empty", first.title.isNotBlank())
      org.junit.Assert.assertTrue("artist must not be empty", first.artist.isNotBlank())
      org.junit.Assert.assertTrue("artworkUrl must not be empty", first.artworkUrl.isNotBlank())
    }
  }
}
