package dev.imhamid.period.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

class Repo(private val store: SessionStore) {
    private val json = Api.json
    private val uid: String get() = store.uid ?: throw ApiException(401, "وارد نشدی")

    // ------------------------------------------------------------------ auth
    suspend fun signUp(email: String, password: String): AuthResponse {
        val body = buildJsonObject { put("email", email); put("password", password) }.toString()
        val r = json.decodeFromString<AuthResponse>(Api.raw("POST", "/auth/v1/signup", body))
        if (r.accessToken == null) throw ApiException(0, "تایید ایمیل در Supabase فعاله؛ غیرفعالش کن")
        store.save(r)
        return r
    }

    suspend fun signIn(email: String, password: String): AuthResponse {
        val body = buildJsonObject { put("email", email); put("password", password) }.toString()
        val r = json.decodeFromString<AuthResponse>(Api.raw("POST", "/auth/v1/token?grant_type=password", body))
        store.save(r)
        return r
    }

    private suspend fun refresh(): Boolean {
        val rt = store.refresh ?: return false
        return try {
            val body = buildJsonObject { put("refresh_token", rt) }.toString()
            val r = json.decodeFromString<AuthResponse>(Api.raw("POST", "/auth/v1/token?grant_type=refresh_token", body))
            store.save(r)
            true
        } catch (e: ApiException) {
            if (e.code in 400..403) store.clear()
            false
        }
    }

    fun signOut() = store.clear()

    /** Authenticated call with one automatic token refresh. */
    private suspend fun authed(method: String, path: String, body: String? = null, headers: Map<String, String> = emptyMap()): String {
        return try {
            Api.raw(method, path, body, store.access, headers)
        } catch (e: ApiException) {
            if ((e.code == 401 || e.code == 403) && refresh()) Api.raw(method, path, body, store.access, headers) else throw e
        }
    }

    // ------------------------------------------------------------------ profile
    suspend fun myProfile(): Profile {
        val s = authed("GET", "/rest/v1/profiles?id=eq.$uid&select=*")
        return json.decodeFromString(ListSerializer(Profile.serializer()), s).firstOrNull()
            ?: throw ApiException(404, "پروفایل پیدا نشد؛ schema.sql رو اجرا کردی؟")
    }

    suspend fun updateProfile(fields: JsonObject) {
        authed("PATCH", "/rest/v1/profiles?id=eq.$uid", fields.toString(), mapOf("Prefer" to "return=minimal"))
    }

    suspend fun updateProfile(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) =
        updateProfile(buildJsonObject(block))

    private suspend fun profilesIn(ids: Collection<String>): Map<String, Profile> {
        if (ids.isEmpty()) return emptyMap()
        val s = authed("GET", "/rest/v1/profiles?id=in.(${ids.joinToString(",")})&select=*")
        return json.decodeFromString(ListSerializer(Profile.serializer()), s).associateBy { it.id }
    }

    // ------------------------------------------------------------------ logs
    suspend fun logs(userId: String): Set<LocalDate> {
        val s = authed("GET", "/rest/v1/period_logs?user_id=eq.$userId&select=day&order=day.asc&limit=3000")
        return json.decodeFromString(ListSerializer(LogRow.serializer()), s).map { LocalDate.parse(it.day) }.toSet()
    }

