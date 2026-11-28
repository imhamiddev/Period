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
    val regularity: Int = 0,
    val hormonal: Boolean = false,
)

/** Only she can read this (private_profile table). */
@Serializable
data class Body(
    @SerialName("user_id") val userId: String,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    val gender: String = "female",
    val phone: String? = null,
    @SerialName("water_goal_ml") val waterGoalMl: Int = 2400,
    @SerialName("cup_ml") val cupMl: Int = 200,
)

/** Medicines, note, weight, temperature and water for one day. Only she can read this (health_logs table). */
@Serializable
data class HealthLog(
    @SerialName("user_id") val userId: String,
    val day: String,
    val medicines: List<String> = emptyList(),
    val note: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("temperature_c") val temperatureC: Double? = null,
    @SerialName("water_ml") val waterMl: Int? = null,
)

@Serializable
data class PairRow(
    val id: String,
    @SerialName("her_id") val herId: String,
    @SerialName("partner_id") val partnerId: String,
    val status: String,
    /** Role she gave this connection (partner, mother, sister…). */
    val label: String? = null,
)

@Serializable
data class LogRow(val day: String)

@Serializable
data class IdRow(val id: String)

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
    val moods: List<String> = emptyList(),
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
    val inbox: List<Message> = emptyList(),
    val body: Body? = null,
    val health: Map<java.time.LocalDate, HealthLog> = emptyMap(),
) {
    fun with(
        logs: Set<java.time.LocalDate> = this.logs,
        days: Map<java.time.LocalDate, DayLog> = this.days,
        inbox: List<Message> = this.inbox,
        body: Body? = this.body,
        health: Map<java.time.LocalDate, HealthLog> = this.health,
    ) = Snapshot(me, pairs, others, owner, logs, days, inbox, body, health)

    fun withMe(m: Profile) = Snapshot(m, pairs, others, if (owner?.id == m.id) m else owner, logs, days, inbox, body, health)

    val activePair: PairRow? get() = pairs.firstOrNull { it.status == "active" }
}
