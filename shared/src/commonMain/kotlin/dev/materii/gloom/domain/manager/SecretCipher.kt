package dev.materii.gloom.domain.manager

/** Encrypts small secrets (account tokens) before they are written to local storage. */
interface SecretCipher {
    fun encrypt(plain: String): String

    /** Throws if [encoded] can't be decrypted, e.g. because the key no longer exists. */
    fun decrypt(encoded: String): String
}
