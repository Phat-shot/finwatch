package one.srz.jellywear.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the security-relevant server URL handling extracted
 * into [ServerUrl]: https-first probing for schemeless input, respecting an
 * explicitly typed scheme, and never downgrading https to http.
 */
class ServerUrlTest {

    // --- normalize ---

    @Test
    fun `normalize trims surrounding whitespace`() {
        assertEquals("demo.jellyfin.org", ServerUrl.normalize("  demo.jellyfin.org "))
    }

    @Test
    fun `normalize drops trailing slashes but keeps inner path`() {
        assertEquals(
            "https://example.com/jellyfin",
            ServerUrl.normalize("https://example.com/jellyfin///"),
        )
    }

    @Test
    fun `normalize lowercases -- hostnames are case-insensitive`() {
        assertEquals("https://myserver.local", ServerUrl.normalize("HTTPS://MyServer.Local"))
    }

    @Test
    fun `normalize leaves an already clean url untouched`() {
        assertEquals("http://192.168.1.10:8096", ServerUrl.normalize("http://192.168.1.10:8096"))
    }

    // --- hasExplicitScheme ---

    @Test
    fun `explicit scheme is detected for http and https`() {
        assertTrue(ServerUrl.hasExplicitScheme("https://example.com"))
        assertTrue(ServerUrl.hasExplicitScheme("http://example.com"))
    }

    @Test
    fun `bare host is schemeless, even with a port`() {
        assertFalse(ServerUrl.hasExplicitScheme("example.com"))
        // ":" alone (port separator) must not count as a scheme.
        assertFalse(ServerUrl.hasExplicitScheme("example.com:8096"))
    }

    // --- candidates (https-first probing) ---

    @Test
    fun `schemeless input probes https first, http second`() {
        assertEquals(
            listOf("https://myserver.local:8096", "http://myserver.local:8096"),
            ServerUrl.candidates("myserver.local:8096"),
        )
    }

    @Test
    fun `explicit https input is used as-is with no fallback`() {
        assertEquals(
            listOf("https://example.com"),
            ServerUrl.candidates("https://example.com"),
        )
    }

    @Test
    fun `explicit http input is respected -- no forced https attempt`() {
        // The user's explicit choice wins; the login-retry upgrade path is
        // handled separately by upgradeToHttps.
        assertEquals(
            listOf("http://192.168.1.10:8096"),
            ServerUrl.candidates("http://192.168.1.10:8096"),
        )
    }

    @Test
    fun `candidates are built from normalized input`() {
        assertEquals(
            listOf("https://myserver.local", "http://myserver.local"),
            ServerUrl.candidates("  MyServer.Local/ "),
        )
    }

    // --- upgradeToHttps (no-downgrade guarantee) ---

    @Test
    fun `http upgrades to https keeping port and path`() {
        assertEquals(
            "https://example.com:8096/jellyfin",
            ServerUrl.upgradeToHttps("http://example.com:8096/jellyfin"),
        )
    }

    @Test
    fun `https is never rewritten -- upgrade only exists for http`() {
        assertNull(ServerUrl.upgradeToHttps("https://example.com"))
    }

    @Test
    fun `schemeless url cannot be upgraded`() {
        assertNull(ServerUrl.upgradeToHttps("example.com"))
    }
}
