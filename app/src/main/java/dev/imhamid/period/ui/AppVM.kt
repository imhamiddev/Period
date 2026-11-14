package dev.imhamid.period.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.imhamid.period.data.Api
import dev.imhamid.period.data.ApiException
import dev.imhamid.period.data.Repo
import dev.imhamid.period.data.SessionStore
import dev.imhamid.period.data.DayLog
import dev.imhamid.period.data.Note
import dev.imhamid.period.data.Snapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import dev.imhamid.period.notify.SyncWorker
import dev.imhamid.period.update.UpdateState
import dev.imhamid.period.update.Updater
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put
import java.time.LocalDate

enum class Route { Boot, Intro, Auth, Name, Role, Setup, Home }

class AppVM(private val app: Application) : AndroidViewModel(app) {
    val store = SessionStore(app)
    private val repo = Repo(store)

    var booting by mutableStateOf(true)
    var signedIn by mutableStateOf(store.signedIn)
    var introSeen by mutableStateOf(store.introSeen)
    var snap by mutableStateOf<Snapshot?>(null)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var update by mutableStateOf<UpdateState>(UpdateState.None)
    var lockEnabled by mutableStateOf(store.lockEnabled)

    private val dayJobs = mutableMapOf<LocalDate, Job>()
    private val noteJobs = mutableMapOf<LocalDate, Job>()

    val route: Route
        get() {
            if (booting) return Route.Boot
            if (!signedIn) return if (!introSeen) Route.Intro else Route.Auth
            val s = snap ?: return Route.Boot
            val me = s.me
            return when {
                me.name.isNullOrBlank() -> Route.Name
                me.role == null -> Route.Role
                me.role == "her" && !me.onboarded -> Route.Setup
                else -> Route.Home
            }
        }

    init {
        viewModelScope.launch {
            if (signedIn) {
                load()
                WorkManager.getInstance(app).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
            }
            booting = false
            checkUpdate()
        }
    }

    private fun fail(e: Exception) {
        if (e is ApiException) {
            if (e.code == 401 && !store.signedIn) logoutLocal() else error = e.message
        } else error = e.message ?: "خطای ناشناخته"
    }

    private fun logoutLocal() {
        snap = null
        signedIn = false
    }

    suspend fun load() {
        try {
            snap = repo.snapshot()
        } catch (e: Exception) {
            fail(e)
        }
    }

    fun refresh() = viewModelScope.launch { load() }

    // ------------------------------------------------------------------ intro / auth
    fun finishIntro() {
        store.introSeen = true
        introSeen = true
    }

    fun submitAuth(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { error = "ایمیل و رمز رو وارد کن"; return }
        viewModelScope.launch {
            busy = true
            try {
                try {
                    repo.signIn(email.trim(), password)
                } catch (e: ApiException) {
                    if (e.message != dev.imhamid.period.data.ERR_INVALID) throw e
                    // unknown email -> create the account; known email -> it was a wrong password
                    try {
                        repo.signUp(email.trim(), password)
                    } catch (e2: ApiException) {
                        if (e2.message == dev.imhamid.period.data.ERR_EXISTS) throw ApiException(400, "رمز عبور اشتباهه") else throw e2
                    }
                }
                signedIn = true
                load()
                WorkManager.getInstance(app).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
            } catch (e: Exception) {
                fail(e)
            } finally {
                busy = false
            }
        }
    }

    fun logout() {
        repo.signOut()
        logoutLocal()
    }

