package dev.imhamid.period.data

import android.content.Context

class SessionStore(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("session", Context.MODE_PRIVATE)

    var access: String?
        get() = p.getString("access", null)
        set(v) = p.edit().putString("access", v).apply()
    var refresh: String?
        get() = p.getString("refresh", null)
        set(v) = p.edit().putString("refresh", v).apply()
    var uid: String?
        get() = p.getString("uid", null)
        set(v) = p.edit().putString("uid", v).apply()
    var introSeen: Boolean
        get() = p.getBoolean("intro", false)
        set(v) = p.edit().putBoolean("intro", v).apply()

    var lockEnabled: Boolean
        get() = p.getBoolean("lock", false)
        set(v) = p.edit().putBoolean("lock", v).apply()

    /** 0 = follow system, 1 = light, 2 = dark. */
    var themeMode: Int
        get() = p.getInt("theme", 0)
        set(v) = p.edit().putInt("theme", v).apply()

    var accent: Int
        get() = p.getInt("accent", 0)
        set(v) = p.edit().putInt("accent", v).apply()

    /** Instant message alerts via a small foreground service (device-local). */
    var liveNotify: Boolean
        get() = p.getBoolean("live", false)
        set(v) = p.edit().putBoolean("live", v).apply()

    /** Labels (mother, sister, …) she gave her connections; kept here as a fallback to the server value. */
    var pairLabels: Map<String, String>
        get() = (p.getString("pair_labels", "") ?: "").split('\u001F').mapNotNull {
            val i = it.indexOf('=')
            if (i > 0) it.substring(0, i) to it.substring(i + 1) else null
        }.toMap()
        set(v) = p.edit().putString("pair_labels", v.entries.joinToString("\u001F") { "${it.key}=${it.value}" }).apply()

    /** User-written quick messages (kept on this device). */
    var savedMessages: List<String>
        get() = (p.getString("saved_msgs", "") ?: "").split('\u001F').filter { it.isNotBlank() }
        set(v) = p.edit().putString("saved_msgs", v.joinToString("\u001F")).apply()

    val signedIn: Boolean get() = !refresh.isNullOrEmpty() && !uid.isNullOrEmpty()

    fun save(r: AuthResponse) {
        p.edit()
            .putString("access", r.accessToken)
            .putString("refresh", r.refreshToken)
            .putString("uid", r.user?.id)
            .apply()
    }

    fun clear() {
        val intro = introSeen
        val lock = lockEnabled
        val theme = themeMode
        val acc = accent
        p.edit().clear().putBoolean("intro", intro).putBoolean("lock", lock).putInt("theme", theme).putInt("accent", acc).apply()
    }

    // ---- notification de-duplication
    fun wasSent(key: String) = p.getStringSet("sent", emptySet())!!.contains(key)
    fun markSent(key: String) {
        val s = p.getStringSet("sent", emptySet())!!.toMutableSet()
        s.add(key)
        // keep it small
        val trimmed = if (s.size > 200) s.toList().takeLast(100).toMutableSet() else s
        p.edit().putStringSet("sent", trimmed).apply()
    }
}
