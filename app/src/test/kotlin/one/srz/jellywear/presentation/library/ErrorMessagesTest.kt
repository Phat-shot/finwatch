package one.srz.jellywear.presentation.library

import java.io.IOException
import kotlinx.serialization.SerializationException
import one.srz.jellywear.R
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMessagesTest {

    @Test
    fun `timeouts and IO failures read as network problems`() {
        assertEquals(R.string.error_network, errorMessageRes(TimeoutException("timeout")))
        assertEquals(R.string.error_network, errorMessageRes(IOException("reset")))
        // ...also when the SDK wraps them in an opaque ApiClientException.
        assertEquals(
            R.string.error_network,
            errorMessageRes(ApiClientException("wrapped", IOException("reset"))),
        )
    }

    @Test
    fun `status codes split into auth, server, and generic`() {
        assertEquals(R.string.error_auth, errorMessageRes(InvalidStatusException(401)))
        assertEquals(R.string.error_auth, errorMessageRes(InvalidStatusException(403)))
        assertEquals(R.string.error_server, errorMessageRes(InvalidStatusException(503)))
        assertEquals(R.string.error_generic, errorMessageRes(InvalidStatusException(404)))
    }

    @Test
    fun `non-JSON response body reads as wrong address, not network error`() {
        // What the demo server's landing page produces: HTTP 200 with an
        // HTML document, which the SDK turns into InvalidContentException
        // wrapping the kotlinx SerializationException.
        val htmlInsteadOfJson = InvalidContentException(
            "Deserialization failed",
            SerializationException("Unexpected JSON token at offset 0"),
        )
        assertEquals(R.string.error_not_jellyfin, errorMessageRes(htmlInsteadOfJson))
    }

    @Test
    fun `unclassified errors fall back to generic`() {
        assertEquals(R.string.error_generic, errorMessageRes(IllegalStateException("odd")))
    }
}
