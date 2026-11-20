package dev.imhamid.period.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts small strings (tokens, the offline copy, notes) with AES-256-GCM using a key that never leaves
 * the Android Keystore. Stored form: `enc1:` + Base64(iv ‖ ciphertext+tag).
 *
 * Values that don't start with the prefix are treated as legacy plaintext (written by older versions) so
 * the store can migrate them on first read.
 */
object SecureBox {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "luna_store_key_v1"
    private const val PREFIX = "enc1:"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    private val lock = Any()

    fun isEncrypted(s: String) = s.startsWith(PREFIX)

    private fun key(): SecretKey = synchronized(lock) {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        gen.generateKey()
    }

    /** @return the encrypted form, or null if the Keystore is unusable (callers then store nothing). */
    fun encrypt(plain: String): String? = try {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key()) // the Keystore picks a fresh random IV
        val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        PREFIX + Base64.encodeToString(c.iv + ct, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }

    /**
     * @return the plain text; legacy (unprefixed) values are returned as they are;
     * null if the value is corrupt or its key is gone (e.g. after a device restore).
     */
    fun decrypt(stored: String): String? {
        if (!isEncrypted(stored)) return stored
        return try {
            val raw = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            if (raw.size <= IV_BYTES) return null
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, raw.copyOfRange(0, IV_BYTES)))
            String(c.doFinal(raw.copyOfRange(IV_BYTES, raw.size)), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
