package dev.imhamid.period.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

/** Last known state, kept on the phone so the app opens and works without internet. */
@Serializable
data class CacheDto(
    val me: Profile,
    val pairs: List<PairRow>,
    val others: List<Profile>,
    val ownerId: String?,
    val logs: List<String>,
    val days: List<DayLog>,
    val inbox: List<Message>,
)

object Offline {
    fun save(store: SessionStore, s: Snapshot) {
        runCatching {
            val dto = CacheDto(
                s.me, s.pairs, s.others.values.toList(), s.owner?.id,
                s.logs.map { it.toString() }, s.days.values.toList(), s.inbox,
            )
            val str = Api.json.encodeToString(CacheDto.serializer(), dto)
            if (str != store.cacheJson) store.cacheJson = str
        }
    }

    fun load(store: SessionStore): Snapshot? {
        val raw = store.cacheJson ?: return null
        return try {
            val d = Api.json.decodeFromString(CacheDto.serializer(), raw)
            if (d.me.id != store.uid) {
                null
            } else {
                val others = d.others.associateBy { it.id }
                val oid = d.ownerId
                val owner = if (oid == null) null else if (oid == d.me.id) d.me else others[oid]
                Snapshot(
                    d.me, d.pairs, others, owner,
                    d.logs.map { LocalDate.parse(it) }.toSet(),
                    d.days.associateBy { LocalDate.parse(it.day) },
                    d.inbox,
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

/** A change made while offline (or not yet confirmed by the server). */
@Serializable
data class PendingOp(
    val type: String, // day | days | daylog | profile | msg
    val day: String? = null,
    val on: Boolean = false,
    val days: List<String> = emptyList(),
    val payload: String? = null,
    val pairId: String? = null,
    val toId: String? = null,
) {
    /** Newer ops with the same key replace older ones. */
    val key: String?
        get() = when (type) {
            "day" -> "day:$day"
            "daylog" -> "daylog:$day"
            else -> null
        }
}

class FlushResult(
    /** What stopped the queue (offline = code -1, or an auth problem); the rest stays queued. */
    val blocked: ApiException?,
    /** An operation the server rejected for good; it was dropped. */
    val dropped: ApiException?,
)

object PendingQueue {
    private val flushLock = Mutex()
    private val mem = Any()
    private val ser = ListSerializer(PendingOp.serializer())

    fun read(store: SessionStore): List<PendingOp> =
        runCatching { Api.json.decodeFromString(ser, store.queueJson) }.getOrDefault(emptyList())

    private fun write(store: SessionStore, list: List<PendingOp>) {
        store.queueJson = Api.json.encodeToString(ser, list)
    }

    fun add(store: SessionStore, op: PendingOp) = synchronized(mem) {
        val cur = read(store)
        val kept = if (op.key != null) cur.filterNot { it.key == op.key } else cur
        write(store, kept + op)
    }

    suspend fun flush(store: SessionStore, exec: suspend (PendingOp) -> Unit): FlushResult = flushLock.withLock {
        var dropped: ApiException? = null
        while (true) {
            val op = synchronized(mem) { read(store).firstOrNull() } ?: break
            try {
                exec(op)
            } catch (e: ApiException) {
                if (e.code == -1 || e.code == 401) return@withLock FlushResult(e, dropped)
                dropped = e // rejected for good (e.g. missing column): do not block the rest
            }
            synchronized(mem) {
                val cur = read(store).toMutableList()
                cur.remove(op)
                write(store, cur)
            }
        }
        FlushResult(null, dropped)
    }
}

/** Applies a pending profile PATCH to the local copy so the UI updates instantly. */
fun Profile.patched(o: JsonObject): Profile {
    fun i(k: String) = o[k]?.jsonPrimitive?.intOrNull
    fun b(k: String) = o[k]?.jsonPrimitive?.booleanOrNull
    fun s(k: String) = o[k]?.jsonPrimitive?.contentOrNull
    return copy(
        name = s("name") ?: name,
        role = s("role") ?: role,
        cycleLength = i("cycle_length") ?: cycleLength,
        periodLength = i("period_length") ?: periodLength,
        lutealLength = i("luteal_length") ?: lutealLength,
        pmsDays = i("pms_days") ?: pmsDays,
        regularity = i("regularity") ?: regularity,
        hormonal = b("hormonal") ?: hormonal,
        onboarded = b("onboarded") ?: onboarded,
        shareEnabled = b("share_enabled") ?: shareEnabled,
        notifyEnabled = b("notify_enabled") ?: notifyEnabled,
        notifyHour = i("notify_hour") ?: notifyHour,
        notifyPms = b("notify_pms") ?: notifyPms,
        notifyPeriod = b("notify_period") ?: notifyPeriod,
        notifyOvulation = b("notify_ovulation") ?: notifyOvulation,
        notifyLate = b("notify_late") ?: notifyLate,
    )
}
