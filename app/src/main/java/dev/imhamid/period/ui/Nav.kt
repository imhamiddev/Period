package dev.imhamid.period.ui

import java.time.LocalDate

/** Pages that open on top of the five tabs. */
sealed class Screen {
    data class PhaseInfo(val index: Int) : Screen()
    data object EditPeriod : Screen()
    data object AddLog : Screen()
    data object LogWater : Screen()
    data object LogWeight : Screen()
    data object LogTemp : Screen()
    data object MyCycles : Screen()
    data class Article(val id: String) : Screen()
    data class ArticleList(val category: String?) : Screen()
    data object Partner : Screen()
    data object PersonalInfo : Screen()
    data object Preferences : Screen()
    data object CycleSettings : Screen()
    data object Reminder : Screen()
    data object Notifications : Screen()
    data object Security : Screen()
    data object ChangePassword : Screen()
    data object Linked : Screen()
    data object Billing : Screen()
    data object DataAnalytics : Screen()
    data object PaymentMethods : Screen()
    data object AddPayment : Screen()
    data object Appearance : Screen()
    data object ThemeScreen : Screen()
    data object Language : Screen()
    data object Help : Screen()
    data object Faq : Screen()
    data object Contact : Screen()
    data object Privacy : Screen()
    data object Terms : Screen()
    data object Upgrade : Screen()
    data object ChoosePay : Screen()
    data object Review : Screen()
    data object Processing : Screen()
    data object PaySuccess : Screen()
}

/** What the "Add log" form is currently editing. */
data class LogDraft(
    val day: LocalDate,
    val flow: String? = null,
    val symptoms: Set<String> = emptySet(),
    val moods: Set<String> = emptySet(),
    val medicines: Set<String> = emptySet(),
    val note: String = "",
    val weight: Double? = null,
    val temp: Double? = null,
    val water: Int? = null,
    val energy: Int? = null,
    /** Old 1..5 mood from before the 12-mood picker; kept when no new mood is chosen. */
    val legacyMood: Int? = null,
)

enum class AuthMode { SignIn, SignUp, Forgot, Otp, NewPass, Updated }