    suspend fun setDay(day: LocalDate, on: Boolean) {
        if (on) {
            val body = buildJsonObject { put("user_id", uid); put("day", day.toString()) }.toString()
            authed("POST", "/rest/v1/period_logs?on_conflict=user_id,day", body,
                mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal"))
        } else {
            authed("DELETE", "/rest/v1/period_logs?user_id=eq.$uid&day=eq.$day")
        }
    }

    suspend fun addDays(days: List<LocalDate>) {
        if (days.isEmpty()) return
        val arr = days.joinToString(",", "[", "]") { """{"user_id":"$uid","day":"$it"}""" }
        authed("POST", "/rest/v1/period_logs?on_conflict=user_id,day", arr,
            mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal"))
    }

    // ------------------------------------------------------------------ pairing
    suspend fun pairs(): List<PairRow> {
        val s = authed("GET", "/rest/v1/pairs?select=*&or=(her_id.eq.$uid,partner_id.eq.$uid)")
        return json.decodeFromString(ListSerializer(PairRow.serializer()), s)
    }

    suspend fun requestPair(code: String) {
        val body = buildJsonObject { put("p_code", code.trim().uppercase()) }.toString()
        authed("POST", "/rest/v1/rpc/request_pair", body)
    }

    suspend fun approve(pairId: String) {
        authed("PATCH", "/rest/v1/pairs?id=eq.$pairId", """{"status":"active"}""", mapOf("Prefer" to "return=minimal"))
    }

    suspend fun setPairLabel(pairId: String, label: String) {
        val b = buildJsonObject { put("label", label) }.toString()
        authed("PATCH", "/rest/v1/pairs?id=eq.$pairId", b, mapOf("Prefer" to "return=minimal"))
    }

    suspend fun removePair(pairId: String) {
        authed("DELETE", "/rest/v1/pairs?id=eq.$pairId")
    }

    // ------------------------------------------------------------------ mood / notes / messages
    suspend fun dayLogs(userId: String): Map<LocalDate, DayLog> {
        val s = authed("GET", "/rest/v1/daily_logs?user_id=eq.$userId&select=*&order=day.desc&limit=400")
        return json.decodeFromString(ListSerializer(DayLog.serializer()), s).associateBy { LocalDate.parse(it.day) }
    }

    suspend fun notes(userId: String): Map<LocalDate, Note> {
        val s = authed("GET", "/rest/v1/notes?user_id=eq.$userId&select=*&order=day.desc&limit=200")
        return json.decodeFromString(ListSerializer(Note.serializer()), s).associateBy { LocalDate.parse(it.day) }
    }

    suspend fun messages(): List<Message> {
        val s = authed("GET", "/rest/v1/messages?select=*&order=created_at.desc&limit=30")
        return json.decodeFromString(ListSerializer(Message.serializer()), s)
    }

    suspend fun upsertDayLog(d: DayLog) {
        authed("POST", "/rest/v1/daily_logs?on_conflict=user_id,day", json.encodeToString(DayLog.serializer(), d),
            mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
    }

    suspend fun upsertNote(n: Note) {
        authed("POST", "/rest/v1/notes?on_conflict=user_id,day", json.encodeToString(Note.serializer(), n),
            mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
    }

    suspend fun deleteNote(day: LocalDate) {
        authed("DELETE", "/rest/v1/notes?user_id=eq.$uid&day=eq.$day")
    }

    suspend fun sendMessage(pairId: String, toId: String, body: String) {
        val b = buildJsonObject {
            put("pair_id", pairId); put("from_id", uid); put("to_id", toId); put("kind", "quick"); put("body", body)
        }.toString()
        authed("POST", "/rest/v1/messages", b, mapOf("Prefer" to "return=minimal"))
    }

    suspend fun markRead(ids: List<String>) {
        if (ids.isEmpty()) return
        val b = buildJsonObject { put("read_at", java.time.Instant.now().toString()) }.toString()
        authed("PATCH", "/rest/v1/messages?id=in.(${ids.joinToString(",")})", b, mapOf("Prefer" to "return=minimal"))
    }

    // ------------------------------------------------------------------ snapshot (shared by UI + worker)
    suspend fun snapshot(): Snapshot {
        val me = myProfile()
        val pairs = pairs()
        val ids = pairs.map { if (me.role == "her") it.partnerId else it.herId }.distinct()
        val others = profilesIn(ids)
        return when (me.role) {
            "her" -> Snapshot(me, pairs, others, me, logs(me.id)).extras()
            "partner" -> {
                val active = pairs.firstOrNull { it.status == "active" }
                val owner = active?.let { others[it.herId] }
                val logs = if (owner != null) runCatching { logs(owner.id) }.getOrDefault(emptySet()) else emptySet()
                Snapshot(me, pairs, others, owner, logs).extras()
            }
            else -> Snapshot(me, pairs, others, null, emptySet())
        }
    }

    /** Optional data (tolerates a missing migration). */
    private suspend fun Snapshot.extras(): Snapshot {
        val oid = owner?.id
        val d = if (oid != null) runCatching { dayLogs(oid) }.getOrDefault(emptyMap()) else emptyMap()
        val n = if (oid != null) runCatching { notes(oid) }.getOrDefault(emptyMap()) else emptyMap()
        val m = runCatching { messages() }.getOrDefault(emptyList())
        return with(days = d, notes = n, inbox = m)
    }
}
