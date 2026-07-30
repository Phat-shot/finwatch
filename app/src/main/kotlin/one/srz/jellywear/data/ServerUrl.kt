package one.srz.jellywear.data

/**
 * Pure string logic for turning user-typed server addresses into the base
 * URLs [JellyfinSession] actually connects to. Extracted from
 * [JellyfinSession] so the security-relevant parts (https-first probing,
 * never downgrading to http) are unit-testable without any Android or
 * network dependencies.
 */
object ServerUrl {

    /**
     * Normalizes user input: trims surrounding whitespace, drops trailing
     * slashes, and lowercases. Hostnames are case-insensitive; normalizing
     * avoids e.g. Settings showing "MyServer.Local" vs "myserver.local"
     * depending on how the user happened to type it.
     */
    fun normalize(input: String): String = input.trim().trimEnd('/').lowercase()

    /** True if [url] carries an explicit scheme ("scheme://..."). */
    fun hasExplicitScheme(url: String): Boolean = url.contains("://")

    /**
     * The base URLs to probe for [input], in order. An explicit scheme is
     * respected as-is (the user's choice, never rewritten). Schemeless
     * input is tried https:// first -- credentials and the access token
     * should never travel in cleartext when the server supports TLS -- with
     * http:// (e.g. a LAN-local server without certificates) as the only
     * fallback.
     */
    fun candidates(input: String): List<String> {
        val normalized = normalize(input)
        return if (hasExplicitScheme(normalized)) {
            listOf(normalized)
        } else {
            listOf("https://$normalized", "http://$normalized")
        }
    }

    /**
     * The https:// twin of an http:// [baseUrl], or null if [baseUrl] is
     * already https (or has no recognizable scheme). Only the http -> https
     * direction exists: the reverse would silently resend credentials over
     * a cleartext connection.
     */
    fun upgradeToHttps(baseUrl: String): String? {
        if (!baseUrl.startsWith("http://")) return null
        return "https://${baseUrl.removePrefix("http://")}"
    }
}
