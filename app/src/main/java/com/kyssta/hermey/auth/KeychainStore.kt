package com.kyssta.hermey.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.spec.SecretKeySpec

class KeychainStore(@ApplicationContext private val context: Context) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "hermex_"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    fun save(value: String, key: String, scope: String? = null): Result<Unit> {
        try {
            val alias = "${KEY_ALIAS_PREFIX}${scope?.hashCode() ?: 0}_${key.hashCode()}"
            val encrypted = encrypt(value.toByteArray(), alias)
            val prefs = context.getSharedPreferences("hermex_keys", Context.MODE_PRIVATE)
            prefs.edit().putString(alias, encrypted)?.apply()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun load(key: String, scope: String? = null): Result<String?> {
        try {
            val alias = "${KEY_ALIAS_PREFIX}${scope?.hashCode() ?: 0}_${key.hashCode()}"
            val prefs = context.getSharedPreferences("hermex_keys", Context.MODE_PRIVATE)
            val encrypted = prefs.getString(alias, null) ?: return Result.success(null)
            val decrypted = decrypt(encrypted, alias)
            return Result.success(String(decrypted))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun delete(key: String, scope: String? = null): Result<Unit> {
        try {
            val alias = "${KEY_ALIAS_PREFIX}${scope?.hashCode() ?: 0}_${key.hashCode()}"
            val prefs = context.getSharedPreferences("hermex_keys", Context.MODE_PRIVATE)
            prefs.edit().remove(alias)?.apply()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun generateKey(alias: String): SecretKey {
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGen.generateKey()
    }

    private fun encrypt(plaintext: ByteArray, alias: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, generateKey(alias))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return "${android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT)}:${android.util.Base64.encodeToString(ciphertext, android.util.Base64.DEFAULT)}"
    }

    private fun decrypt(encrypted: String, alias: String): ByteArray {
        val parts = encrypted.split(":", limit = 2)
        val iv = android.util.Base64.decode(parts[0], android.util.Base64.DEFAULT)
        val ciphertext = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, generateKey(alias), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }
}
