package one.srz.jellywear.presentation.library

import androidx.annotation.StringRes
import java.io.IOException
import one.srz.jellywear.R
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.exception.TimeoutException

// The SDK wraps transport failures (e.g. OkHttp's IOException) in an
// ApiClientException whose own type says nothing useful, so classification
// walks the cause chain; bounded in case something builds a cyclic chain.
private const val MAX_CAUSE_DEPTH = 10

/**
 * Maps an exception from a Jellyfin call to a short, localized,
 * watch-sized error string instead of surfacing the SDK's raw English
 * `message` (see #25): timeouts and I/O failures read as a network
 * problem, 401/403 as an auth problem, 5xx as a server problem,
 * everything else as a generic failure.
 */
@StringRes
fun errorMessageRes(error: Throwable): Int {
    var current: Throwable? = error
    var depth = 0
    while (current != null && depth < MAX_CAUSE_DEPTH) {
        when (current) {
            is TimeoutException -> return R.string.error_network
            is SecureConnectionException -> return R.string.error_network
            is IOException -> return R.string.error_network
            is InvalidStatusException -> return when {
                current.status == 401 || current.status == 403 -> R.string.error_auth
                current.status in 500..599 -> R.string.error_server
                else -> R.string.error_generic
            }
        }
        current = current.cause
        depth++
    }
    return R.string.error_generic
}
