package com.instarecipe.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurePreferences {
    private const val AndroidKeyStore = "AndroidKeyStore"
    private const val KeyAlias = "instarecipe_local_secrets_v1"
    private const val Preferences = "instarecipe_secure_values"
    private const val GeminiPrimaryKey = "gemini_api_key_encrypted"
    private const val GeminiBackupKey = "gemini_backup_api_key_encrypted"

    fun getGeminiApiKey(context: Context): String = getEncryptedValue(context, GeminiPrimaryKey)

    fun getGeminiBackupApiKey(context: Context): String = getEncryptedValue(context, GeminiBackupKey)

    fun setGeminiApiKey(context: Context, value: String): Boolean =
        setGeminiApiKeys(context, value, getGeminiBackupApiKey(context))

    fun setGeminiApiKeys(context: Context, primary: String, backup: String): Boolean = runCatching {
        val editor = context.getSharedPreferences(Preferences, Context.MODE_PRIVATE).edit()
        editor.putEncryptedOrRemove(GeminiPrimaryKey, primary)
        editor.putEncryptedOrRemove(GeminiBackupKey, backup)
        editor.commit()
    }.getOrDefault(false)

    private fun getEncryptedValue(context: Context, key: String): String {
        val encoded = context.getSharedPreferences(Preferences, Context.MODE_PRIVATE)
            .getString(key, null)
            ?: return ""
        return runCatching { decrypt(encoded) }.getOrDefault("")
    }

    private fun android.content.SharedPreferences.Editor.putEncryptedOrRemove(
        key: String,
        value: String
    ) {
        if (value.isBlank()) remove(key) else putString(key, encrypt(value.trim()))
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        return "$iv:$encrypted"
    }

    private fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (keyStore.getKey(KeyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        generator.init(
            KeyGenParameterSpec.Builder(
                KeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}