    // ------------------------------------------------------------------ profile
    fun patch(block: JsonObjectBuilder.() -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                repo.updateProfile(block)
                load()
            } catch (e: Exception) {
                fail(e)
            } finally {
                busy = false
            }
        }
    }

    fun saveName(name: String) {
        if (name.isBlank()) { error = "اسمت رو بنویس"; return }
        patch { put("name", name.trim()) }
    }

    fun saveRole(role: String) = patch { put("role", role) }

    fun saveSetup(
        lastStart: LocalDate, earlier: List<LocalDate>, cycle: Int, period: Int,
        pms: Int, regularity: Int, hormonal: Boolean,
    ) {
        viewModelScope.launch {
            busy = true
            try {
                val today = LocalDate.now()
                val days = (earlier + lastStart)
                    .flatMap { st -> (0 until period).map { st.plusDays(it.toLong()) } }
                    .filter { !it.isAfter(today) }
                    .distinct()
                repo.addDays(days)
                repo.updateProfile {
                    put("cycle_length", cycle)
                    put("period_length", period)
                    put("pms_days", pms)
                    put("regularity", regularity)
                    put("hormonal", hormonal)
                    put("onboarded", true)
                }
                load()
                WorkManager.getInstance(app).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
            } catch (e: Exception) {
                fail(e)
            } finally {
                busy = false
            }
        }
    }

    // ------------------------------------------------------------------ logging
    fun toggleDay(d: LocalDate) {
        val s = snap ?: return
        if (s.me.role != "her" || d.isAfter(LocalDate.now())) return
        val on = d !in s.logs
        val newLogs = if (on) s.logs + d else s.logs - d
        snap = s.with(logs = newLogs)
        viewModelScope.launch {
            try {
                repo.setDay(d, on)
            } catch (e: Exception) {
                snap = s
                fail(e)
            }
        }
    }

    // ------------------------------------------------------------------ mood / symptoms / notes
    fun saveDay(day: LocalDate, mood: Int?, energy: Int?, symptoms: List<String>) {
        val s = snap ?: return
        if (s.me.role != "her") return
        val dl = DayLog(s.me.id, day.toString(), mood, energy, symptoms)
        snap = s.with(days = s.days + (day to dl))
        dayJobs[day]?.cancel()
        dayJobs[day] = viewModelScope.launch {
            delay(450)
            try { repo.upsertDayLog(dl) } catch (e: Exception) { fail(e) }
        }
    }

    fun saveNote(day: LocalDate, text: String, shared: Boolean) {
        val s = snap ?: return
        if (s.me.role != "her") return
        val body = text.trim()
        snap = if (body.isEmpty()) s.with(notes = s.notes - day) else s.with(notes = s.notes + (day to Note(s.me.id, day.toString(), body, shared)))
        noteJobs[day]?.cancel()
        noteJobs[day] = viewModelScope.launch {
            try {
                if (body.isEmpty()) repo.deleteNote(day) else repo.upsertNote(Note(s.me.id, day.toString(), body, shared))
            } catch (e: Exception) { fail(e) }
        }
    }

    // ------------------------------------------------------------------ quick messages
    fun sendMessage(body: String) {
        val s = snap ?: return
        val pair = s.activePair
        if (pair == null) { error = "هنوز به هم وصل نیستید"; return }
        val to = if (s.me.role == "her") pair.partnerId else pair.herId
        viewModelScope.launch {
            try { repo.sendMessage(pair.id, to, body); load() } catch (e: Exception) { fail(e) }
        }
    }

    fun markRead() {
        val s = snap ?: return
        val ids = s.inbox.filter { it.toId == s.me.id && it.readAt == null }.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch { runCatching { repo.markRead(ids) }; load() }
    }

    // ------------------------------------------------------------------ app lock
    fun setLock(v: Boolean) {
        store.lockEnabled = v
        lockEnabled = v
    }

    // ------------------------------------------------------------------ pairing
    fun connect(code: String) {
        if (code.length < 4) { error = "کد رو کامل وارد کن"; return }
        viewModelScope.launch {
            busy = true
            try {
                repo.requestPair(code)
                load()
            } catch (e: Exception) {
                fail(e)
            } finally {
                busy = false
            }
        }
    }

    fun approve(pairId: String) = viewModelScope.launch {
        try { repo.approve(pairId); load() } catch (e: Exception) { fail(e) }
    }

    fun removePair(pairId: String) = viewModelScope.launch {
        try { repo.removePair(pairId); load() } catch (e: Exception) { fail(e) }
    }

    // ------------------------------------------------------------------ updates
    fun checkUpdate() {
        if (update is UpdateState.Downloading) return
        viewModelScope.launch {
            val r = Updater.check()
            if (r is UpdateState.Available && update !is UpdateState.Ready) update = r
        }
    }

    fun startDownload() {
        val u = update
        val (version, url) = when (u) {
            is UpdateState.Available -> u.version to u.url
            is UpdateState.Failed -> u.version to u.url
            else -> return
        }
        update = UpdateState.Downloading(version, 0f)
        viewModelScope.launch {
            try {
                val f = Updater.download(app, version, url) { p -> update = UpdateState.Downloading(version, p) }
                update = UpdateState.Ready(version, f)
            } catch (e: Exception) {
                update = UpdateState.Failed(version, url, e.message ?: "دانلود ناموفق بود")
            }
        }
    }

    fun install() {
        val u = update as? UpdateState.Ready ?: return
        if (!Updater.install(app, u.file)) error = "اجازه‌ی نصب برنامه‌ها رو فعال کن و دوباره بزن"
    }

    val configured get() = Api.configured
}
