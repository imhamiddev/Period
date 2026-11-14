package dev.imhamid.period.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    @SerialName("pair_code") val pairCode: String? = null,
    @SerialName("cycle_length") val cycleLength: Int = 28,
    @SerialName("period_length") val periodLength: Int = 5,
    @SerialName("luteal_length") val lutealLength: Int = 14,
    @SerialName("pms_days") val pmsDays: Int = 5,
    @SerialName("share_enabled") val shareEnabled: Boolean = true,
    @SerialName("notify_enabled") val notifyEnabled: Boolean = true,
    val onboarded: Boolean = false,
    @SerialName("notify_hour") val notifyHour: Int = 9,
    @SerialName("notify_pms") val notifyPms: Boolean = true,
    @SerialName("notify_period") val notifyPeriod: Boolean = true,
    @SerialName("notify_ovulation") val notifyOvulation: Boolean = false,
    @SerialName("notify_late") val notifyLate: Boolean = true,
)

@Serializable
data class PairRow(
    val id: String,
    @SerialName("her_id") val herId: String,
    @SerialName("partner_id") val partnerId: String,
    val status: String,
)

@Serializable
data class LogRow(val day: String)

@Serializable
data class AuthUser(val id: String, val email: String? = null)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: AuthUser? = null,
)

@Serializable
data class DayLog(
    @SerialName("user_id") val userId: String,
    val day: String,
    val mood: Int? = null,
    val energy: Int? = null,
    val symptoms: List<String> = emptyList(),
)

@Serializable
data class Note(
    @SerialName("user_id") val userId: String,
    val day: String,
    val body: String,
    val shared: Boolean = false,
)

@Serializable
data class Message(
    val id: String,
    @SerialName("from_id") val fromId: String,
    @SerialName("to_id") val toId: String,
    val kind: String = "quick",
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null,
)

class Snapshot(
    val me: Profile,
    val pairs: List<PairRow>,
    val others: Map<String, Profile>,
    /** The person whose cycle is tracked: me (role=her) or my active partner. */
    val owner: Profile?,
    val logs: Set<java.time.LocalDate>,
    val days: Map<java.time.LocalDate, DayLog> = emptyMap(),
    val notes: Map<java.time.LocalDate, Note> = emptyMap(),
    val inbox: List<Message> = emptyList(),
) {
    fun with(
        logs: Set<java.time.LocalDate> = this.logs,
        days: Map<java.time.LocalDate, DayLog> = this.days,
        notes: Map<java.time.LocalDate, Note> = this.notes,
        inbox: List<Message> = this.inbox,
    ) = Snapshot(me, pairs, others, owner, logs, days, notes, inbox)

    val activePair: PairRow? get() = pairs.firstOrNull { it.status == "active" }
}
