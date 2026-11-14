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
        p.edit().clear().putBoolean("intro", intro).putBoolean("lock", lock).apply()
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
