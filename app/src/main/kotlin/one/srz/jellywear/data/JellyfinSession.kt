package one.srz.jellywear.data

import android.content.Context
import androidx.core.content.edit
import one.srz.jellywear.BuildConfig
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.authenticateWithQuickConnect
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.AuthenticationResult
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.QuickConnectResult
import org.jellyfin.sdk.model.api.request.GetItemsRequest
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

    val username: String? get() = prefs.getString(KEY_USERNAME, null)

    // lowercase() here too, not just when it's written -- installs updated
    // from before that normalization existed still have the original-case
    // value stored.
    val serverUrl: String? get() = prefs.getString(KEY_SERVER_URL, null)?.lowercase()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val storedServerUrl = prefs.getString(KEY_SERVER_URL, null)?.lowercase() ?: return
        val encryptedToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return
        val token = SecureTokenStore.decrypt(encryptedToken) ?: return
        api = jellyfin.createApi(baseUrl = storedServerUrl, accessToken = token)
    }

    /**
     * Builds an unauthenticated client bound to [serverUrl] and verifies it
     * can actually reach a Jellyfin server. If the user didn't type a
     * scheme, tries http:// first and falls back to https:// (many
     * reverse-proxied Jellyfin servers are https-only) before giving up.
     */
    suspend fun buildVerifiedClient(serverUrl: String): Result<ApiClient> {
        // Hostnames are case-insensitive; normalizing avoids e.g. Settings
        // showing "MyServer.Local" vs "myserver.local" depending on how the
        // user happened to type it.
        val trimmed = serverUrl.trim().trimEnd('/').lowercase()
        val hasExplicitScheme = trimmed.contains("://")

        val primaryClient = jellyfin.createApi(baseUrl = if (hasExplicitScheme) trimmed else "http://$trimmed")
        val primaryError = pingServer(primaryClient)
        if (primaryError == null) return Result.success(primaryClient)

        if (!hasExplicitScheme) {
            val fallbackClient = jellyfin.createApi(baseUrl = "https://$trimmed")
            val fallbackError = pingServer(fallbackClient)
            if (fallbackError == null) return Result.success(fallbackClient)
        }

        return Result.failure(primaryError)
    }

    /**
     * Rebuilds a client against the opposite http/https scheme of [baseUrl],
     * or null if [baseUrl] has neither prefix.
     *
     * Used as a second-chance retry around the actual sign-in call: the
     * http(s) connectivity check in [buildVerifiedClient] is a GET, which
     * some reverse proxies transparently redirect from http to https --
     * making the check pass even though a POST (the real login call) hits
     * the same redirect and has its body dropped per HTTP redirect
     * semantics, failing where the GET didn't.
     */
    fun buildClientWithFlippedScheme(baseUrl: String): ApiClient? {
        val flipped = when {
            baseUrl.startsWith("http://") -> "https://${baseUrl.removePrefix("http://")}"
            baseUrl.startsWith("https://") -> "http://${baseUrl.removePrefix("https://")}"
            else -> return null
        }
        return jellyfin.createApi(baseUrl = flipped)
    }

    /** Null on success, the failure otherwise. */
    private suspend fun pingServer(client: ApiClient): Throwable? = try {
        client.systemApi.getPublicSystemInfo()
        null
    } catch (e: ApiClientException) {
        e
    } catch (e: IllegalArgumentException) {
        e
    }

    suspend fun login(client: ApiClient, username: String, password: String): Result<Unit> = try {
        val authResult = client.userApi.authenticateUserByName(
            username = username,
            password = password,
        ).content
        completeLogin(client, authResult)
    } catch (e: ApiClientException) {
        Result.failure(e)
    } catch (e: IllegalArgumentException) {
        // e.g. a server URL that OkHttp's URL parser rejects outright.
        Result.failure(e)
    }

    suspend fun isQuickConnectEnabled(client: ApiClient): Boolean = try {
        client.quickConnectApi.getQuickConnectEnabled().content
    } catch (e: ApiClientException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    suspend fun initiateQuickConnect(client: ApiClient): Result<QuickConnectResult> = try {
        Result.success(client.quickConnectApi.initiateQuickConnect().content)
    } catch (e: ApiClientException) {
        Result.failure(e)
    }

    /** Polls once. Result.success(false) means "still waiting", Result.success(true) means logged in. */
    suspend fun pollQuickConnect(client: ApiClient, secret: String): Result<Boolean> = try {
        val state = client.quickConnectApi.getQuickConnectState(secret).content
        if (!state.authenticated) {
            Result.success(false)
        } else {
            val authResult = client.userApi.authenticateWithQuickConnect(secret).content
            completeLogin(client, authResult).map { true }
        }
    } catch (e: ApiClientException) {
        Result.failure(e)
    }

    private fun completeLogin(client: ApiClient, authResult: AuthenticationResult): Result<Unit> {
        val token = authResult.accessToken
            ?: return Result.failure(IllegalStateException("Server did not return an access token"))
        client.update(accessToken = token)
        api = client
        prefs.edit {
            putString(KEY_SERVER_URL, client.baseUrl)
            putString(KEY_ACCESS_TOKEN, SecureTokenStore.encrypt(token))
            putString(KEY_USER_ID, authResult.user?.id?.toString())
            putString(KEY_USERNAME, authResult.user?.name)
        }
        return Result.success(Unit)
    }

    /** Fetches items for [request] and returns them shuffled, or null on error/empty result. */
    suspend fun fetchShuffledQueue(request: GetItemsRequest): List<BaseItemDto>? = try {
        api?.itemsApi?.getItems(request)?.content?.items
            ?.takeIf { it.isNotEmpty() }
            ?.shuffled()
    } catch (e: ApiClientException) {
        null
    }

    /**
     * Audiobooks aren't reliably tagged as BaseItemKind.AUDIO_BOOK on every
     * server, so instead of filtering by kind, this finds the user's
     * "Books" library views and lists every audio item underneath them.
     */
    suspend fun fetchAudiobooks(): List<BaseItemDto> {
        val currentApi = api ?: return emptyList()
        return try {
            val bookLibraries = currentApi.userViewsApi.getUserViews(userId = userId).content.items
                .filter { it.collectionType == CollectionType.BOOKS }
            bookLibraries.flatMap { library ->
                currentApi.itemsApi.getItems(
                    GetItemsRequest(
                        userId = userId,
                        parentId = library.id,
                        recursive = true,
                        mediaTypes = listOf(MediaType.AUDIO),
                    ),
                ).content.items
            }
        } catch (e: ApiClientException) {
            emptyList()
        }
    }

    /** Favorited songs, server-wide. */
    suspend fun fetchFavoriteMusic(): List<BaseItemDto> {
        val currentApi = api ?: return emptyList()
        return try {
            currentApi.itemsApi.getItems(
                GetItemsRequest(
                    userId = userId,
                    recursive = true,
                    includeItemTypes = listOf(BaseItemKind.AUDIO),
                    isFavorite = true,
                    sortBy = listOf(ItemSortBy.SORT_NAME),
                ),
            ).content.items
        } catch (e: ApiClientException) {
            emptyList()
        }
    }

    /** All tracks across every playlist, server-wide (for the Playlists tile's shuffle-all). */
    suspend fun fetchPlaylistTracks(): List<BaseItemDto> {
        val currentApi = api ?: return emptyList()
        return try {
            val playlists = currentApi.itemsApi.getItems(
                GetItemsRequest(
                    userId = userId,
                    recursive = true,
                    includeItemTypes = listOf(BaseItemKind.PLAYLIST),
                ),
            ).content.items
            playlists.flatMap { playlist ->
                currentApi.itemsApi.getItems(
                    GetItemsRequest(
                        userId = userId,
                        parentId = playlist.id,
                        recursive = true,
                        mediaTypes = listOf(MediaType.AUDIO, MediaType.VIDEO),
                    ),
                ).content.items
            }
        } catch (e: ApiClientException) {
            emptyList()
        }
    }

    /** Primary-image URL for [itemId], sized for a small chip thumbnail, or null if not logged in. */
    fun imageUrl(itemId: UUID): String? {
        val currentApi = api ?: return null
        val url = currentApi.imageApi.getItemImageUrl(itemId = itemId, imageType = ImageType.PRIMARY, maxWidth = 120)
        val separator = if (url.contains("?")) "&" else "?"
        return "$url$separator${ApiClient.QUERY_ACCESS_TOKEN}=${currentApi.accessToken}"
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
        private const val KEY_USERNAME = "username"

        @Volatile
        private var instance: JellyfinSession? = null

        fun getInstance(context: Context): JellyfinSession =
            instance ?: synchronized(this) {
                instance ?: JellyfinSession(context).also { instance = it }
            }
    }
}
