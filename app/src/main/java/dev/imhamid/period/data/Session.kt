package dev.imhamid.period.data

import android.content.Context

/**
 * Everything the app keeps on the phone. Anything personal (tokens, the offline copy of her cycle,
 * the queue of unsent changes, saved messages) is encrypted with [SecureBox]; plain settings (theme, lock
 * switch, …) stay as ordinary preferences.
 */
class SessionStore(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("session", Context.MODE_PRIVATE)

    // ---- encrypted values (with transparent migration from the old plaintext format)
    private fun getSecure(key: String): String? {
        val raw = p.getString(key, null) ?: return null
        val plain = SecureBox.decrypt(raw) ?: return null
        if (!SecureBox.isEncrypted(raw)) setSecure(key, plain) // legacy plaintext -> encrypt now
        return plain
    }

    private fun setSecure(key: String, v: String?) {
        val enc = if (v == null) null else SecureBox.encrypt(v)
        // if the Keystore is unusable we store nothing rather than falling back to plaintext
        p.edit().apply { if (enc == null) remove(key) else putString(key, enc) }.apply()
    }

    var access: String?
        get() = getSecure("access")
        set(v) = setSecure("access", v)
    var refresh: String?
        get() = getSecure("refresh")
        set(v) = setSecure("refresh", v)
    var uid: String?
        get() = p.getString("uid", null)
        set(v) = p.edit().putString("uid", v).apply()
    /** Own e-mail, shown in Settings (it is no longer kept in the shared profiles table). */
    var email: String?
        get() = getSecure("email")
        set(v) = setSecure("email", v)
    var introSeen: Boolean
        get() = p.getBoolean("intro", false)
        set(v) = p.edit().putBoolean("intro", v).apply()

    var lockEnabled: Boolean
        get() = p.getBoolean("lock", false)
        set(v) = p.edit().putBoolean("lock", v).apply()

    /** Offline cache of the last snapshot and the queue of unsent changes (wiped on sign-out). */
    var cacheJson: String?
        get() = getSecure("cache")
        set(v) = setSecure("cache", v)

    var queueJson: String
        get() = getSecure("queue") ?: "[]"
        set(v) = setSecure("queue", v)

    /** 0 = follow system, 1 = light, 2 = dark. */
    var themeMode: Int
        get() = p.getInt("theme", 0)
        set(v) = p.edit().putInt("theme", v).apply()

    var accent: Int
        get() = p.getInt("accent", 0)
        set(v) = p.edit().putInt("accent", v).apply()

    /** Labels (mother, sister, …) she gave her connections; kept here as a fallback to the server value. */
    var pairLabels: Map<String, String>
        get() = (getSecure("pair_labels") ?: "").split('\u001F').mapNotNull {
            val i = it.indexOf('=')
            if (i > 0) it.substring(0, i) to it.substring(i + 1) else null
        }.toMap()
        set(v) = setSecure("pair_labels", v.entries.joinToString("\u001F") { "${it.key}=${it.value}" })

    /** User-written quick messages (kept on this device). */
    var savedMessages: List<String>
        get() = (getSecure("saved_msgs") ?: "").split('\u001F').filter { it.isNotBlank() }
        set(v) = setSecure("saved_msgs", v.joinToString("\u001F"))

    val signedIn: Boolean get() = !refresh.isNullOrEmpty() && !uid.isNullOrEmpty()

    fun save(r: AuthResponse) {
        val newUid = r.user?.id
        val oldUid = uid
        // a different account signed in: the previous account's cache and unsent changes must not leak over
        if (newUid != null && oldUid != null && newUid != oldUid) {
            cacheJson = null
            queueJson = "[]"
        }
        access = r.accessToken
        refresh = r.refreshToken
        if (newUid != null) uid = newUid
        r.user?.email?.takeIf { it.isNotBlank() }?.let { email = it }
    }

    /**
     * The server rejected our refresh token for good: sign in again, but keep the offline copy and the queue of
     * unsent changes so nothing she logged is lost (they are sent after the next sign-in of the same account).
     */
    fun invalidateSession() {
        access = null
        refresh = null
    }

    /** Full sign-out: wipes everything personal; keeps only the plain app settings. */
    fun clear() {
        val intro = introSeen
        val lock = lockEnabled
        val theme = themeMode
        val acc = accent
        p.edit().clear().putBoolean("intro", intro).putBoolean("lock", lock).putInt("theme", theme).putInt("accent", acc).apply()
    }

    // ---- notification de-duplication (ordered list, oldest dropped first)
    private fun sentList(): List<String> =
        (p.getString("sent_list", "") ?: "").split('\n').filter { it.isNotEmpty() }

    fun wasSent(key: String): Boolean = synchronized(SENT_LOCK) { key in sentList() }

    fun markSent(key: String) {
        synchronized(SENT_LOCK) { addSent(key) }
    }

    /** Atomically marks [key] as sent; true only for the caller that marked it (so two threads never both notify). */
    fun claim(key: String): Boolean = synchronized(SENT_LOCK) {
        if (key in sentList()) false else { addSent(key); true }
    }

    private fun addSent(key: String) {
        val l = sentList()
        if (key in l) return
        p.edit().putString("sent_list", (l + key).takeLast(200).joinToString("\n")).apply()
    }

    private companion object {
        val SENT_LOCK = Any()
    }
}
