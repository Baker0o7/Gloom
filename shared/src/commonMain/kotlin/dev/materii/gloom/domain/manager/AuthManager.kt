package dev.materii.gloom.domain.manager

import androidx.compose.runtime.*
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.apolloStore
import dev.materii.gloom.util.Logger
import dev.materii.gloom.util.SettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class AuthManager(
    settingsProvider: SettingsProvider,
    private val apollo: ApolloClient,
    private val json: Json,
    private val logger: Logger,
    private val cipher: SecretCipher? = null
) {

    private val settings = settingsProvider.createSettings()

    // ID to account
    val accounts = mutableStateMapOf<String, Account>()

    init {
        settings.remove("authToken")
        for (key in settings.keys.toList()) {
            if (key == "logged_in_id") continue
            try {
                val raw = settings.getString(key, "")
                val acct = json.decodeFromString<Account>(readStored(raw))
                accounts[key] = acct
                // Older installs stored the token in plain text: encrypt it on first read
                if (cipher != null && !raw.startsWith(ENCRYPTED_PREFIX)) store(acct)
            } catch (e: Exception) {
                logger.error("AuthManager", "Couldn't read stored account", e)
            }
        }
    }

    private fun writeStored(plain: String): String {
        val c = cipher ?: return plain
        return runCatching { ENCRYPTED_PREFIX + c.encrypt(plain) }.getOrDefault(plain)
    }

    private fun readStored(raw: String): String {
        if (!raw.startsWith(ENCRYPTED_PREFIX)) return raw
        val c = cipher ?: error("Stored account is encrypted but no cipher is available")
        return c.decrypt(raw.removePrefix(ENCRYPTED_PREFIX))
    }

    private fun store(account: Account) {
        settings.putString(account.id, writeStored(json.encodeToString(account)))
    }

    private var currentAccountId: String
        get() = settings.getString("logged_in_id", "")
        set(value) = settings.putString("logged_in_id", value)

    val currentAccount: Account?
        get() {
            if (!isSignedIn) return null
            return accounts[currentAccountId]
        }

    val authToken: String
        get() = currentAccount?.token ?: ""

    val isSignedIn: Boolean get() = currentAccountId.isNotBlank() && awaitingAuthType == null

    var awaitingAuthType: Account.Type? = null
        private set

    var loading by mutableStateOf(false)
        private set

    fun setAuthState(authType: Account.Type? = awaitingAuthType, loading: Boolean = this.loading) {
        awaitingAuthType = authType
        this.loading = loading
    }

    fun addAccount(
        id: String,
        token: String,
        type: Account.Type = Account.Type.REGULAR,
        baseUrl: String? = null,
        avatarUrl: String,
        username: String,
        displayName: String?,
        notificationCount: Int = 0
    ) {
        Account(id, token, type, baseUrl, avatarUrl, username, displayName, notificationCount).let {
            accounts[id] = it
            store(it)
        }
    }

    fun editAccount(
        id: String,
        token: String? = null,
        type: Account.Type? = null,
        baseUrl: String? = null,
        avatarUrl: String? = null,
        username: String? = null,
        displayName: String? = null,
        notificationCount: Int? = null
    ) {
        val existingAccount = accounts[id] ?: return
        editAccount(
            existingAccount,
            token ?: existingAccount.token,
            type ?: existingAccount.type,
            baseUrl ?: existingAccount.baseUrl,
            avatarUrl ?: existingAccount.avatarUrl,
            username ?: existingAccount.username,
            displayName ?: existingAccount.displayName,
            notificationCount ?: existingAccount.notificationCount
        )
    }

    private fun editAccount(
        account: Account,
        token: String = account.token,
        type: Account.Type = account.type,
        baseUrl: String? = account.baseUrl,
        avatarUrl: String = account.avatarUrl,
        username: String = account.username,
        displayName: String? = account.displayName,
        notificationCount: Int = account.notificationCount
    ) {
        accounts[account.id]?.let {
            accounts[account.id] = account.copy(
                token = token,
                type = type,
                baseUrl = baseUrl,
                avatarUrl = avatarUrl,
                username = username,
                displayName = displayName,
                notificationCount = notificationCount
            )
            accounts[account.id]?.let { updated -> store(updated) }
        }
    }

    fun removeAccount(id: String) {
        if (id == currentAccountId) currentAccountId = ""
        accounts.remove(id)
        settings.remove(id)
    }

    fun switchToAccount(id: String) {
        if (accounts.containsKey(id)) currentAccountId = id
        clearApolloCache()
    }

    fun clearApolloCache() {
        apollo.apolloStore.clearAll()
    }

    private companion object {
        const val ENCRYPTED_PREFIX = "enc1:"
    }

}

@Stable
@Serializable
data class Account(
    val id: String,
    val token: String,
    val type: Type,
    val baseUrl: String?,
    val avatarUrl: String,
    val username: String,
    val displayName: String?,
    val notificationCount: Int = 0
) {

    @Serializable
    enum class Type {

        REGULAR,
        ENTERPRISE
    }

}