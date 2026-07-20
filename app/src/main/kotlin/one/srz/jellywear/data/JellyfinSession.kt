package one.srz.jellywear.data

import android.content.Context
import androidx.core.content.edit
import one.srz.jellywear.BuildConfig
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

/**
 * Holds the current Jellyfin server connection, persisted across process
 * restarts via SharedPreferences. The access token itself is encrypted at
 * rest with an Android Keystore-backed key, see [SecureTokenStore].
 */
class JellyfinSession private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val jellyfin: Jellyfin = createJellyfin {
        this.context = context.applicationContext
        clientInfo = ClientInfo(name = "jellywear", version = BuildConfig.VERSION_NAME)
    }

    var api: ApiClient? = null
        private set

    val isLoggedIn: Boolean get() = api != null

    val userId: UUID? get() = prefs.getString(KEY_USER_ID, null)?.toUUIDOrNull()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return
        val encryptedToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return
        val token = SecureTokenStore.decrypt(encryptedToken) ?: return
        api = jellyfin.createApi(baseUrl = serverUrl, accessToken = token)
    }

    suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> = try {
        val normalizedUrl = serverUrl.trim().trimEnd('/')
        val client = jellyfin.createApi(baseUrl = normalizedUrl)
        val authResult = client.userApi.authenticateUserByName(
            username = username,
            password = password,
        ).content

        val token = authResult.accessToken
        if (token == null) {
            Result.failure(IllegalStateException("Server did not return an access token"))
        } else {
            client.update(accessToken = token)
            api = client
            prefs.edit {
                putString(KEY_SERVER_URL, normalizedUrl)
                putString(KEY_ACCESS_TOKEN, SecureTokenStore.encrypt(token))
                putString(KEY_USER_ID, authResult.user?.id?.toString())
            }
            Result.success(Unit)
        }
    } catch (e: ApiClientException) {
        Result.failure(e)
    }

    fun logout() {
        api = null
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "jellyfin_session"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"

        @Volatile
        private var instance: JellyfinSession? = null

        fun getInstance(context: Context): JellyfinSession =
            instance ?: synchronized(this) {
                instance ?: JellyfinSession(context).also { instance = it }
            }
    }
}
