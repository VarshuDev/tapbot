package com.tapbot.core.runner

import com.tapbot.core.runner.audio.AudioSourceType
import com.tapbot.core.runner.audio.AudioStreamResolver
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AudioStreamResolverTest {

    private lateinit var resolver: AudioStreamResolver

    @Before
    fun setUp() {
        resolver = AudioStreamResolver()
    }

    @Test
    fun `getCuratedRadioStations returns 6 distinct genres`() {
        val stations = resolver.getCuratedRadioStations()
        assertEquals(6, stations.size)
        assertTrue(stations.any { it.genre == "lofi" })
        assertTrue(stations.any { it.genre == "synthwave" })
        assertTrue(stations.any { it.genre == "chillhop" })
        assertTrue(stations.any { it.genre == "rock" })
        assertTrue(stations.any { it.genre == "jazz" })
        assertTrue(stations.any { it.genre == "ambient" })
        assertTrue(stations.all { it.source == AudioSourceType.RADIO_STATION })
        assertTrue(stations.all { it.durationFormatted == "LIVE 🔴" })
    }

    @Test
    fun `getRadioStationByGenre matches exact genre and title keywords`() {
        val lofi = resolver.getRadioStationByGenre("lofi")
        assertNotNull(lofi)
        assertEquals("Lofi Girl Study Beats", lofi?.title)

        val jazz = resolver.getRadioStationByGenre("jazz")
        assertNotNull(jazz)
        assertEquals("Jazz Cafe & Smooth Bossa", jazz?.title)

        val synth = resolver.getRadioStationByGenre("synthwave")
        assertNotNull(synth)
        assertEquals("Nightwave Plaza 80s Cyber", synth?.title)
    }

    @Test
    fun `resolveTrack with direct audio URL extracts title and sets DIRECT_URL source`() = runBlocking {
        val url = "https://cdn.example.com/music/summer_vibes_lofi.mp3"
        val result = resolver.resolveTrack(url)

        assertTrue(result.isSuccess)
        val track = result.getOrThrow()
        assertEquals("Summer vibes lofi", track.title)
        assertEquals(url, track.streamUrl)
        assertEquals(AudioSourceType.DIRECT_URL, track.source)
    }

    @Test
    fun `resolveTrack with radio genre query returns corresponding radio station`() = runBlocking {
        val result = resolver.resolveTrack("synthwave")
        assertTrue(result.isSuccess)
        val track = result.getOrThrow()
        assertEquals("Nightwave Plaza 80s Cyber", track.title)
        assertEquals(AudioSourceType.RADIO_STATION, track.source)
    }

    @Test
    fun `resolveTrack with custom yt-dlp API parses track response`() = runBlocking {
        val interceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    """
                    {
                        "title": "Never Gonna Give You Up",
                        "uploader": "Rick Astley",
                        "url": "https://googlevideo.example.com/videoplayback?id=rickroll",
                        "duration": 213,
                        "thumbnail": "https://i.ytimg.com/vi/dQw4w9WgXcQ/default.jpg"
                    }
                    """.trimIndent().toResponseBody("application/json".toMediaType())
                )
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val customResolver = AudioStreamResolver(client)

        val result = customResolver.resolveTrack(
            query = "rick astley never gonna give you up",
            customResolverUrl = "https://my-ytdlp.example.com/api/stream"
        )

        assertTrue(result.isSuccess)
        val track = result.getOrThrow()
        assertEquals("Never Gonna Give You Up", track.title)
        assertEquals("Rick Astley", track.artist)
        assertEquals("https://googlevideo.example.com/videoplayback?id=rickroll", track.streamUrl)
        assertEquals(213, track.durationSeconds)
        assertEquals("03:33", track.durationFormatted)
        assertEquals(AudioSourceType.YTDLP_API, track.source)
    }

    @Test
    fun `resolveTrack blocks private metadata or cleartext audio URLs`() = runBlocking {
        val metaResult = resolver.resolveTrack("https://169.254.169.254/latest/meta-data/audio.mp3")
        assertFalse(metaResult.isSuccess)
        assertTrue(metaResult.exceptionOrNull() is SecurityException)

        val httpResult = resolver.resolveTrack("http://cdn.example.com/music/test.mp3")
        assertFalse(httpResult.isSuccess)
        assertTrue(httpResult.exceptionOrNull() is SecurityException)

        val internalResult = resolver.resolveTrack("https://metadata.google.internal/stream.mp3")
        assertFalse(internalResult.isSuccess)
        assertTrue(internalResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `resolveTrack with blank query fails with IllegalArgumentException`() = runBlocking {
        val result = resolver.resolveTrack("   ")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `resolveTrack fallback returns valid station when offline`() = runBlocking {
        // Query that is not a direct URL and not a genre
        val result = resolver.resolveTrack("obscure underground experimental noise track 9876")
        assertTrue(result.isSuccess)
        val track = result.getOrThrow()
        assertTrue(track.streamUrl.isNotBlank())
        assertTrue(track.title.contains("obscure underground"))
    }
}
